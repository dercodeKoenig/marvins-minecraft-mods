package advRocketry.LifeSupport;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.GlobalTime;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

///  note:  this system was initially designed to work for oxygen only,
///         so you will find comments / variable names that might be confusing
///         it will use the same system but copied multiple times for different types of life support

// finds oxygen supplied blocks, distributes scanning over many ticks to improve performance
public class LifeSupportSystem {

    // one system for every level
    private static final HashMap<ResourceLocation, LifeSupportSystem> LifeSupportSystems = new HashMap<>();

    // every type gets its own data to work with
    private final HashMap<LifeSupportType, LifeSupportData> lifeSupportData = new HashMap<>();

    public LifeSupportSystem() {
        lifeSupportData.put(LifeSupportType.PRESSURE_SUPPLIER, new LifeSupportData());
        lifeSupportData.put(LifeSupportType.OXYGEN_SUPPLIER, new LifeSupportData());
        lifeSupportData.put(LifeSupportType.TEMPERATURE_REGULATOR, new LifeSupportData());
    }

    public static boolean isTemperatureRegulated(Level level, BlockPos pos) {
        LifeSupportSystem instance = LifeSupportSystems.get(level.dimension().location());
        if (instance != null) {
            return instance.lifeSupportData.get(LifeSupportType.TEMPERATURE_REGULATOR).suppliedBlocks.contains(pos);
        }
        return false;
    }

    public static boolean isOxygenSupplied(Level level, BlockPos pos) {
        LifeSupportSystem instance = LifeSupportSystems.get(level.dimension().location());
        if (instance != null) {
            return instance.lifeSupportData.get(LifeSupportType.OXYGEN_SUPPLIER).suppliedBlocks.contains(pos);
        }
        return false;
    }

    public static boolean isPressurized(Level level, BlockPos pos) {
        LifeSupportSystem instance = LifeSupportSystems.get(level.dimension().location());
        if (instance != null) {
            return instance.lifeSupportData.get(LifeSupportType.PRESSURE_SUPPLIER).suppliedBlocks.contains(pos);
        }
        return false;
    }

    public static int SCAN_LIMIT_PER_TICK() {
        return 100; // can only scan this many blocks in total per tick
    }

    public static int SECONDS_BETWEEN_FULL_SCAN() {
        return 5;
    }

    // onload the blockentity should register itself here
    public static void registerLifeSupportSupplier(Level level, LifeSupportSupplier supplier) {
        if (level.isClientSide) return;
        LifeSupportSystems.putIfAbsent(level.dimension().location(), new LifeSupportSystem());
        LifeSupportSystems.get(level.dimension().location())
                .lifeSupportData.get(supplier.getType())
                .allRegisteredSuppliers.add(supplier);
    }

    // on setremoved the blockentity should unregister itself
    public static void removeLifeSupportSupplier(Level level, LifeSupportSupplier supplier) {
        if (level.isClientSide) return;
        if (!LifeSupportSystems.containsKey(level.dimension().location())) return;
        LifeSupportSystems.get(level.dimension().location())
                .lifeSupportData.get(supplier.getType())
                .allRegisteredSuppliers.remove(supplier);
    }


    // return the remaining problems
    // perform survival tick, should be called in server tick every second
    // maybe consume oxygen from equipment here
    public static void trySurvive(LivingEntity e, Level level, BlockPos pos, Set<Dimension.SurvivalProblem> survivalProblems) {
        SurvivalSystem.getSurvivalRule(e.getType()).trySurvive(e, level, pos, survivalProblems);
    }

    public static void mitigateProblems(Level level, BlockPos pos, Set<Dimension.SurvivalProblem> survivalProblems) {
        if (isPressurized(level, pos)) {
            survivalProblems.remove(Dimension.SurvivalProblem.TOO_LOW_PRESSURE);
        }
        if (isOxygenSupplied(level, pos)) {
            survivalProblems.remove(Dimension.SurvivalProblem.TOO_LITTLE_O2);
            survivalProblems.remove(Dimension.SurvivalProblem.TOO_MUCH_CO2);
        }
        if (isTemperatureRegulated(level, pos)) {
            survivalProblems.remove(Dimension.SurvivalProblem.TOO_COLD);
            survivalProblems.remove(Dimension.SurvivalProblem.TOO_HOT);
        }
    }

    public static void serverTick() {

        // check entities and apply damage if required
        if (GlobalTime.getGlobalTime() % 20 == 0) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            for (ResourceLocation levelId : DimensionManager.INSTANCE_SERVER.dimensions.keySet()) {
                Dimension dim = DimensionManager.INSTANCE_SERVER.get(levelId);
                if (dim == null)
                    continue;
                ServerLevel level = DimensionManager.getServerLevel(levelId);
                if (level == null)
                    continue;
                Set<Dimension.SurvivalProblem> problems = dim.getSurvivalProblems();
                if (problems.isEmpty())
                    // this dimension is habitable, no problems
                    continue;

                for (Entity e : level.getEntities().getAll()) {
                    if (e instanceof LivingEntity livingEntity) {
                        Set<Dimension.SurvivalProblem> remainingProblems = new HashSet<>(problems);

                        // try mitigate problems with lifesupport systems
                        mitigateProblems(level, livingEntity.blockPosition(), remainingProblems);
                        if (remainingProblems.isEmpty())
                            continue;

                        // the entity has to survive the remaining problems
                        trySurvive(livingEntity, level, livingEntity.blockPosition(), remainingProblems);

                        if (!remainingProblems.isEmpty()) {
                            livingEntity.hurt(new DamageSource(server.registryAccess().holderOrThrow(DamageTypes.GENERIC)), 1);
                            if (livingEntity instanceof Player player) {
                                String msg = "Life Support Warning: \n";
                                for (Dimension.SurvivalProblem p : remainingProblems) {
                                    msg += p.reason + "\n";
                                }
                                player.sendSystemMessage(Component.literal(msg));
                            }
                        }
                    }
                }
            }
        }


        // in case a dimension is deleted by a death star laser maybe?
        LifeSupportSystems.entrySet().removeIf(entry ->
                DimensionManager.getServerLevel(entry.getKey()) == null
        );
        // tick for every level
        for (ResourceLocation levelId : LifeSupportSystems.keySet()) {
            LifeSupportSystems.get(levelId).tick();
        }
    }

    public static void onServerStop() {
        LifeSupportSystems.clear();
    }

    void tick() {
        for (LifeSupportType type : lifeSupportData.keySet()) {
            LifeSupportData data = lifeSupportData.get(type);

            if (data.allRegisteredSuppliers.isEmpty()) continue;

            if (data.shouldScanNextTick) {
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

                if (allCompleted) {
                    data.shouldScanNextTick = false;

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
        PRESSURE_SUPPLIER,
        OXYGEN_SUPPLIER,
        TEMPERATURE_REGULATOR,
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
