package advRocketry.Oxygen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

// a block like oxygen vent is an oxygen supplier or creates an instance of it and registers it to the exygen system
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
    // overwrite this
    public boolean isActive() {
        return true;
    }
    // overwrite this
    public int getBlockLimit(){
        return 1000;
    }

    public void reset() {
        scannedBlocksCounter = 0;
        isValidArea = false;
        isComplete = false;
        connectedSuppliers.clear();
        connectedSuppliers.add(this);
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
            if (scannedBlocks.containsKey(otherPos)) {
                // early stopping saves iteration steps, but don't forget to add the other supplier!
                connectedSuppliers.add(scannedBlocks.get(otherPos));
            } else{
                BlockState otherState = level.getBlockState(otherPos);

                if(otherState.isAir()){
                    // fast check for air
                    queue.add(otherPos);
                    continue;
                }

                if(otherState.isCollisionShapeFullBlock(level, otherPos))
                    // full block can not let air through
                    continue;

                // not air and not solid block, it needs some more analysis....
                // i will now test if the face that is toward the current position has a full shape
                // this is not perfect but it allows to work with open/closed doors
                List<Direction.Axis> requiresFullShape = null;
                if(facing.getAxis() == Direction.Axis.X)
                    requiresFullShape = List.of(Direction.Axis.Y, Direction.Axis.Z);
                else if(facing.getAxis() == Direction.Axis.Y)
                    requiresFullShape = List.of(Direction.Axis.X, Direction.Axis.Z);
                else
                    requiresFullShape = List.of(Direction.Axis.X, Direction.Axis.Y);
                boolean bothAxisAreFullShapee = true;
                VoxelShape shape = otherState.getShape(level, otherPos);
                for(Direction.Axis axis : requiresFullShape) {
                    if(shape.max(axis) < 1 || shape.min(axis) > 0) {
                        bothAxisAreFullShapee = false;
                    }
                }
                if(bothAxisAreFullShapee)
                    continue;

                queue.add(otherPos);
            }
        }
    }

    // gather all connected suppliers
    // a supplier might be indirectly connected through multiple other connectors
    // we need to find and add indirect connections so everything works correctly
    public void syncConnections(){
        // for every connected supplier i will add to my list all the other suppliers connections
        // over multiple ticks this should accumulate all connections
        // warning, this is slow! so don't call it too often!
        for (OxygenSupplier i : new HashSet<>(connectedSuppliers)){
           connectedSuppliers.addAll(i.connectedSuppliers);
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
        int myScanLimit = 0;
        for (OxygenSupplier i : connectedSuppliers) {
            myScanLimit += i.getRemainingScanLimit();
        }
        return myScanLimit;
    }

    // get my current remaining scan limit
    // TODO: this algorithm should support individual max scan size, so allow each block to have its own block scan limit
    private int getRemainingScanLimit() {
        return getBlockLimit() - scannedBlocksCounter;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public boolean hasValidArea() {
        return isValidArea;
    }
}
