package advRocketry.LifeSupport;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.GlobalTime;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.HashSet;

///  note:  this system was initially designed to work for oxygen only,
///         so you will find comments / variable names that might be confusing
///         it will use the same system but copied multiple times for different types of life support
///

// finds oxygen supplied blocks, distributes scanning over many ticks to improve performance
public class LifeSupportSystem {

    // one system for every level
    static HashMap<ResourceLocation, LifeSupportSystem> LifeSupportSystems = new HashMap<>();

    HashMap<LifeSupportType, LifeSupportData> lifeSupportData = new HashMap<>();

    public LifeSupportSystem() {
        lifeSupportData.put(LifeSupportType.OXYGEN_SUPPLIER, new LifeSupportData());
        lifeSupportData.put(LifeSupportType.HEAT_SUPPLIER, new LifeSupportData());
        lifeSupportData.put(LifeSupportType.COOLING_SUPPLIER, new LifeSupportData());
    }

    public static boolean canSurviveAt(Level level, BlockPos pos) {
        Dimension dim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (dim == null)
            return true;
        Dimension.SurvivalInfo info = dim.canSurvive();
        if (info == Dimension.SurvivalInfo.OK)
            return true;

        LifeSupportSystem instance = LifeSupportSystems.get(level.dimension().location());
        if (instance != null) {

            if (info == Dimension.SurvivalInfo.TOO_LITTLE_O2 ||
                    info == Dimension.SurvivalInfo.TOO_MUCH_O2 ||
                    info == Dimension.SurvivalInfo.TOO_MUCH_CO2) {
                if (instance.lifeSupportData.get(LifeSupportType.OXYGEN_SUPPLIER).suppliedBlocks.contains(pos)) {
                    return true;
                }
            }

            if (info == Dimension.SurvivalInfo.TOO_COLD) {
                if (instance.lifeSupportData.get(LifeSupportType.HEAT_SUPPLIER).suppliedBlocks.contains(pos)) {
                    return true;
                }
            }

            if (info == Dimension.SurvivalInfo.TOO_HOT) {
                if (instance.lifeSupportData.get(LifeSupportType.HEAT_SUPPLIER).suppliedBlocks.contains(pos)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static int SCAN_LIMIT_PER_TICK() {
        return 200; // can only scan this many blocks in total per tick
    }

    public static int SECONDS_BETWEEN_FULL_SCAN() {
        return 2;
    }

    // onload the blockentity should register itself here
    public static void registerLifeSupportSupplier(Level level, LifeSupportSupplier supplier, LifeSupportType type) {
        if (level.isClientSide) return;
        LifeSupportSystems.putIfAbsent(level.dimension().location(), new LifeSupportSystem());
        LifeSupportSystems.get(level.dimension().location())
                .lifeSupportData.get(type)
                .allRegisteredSuppliers.add(supplier);
    }

    // on setremoved the blockentity should unregister itself
    public static void removeLifeSupportSupplier(Level level, LifeSupportSupplier supplier, LifeSupportType type) {
        if (level.isClientSide) return;
        LifeSupportSystems.putIfAbsent(level.dimension().location(), new LifeSupportSystem());
        LifeSupportSystems.get(level.dimension().location())
                .lifeSupportData.get(type)
                .allRegisteredSuppliers.remove(supplier);
    }

    public static void serverTick() {

        // check entities and apply damage if required
        if (GlobalTime.getGlobalTime() % 20 == 0) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            for (ResourceLocation levelId : DimensionManager.INSTANCE_SERVER.dimensions.keySet()) {
                Dimension dim = DimensionManager.INSTANCE_SERVER.get(levelId);
                if (dim == null)
                    continue;
                if (dim.canSurvive() == Dimension.SurvivalInfo.OK)
                    continue;
                ServerLevel level = DimensionManager.getServerLevel(levelId);
                if (level == null)
                    continue;
                for (Entity e : level.getEntities().getAll()) {
                    if (e instanceof LivingEntity livingEntity) {
                        if (!canSurviveAt(level, livingEntity.blockPosition())) {
                            livingEntity.hurt(new DamageSource(server.registryAccess().holderOrThrow(DamageTypes.GENERIC)), 1);
                        }
                    }
                }
            }
        }

        for (ResourceLocation levelId : LifeSupportSystems.keySet()) {
            // skip the scanning if there is life possible anyway
            Dimension dim = DimensionManager.INSTANCE_SERVER.get(levelId);
            if (dim == null || dim.canSurvive() == Dimension.SurvivalInfo.OK)
                continue;
            LifeSupportSystems.get(levelId).tick();
        }
    }

    void tick() {
        for (LifeSupportType type : lifeSupportData.keySet()) {
            LifeSupportData data = lifeSupportData.get(type);

            if (data.allRegisteredSuppliers.isEmpty()) return;

            if (data.shouldScanNextTick) {
                long t0 = System.nanoTime();
                boolean allCompleted = true;
                for (int i = 0; i < SCAN_LIMIT_PER_TICK(); i++) {
                    allCompleted = true;
                    for (LifeSupportSupplier supplier : data.activeSuppliers) {
                        if (!supplier.isComplete()) {
                            supplier.tickFloodScan(data.scannedBlocks);
                            allCompleted = false;
                        }
                    }
                    if (allCompleted) {
                        break;
                    }
                }
                // make sure we connect also to indirectly connected oxygen suppliers
                // we could be 3 or 7 areas separated from a connected supplier over multiple iterations we should catch it
                // this is important to find the correct max block limit to scan and to sync the final isValidArea state
                // I choose to sync it only once per tick and not after each block scanned because it is slow.
                // it only takes 5 ticks to connect to an area 5 sections away and this is rare anyway
                // having the correct connections is important for:
                //      - the final isAreaValid calculation (so only at end after all is complete, doesnt need to sync fast)
                //      - to get the correct remaining scan limit per block, important if any block has a significantly lower scan limit than some other connected block
                //
                for (LifeSupportSupplier supplier : data.activeSuppliers) {
                    supplier.syncConnections();
                }

                if (allCompleted) {
                    data.shouldScanNextTick = false;

                    // gather results
                    for (LifeSupportSupplier i : data.activeSuppliers) {
                        i.syncAreaState();
                    }
                    System.out.println("active:" + data.activeSuppliers.size());

                    HashSet<BlockPos> newOxygenSuppliedBlocks = new HashSet<>();
                    // add all blocks that are currently valid to the main set
                    for (BlockPos p : data.scannedBlocks.keySet()) {
                        LifeSupportSupplier supplier = data.scannedBlocks.get(p);
                        if (supplier.hasValidArea()) {
                            newOxygenSuppliedBlocks.add(p);
                        }
                    }
                    data.suppliedBlocks = newOxygenSuppliedBlocks;
                    System.out.println("Scan complete: there are " + data.suppliedBlocks.size() + " blocks supplied with " + type);

                }
                //System.out.println((double) (System.nanoTime() - t0) / 1000000);
            } else {
                // sleep until a new scan is required
                if (data.timeLastReset + SECONDS_BETWEEN_FULL_SCAN() * 20L < GlobalTime.getGlobalTime()) {
                    // reset
                    data.scannedBlocks.clear();
                    // re-read currently active oxygen suppliers
                    data.activeSuppliers.clear();
                    for (LifeSupportSupplier i : data.allRegisteredSuppliers) {
                        if (i.isActive()) {
                            i.reset();
                            data.activeSuppliers.add(i);
                        }
                    }

                    data.timeLastReset = GlobalTime.getGlobalTime();
                    data.shouldScanNextTick = true;
                    System.out.println("reset..." + GlobalTime.getGlobalTime());
                }
            }
        }
    }

    public enum LifeSupportType {
        OXYGEN_SUPPLIER, // this handles too much, too low, or too much co2 for simplicity
        HEAT_SUPPLIER, // this is required when too cold
        COOLING_SUPPLIER, // this is required when too hot
    }

    public static class LifeSupportData {

        // holds all suppliers on this level
        HashSet<LifeSupportSupplier> allRegisteredSuppliers = new HashSet<>();
        // populated during reset and used during scanning
        HashSet<LifeSupportSupplier> activeSuppliers = new HashSet<>();
        // holds all blocks that are currently supplied with whatever type this is by supplier blocks like an oxygen vent supplies oxygen
        HashSet<BlockPos> suppliedBlocks = new HashSet<>();
        // used during flood scan to hold the state for every blockPos
        HashMap<BlockPos, LifeSupportSupplier> scannedBlocks = new HashMap<>();

        // so that we do not scan too often
        long timeLastReset = 0;

        // set to false when all scanning is complete and to true after reset
        boolean shouldScanNextTick = false;
    }

}

///  this was written when it was only oxygen system but the same logic still applies

///  speed test results:
///  0.8ghz cpu clock
///  32 OxygenSuppliers, all connected
///  open area -> max scan time
///  1k blocks max per supplier, 500 per tick
///  took about 4 - 8 ms per tick
///  on normal cpu clock, 1ms per tick

/// how it works:
/// step 1: reset
/// Every OxygenSupplier will receive a reset signal.
///
/// step 2: floodScan
/// During a scan, every blockEntity that is registered as OxygenSupplier will iterate over its reachable blocks.
/// Every reachable block will be stored in the scannedBlocks map together with the OxygenSupplier that reached it.
/// When another OxygenSupplier reaches a blockPos that is already reached by another OxygenSupplier,
/// it will not process this node but remember that it is connected to another OxygenSupplier.
/// A OxygenSupplier will stop scanning once it reached its ScanLimit or it runs out of blocks to scan.
/// The scan limit is calculated as the suppliers own scanlimit + remaining scanlimit of connected suppliers when they ran out of blocks
/// If a Supplier exceeds its scanlimit, it will mark its area as invalid, meaning it can not supply oxygen there
/// It is important to make sure every oxygenSupplier also knows about indirectly connected areas to calculate the correct
/// final state and the remaining block scan limit!
///
/// step 3: gather results (syncAreaState)
/// Every OxygenSupplier will ask its connected parts if they are invalid and if so, mark itself as invalid.
/// Meaning if any connected OxygenSupplier exceeded its limit the space is considered "open" and oxygen can not be contained.
///
/// step 4: store results
/// oxygenSuppliedBlocks is cleared
/// For all entries in scannedBlocks, if the OxygenSupplier matched to it is in state "true", this block will be added to oxygenSuppliedBlocks.
