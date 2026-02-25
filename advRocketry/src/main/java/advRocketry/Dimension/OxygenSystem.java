package advRocketry.Dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;

// finds oxygen supplied blocks, distributes scanning over many ticks to improve performance
public class OxygenSystem {

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
    ///
    /// step 3: gather results (syncAreaState)
    /// Every OxygenSupplier will ask its connected parts if they are invalid and if so, mark itself as invalid.
    /// Meaning if any connected OxygenSupplier exceeded its limit the space is considered "open" and oxygen can not be contained.
    ///
    /// step 4: store results
    /// oxygenSuppliedBlocks is cleared
    /// For all entries in scannedBlocks, if the OxygenSupplier matched to it is in state "true", this block will be added to oxygenSuppliedBlocks.

    public static HashMap<ResourceLocation, OxygenSystem> oxygenSystems = new HashMap<>();
    // holds all oxygen suppliers on this level
    HashSet<OxygenSupplier> allRegisteredOxygenSuppliers = new HashSet<>();
    // populated during reset
    HashSet<OxygenSupplier> activeOxygenSuppliers = new HashSet<>();
    // holds all blocks that are currently supplied with oxygen by oxygen vent or possible other blocks
    HashSet<BlockPos> oxygenSuppliedBlocks = new HashSet<>();
    // used during flood scan
    HashMap<BlockPos, OxygenSupplier> scannedBlocks = new HashMap<>();

    public static boolean hasOxygenAt(Level level, BlockPos pos) {
        if (DimensionManager.getDimensionManager(level.isClientSide).get(level.dimension().location()).hasEnoughOxygen())
            return true;

        OxygenSystem instance = oxygenSystems.get(level.dimension().location());
        if (instance != null) {
            if (instance.oxygenSuppliedBlocks.contains(pos))
                return true;
        }

        return false;
    }

    public static int SCAN_LIMIT() {
        return 10000; // how much blocks a single oxygen supplier can scan, should be configu
    }

    public static int SCAN_LIMIT_PER_TICK() {
        return 500; // can only scan this many blocks in total per tick
    }

    // onload the blockentity should register itself here
    public static void registerOxygenSupplier(ResourceLocation levelId, OxygenSupplier supplier) {
        oxygenSystems.putIfAbsent(levelId, new OxygenSystem());
        oxygenSystems.get(levelId).allRegisteredOxygenSuppliers.add(supplier);
    }

    // on setremoved the blockentity should unregister itself
    public static void removeOxygenSupplier(ResourceLocation levelId, OxygenSupplier supplier) {
        oxygenSystems.putIfAbsent(levelId, new OxygenSystem());
        oxygenSystems.get(levelId).allRegisteredOxygenSuppliers.remove(supplier);
    }

    public static void tickAll(){
        for (OxygenSystem system : oxygenSystems.values()){
            system.tick();
        }
    }

    void tick() {

        if(allRegisteredOxygenSuppliers.isEmpty()) return;

        boolean allCompleted = true;
        for (int i = 0; i < SCAN_LIMIT_PER_TICK(); i++) {
            allCompleted = true;
            for (OxygenSupplier supplier : activeOxygenSuppliers) {
                if (!supplier.isComplete()) {
                    supplier.tickFloodScan(scannedBlocks);
                    allCompleted = false;
                }
            }
            if (allCompleted)
                break;
        }

        if (allCompleted && GlobalTime.getGlobalTime() % 1 == 0) {

            // gather results
            for (OxygenSupplier i : activeOxygenSuppliers) {
                i.syncAreaState();
            }

            // clear existing area
            oxygenSuppliedBlocks.clear();

            // add all blocks that are currently valid to the main set
            for (BlockPos p : scannedBlocks.keySet()) {
                OxygenSupplier supplier = scannedBlocks.get(p);
                if (supplier.hasValidArea()) {
                    oxygenSuppliedBlocks.add(p);
                }
            }

            // reset
            scannedBlocks.clear();
            activeOxygenSuppliers.clear();
            for (OxygenSupplier i : allRegisteredOxygenSuppliers) {
                if (i.isActive()) {
                    i.reset();
                    activeOxygenSuppliers.add(i);
                }
            }

            System.out.println("there are "+oxygenSuppliedBlocks.size()+" blocks supplied with oxygen");
        }
    }


}
