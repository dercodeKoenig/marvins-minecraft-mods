package advRocketry.LifeSupport;

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
public abstract class LifeSupportSupplier {

    private final BlockPos myPos;
    private final Level level;
    private final LinkedList<BlockPos> queue = new LinkedList<>();
    private int scannedBlocksCounter = 0;
    private Network network = new Network();
    private boolean isComplete;

    public LifeSupportSupplier(Level level, BlockPos myPos) {
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

    // we can not use the same system for different times, would mess up instance variables
    public abstract LifeSupportSystem.LifeSupportType getType();

    public void reset() {
        scannedBlocksCounter = 0;
        isComplete = false;
        // reset queue
        queue.clear();
        queue.add(myPos);
        // start with a fresh unique network
        network = new Network();
        network.members.add(this);
    }

    // scan all blocks that are connected until the entire area is scanned or we run out of scan limit
    public void tickFloodScan(HashMap<BlockPos, LifeSupportSupplier> scannedBlocks) {

        BlockPos current = queue.poll();
        if (current == null) {
            // ran out of room to scan, the area is probably valid
            isComplete = true;
            return;
        }
        if (scannedBlocks.containsKey(current)) {
            // this position is already worked
            LifeSupportSupplier otherSupplier = scannedBlocks.get(current);
            network.merge(otherSupplier.network);
            return;
        }

        if (getCombinedRemainingScanLimit() <= 0) {
            // combined scan limit exhausted
            // the area is too large or invalid
            isComplete = true;
            network.isValidArea = false;
            return;
        }
        scannedBlocksCounter++;


        // add to the set that this block was now processed so it will not be processed again
        scannedBlocks.put(current, this);

        // find reachable blocks to scan next
        for (Direction facing : Direction.values()) {
            BlockPos otherPos = current.relative(facing);
            if (scannedBlocks.containsKey(otherPos)) {
                // early stopping saves iteration steps, but don't forget to merge networks
                network.merge(scannedBlocks.get(otherPos).network);
            } else{
                BlockState otherState = level.getBlockState(otherPos);

                if(otherState.isAir()){
                    // fast check for air, has to be scanned
                    queue.add(otherPos);
                    continue;
                }

                if(otherState.isCollisionShapeFullBlock(level, otherPos))
                    // full block can not let air through, even if it is just a structure tower
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
                VoxelShape shape = otherState.getCollisionShape(level, otherPos);
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

    // find the combined remaining scan limit for the LifeSupport suppliers
    private int getCombinedRemainingScanLimit() {
        int myScanLimit = 0;
        for (LifeSupportSupplier i : network.members) {
            myScanLimit += i.getRemainingScanLimit();
        }
        return myScanLimit;
    }

    // get my current remaining scan limit
    private int getRemainingScanLimit() {
        return getBlockLimit() - scannedBlocksCounter;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public boolean hasValidArea() {
        return network.isValidArea;
    }

    public static class Network{
        public HashSet<LifeSupportSupplier> members = new HashSet<>();
        // start assuming a valid area until it is invalid
        // if only 1 connected member sets this to invalid, the entire network is invalid
        // since parts will share the same network reference, it will make sure they all go invalid
        boolean isValidArea = true;

        public void merge(Network other) {
            if (this == other) return; // Already the same network

            // Move all members from the other network into this one
            this.members.addAll(other.members);

            // If either network was already marked invalid,
            // the new combined network must be invalid.
            if (!other.isValidArea) {
                this.isValidArea = false;
            }

            // Point all members of the other network to THIS network, so they share it from now on
            for (LifeSupportSupplier supplier : other.members) {
                supplier.network = this;
            }
        }
    }
}
