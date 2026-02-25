package advRocketry.Dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class OxygenSupplier {

    private int scannedBlocksCounter = 0;
    private BlockPos myPos;
    private Level level;
    private LinkedList<BlockPos> queue = new LinkedList<>();
    private HashSet<OxygenSupplier> connectedSuppliers = new HashSet<>();
    private boolean isValidArea;
    private boolean isComplete;

    public OxygenSupplier(Level level, BlockPos myPos) {
        this.myPos = myPos;
        this.level = level;
        reset();
    }

    public boolean isActive() {
        return true;
    }

    public void reset() {
        scannedBlocksCounter = 0;
        isValidArea = false;
        isComplete = false;
        connectedSuppliers.clear();
        queue.clear();
        queue.add(myPos);
    }

    // scan all blocks that are connected until the entire area is scanned or we run out of scan limit
    public void tickFloodScan(HashMap<BlockPos, OxygenSupplier> scannedBlocks) {

        BlockPos current = queue.poll();
        if (current == null) {
            // ran out of room to scan, the area is probably valid
            isValidArea = true;
            isComplete = true;
            return;
        }
        if (scannedBlocks.containsKey(current)) {
            // this block is already processed by another OxygenSupplier or by this one, skip!
            OxygenSupplier otherSupplier = scannedBlocks.get(current);
            if (otherSupplier != this)
                // if this block was scanned by another OxygenSupplier, the other one is connected to this one
                connectedSuppliers.add(otherSupplier);
            return;
        }

        if (getCombinedRemainingScanLimit() <= 0) {
            // combined scan limit exhausted
            // the area is too large or invalid
            isComplete = true;
            return;
        }
        scannedBlocksCounter++;


        // add to the list that this block was now processed so it will not be processed double
        scannedBlocks.put(current, this);

        // find reachable blocks to scan next
        for (Direction facing : Direction.values()) {
            BlockPos otherPos = current.relative(facing);
            BlockState otherState = level.getBlockState(otherPos);
            // if the other block is not a solid full block like slab or torch,
            // air can flow through so it has to be scanned until we find a solid wall to end the scan
            if (!otherState.isCollisionShapeFullBlock(level, otherPos)) {
                if(!scannedBlocks.containsKey(otherPos)) {
                    queue.add(otherPos);
                }
            }
        }
    }

    // if any connected block has an invalid area,
    // this blocks area is also invalid because it is connected to the invalid area
    public void syncAreaState() {
        for (OxygenSupplier i : connectedSuppliers) {
            if (!i.hasValidArea()) {
                isValidArea = false;
            }
        }
    }

    // find the combined remaining scan limit for the oxygen suppliers
    private int getCombinedRemainingScanLimit() {
        int myScanLimit = getRemainingScanLimit();
        for (OxygenSupplier i : connectedSuppliers) {
            myScanLimit += i.getRemainingScanLimit();
        }
        return myScanLimit;
    }

    // get my current remaining scan limit
    private int getRemainingScanLimit() {
        return OxygenSystem.SCAN_LIMIT() - scannedBlocksCounter;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public boolean hasValidArea() {
        return isValidArea;
    }
}
