package advRocketry.Dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;

// finds oxygen supplied blocks, distributes scanning over many ticks to improve performance
public class OxygenSystem {

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

    public static HashMap<ResourceLocation, OxygenSystem> oxygenSystems = new HashMap<>();
    // holds all oxygen suppliers on this level
    HashSet<OxygenSupplier> allRegisteredOxygenSuppliers = new HashSet<>();
    // populated during reset
    HashSet<OxygenSupplier> activeOxygenSuppliers = new HashSet<>();
    // holds all blocks that are currently supplied with oxygen by oxygen vent or possible other blocks
    HashSet<BlockPos> oxygenSuppliedBlocks = new HashSet<>();
    // used during flood scan
    HashMap<BlockPos, OxygenSupplier> scannedBlocks = new HashMap<>();

    long timeLastReset = 0; // so that we do not scan too often

    public static boolean hasOxygenAt(Level level, BlockPos pos) {
        if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()).hasEnoughOxygen())
            return true;

        OxygenSystem instance = oxygenSystems.get(level.dimension().location());
        if (instance != null) {
            if (instance.oxygenSuppliedBlocks.contains(pos))
                return true;
        }

        return false;
    }

    public static int SCAN_LIMIT() {
        return 1000; // how much blocks a single oxygen supplier can scan
    }

    public static int SCAN_LIMIT_PER_TICK() {
        return 500; // can only scan this many blocks in total per tick
    }

    public static int SECONDS_BETWEEN_FULL_SCAN() {
        return 2;
    }

    // onload the blockentity should register itself here
    public static void registerOxygenSupplier(Level level, OxygenSupplier supplier) {
        if (level.isClientSide) return;
        oxygenSystems.putIfAbsent(level.dimension().location(), new OxygenSystem());
        oxygenSystems.get(level.dimension().location()).allRegisteredOxygenSuppliers.add(supplier);
    }

    // on setremoved the blockentity should unregister itself
    public static void removeOxygenSupplier(Level level, OxygenSupplier supplier) {
        if (level.isClientSide) return;
        oxygenSystems.putIfAbsent(level.dimension().location(), new OxygenSystem());
        oxygenSystems.get(level.dimension().location()).allRegisteredOxygenSuppliers.remove(supplier);
    }

    public static void tickAll() {
        for (OxygenSystem system : oxygenSystems.values()) {
            system.tick();
        }
    }

    void tick() {

        if (allRegisteredOxygenSuppliers.isEmpty()) return;
        //long t0 = System.nanoTime();
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
        // make sure we connect also to indirectly connected oxygen suppliers
        // we could be 3 or 7 areas separated from a connected supplier over multiple iterations we should catch it
        // this is important to find the correct max block limit to scan and to sync the final isValidArea state
        // I choose to sync it only once per tick and not after each block scanned because it is slow.
        // it only takes 5 ticks to connect to an area 5 sections away and this is rare anyway
        // having the correct connections is important for:
        //      - the final isAreaValid calculation (so only at end after all is complete, doesnt need to sync fast)
        //      - to get the correct remaining scan limit per block, important if any block has a significantly lower scan limit than some other connected block
        //
        for (OxygenSupplier supplier : activeOxygenSuppliers) {
            supplier.syncConnections();
        }
        //System.out.println((double) (System.nanoTime() - t0) / 1000000);

        if (allCompleted && timeLastReset + SECONDS_BETWEEN_FULL_SCAN() * 20L < GlobalTime.getGlobalTime()) {

            // gather results
            for (OxygenSupplier i : activeOxygenSuppliers) {
                i.syncAreaState();
            }
            //System.out.println("active:" + activeOxygenSuppliers.size());

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

            //System.out.println("Scan complete: there are " + oxygenSuppliedBlocks.size() + " blocks supplied with oxygen");
            timeLastReset = GlobalTime.getGlobalTime();
        }
    }


}
