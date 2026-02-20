package AOSWorkshopExpansion.Piston;

import ARLib.network.INetworkTagReceiver;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static AOSWorkshopExpansion.Piston.Piston.SPECIALFACING;
import static AOSWorkshopExpansion.Registry.ENTITY_PISTON;

public class EntityPiston extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver {

    double lastRotation;
    double extraResistance = 0;
    double perBlockResistance = 10;
    double baseResistance = 10;
    int moveTicksMax = 10;


    HashMap<BlockPos, BlockState> movingBlocks = new HashMap<>();
    int movingTicks = 0;
    int currentAction =0; // 1 = extend, -1 = unextend for render

    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return 600;
        }

        @Override
        public double getInertia(Direction face) {
            return 1;
        }

        @Override
        public double getTorqueResistance(Direction face) {
            return baseResistance + extraResistance;
        }

        @Override
        public double getTorqueProduced(Direction face) {
            return 0;
        }

        @Override
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            Piston.SpecialFacing facing = getBlockState().getValue(SPECIALFACING);
            if (facing == Piston.SpecialFacing.NORTH)
                return -1;
            return 1;
        }
    };

    public EntityPiston(BlockPos pos, BlockState blockState) {
        super(ENTITY_PISTON.get(), pos, blockState);
    }

    public Pair<List<BlockPos>, List<BlockPos>> getToDestroyToMove(boolean extend) {
        BlockState state = getBlockState();
        BlockPos pos = getBlockPos();
        Direction facing = state.getValue(SPECIALFACING).direction;
        BlockPos headPos = null;

        List<BlockPos> structureBlocks = new ArrayList<>();

        // search for head pos
        int headOffset = 1;
        while (headOffset <= 12) {
            BlockPos currentPos = pos.relative(facing, headOffset);
            BlockState infront = level.getBlockState(currentPos);
            boolean validHead = (infront.getBlock() instanceof PistonHead && infront.getValue(PistonHead.FACING) == facing);
            if (validHead) {
                headPos = currentPos;
                break;
            }
            boolean validFrontExtension = (infront.getBlock() instanceof PistonExtension && infront.getValue(PistonExtension.AXIS) == facing.getAxis());
            if (!validFrontExtension)
                return null;
            structureBlocks.add(currentPos);
            headOffset++;
        }
        if (headPos == null)
            return null;
        if (!extend && headOffset == 1)
            // minimum reached
            return null;

        structureBlocks.add(headPos);

        // search how many back extensions there are
        int backExtensions = 0;
        BlockState lastBlockState = Blocks.AIR.defaultBlockState();
        while (backExtensions < 12) {
            BlockPos currentPos = pos.relative(facing.getOpposite(), backExtensions + 1);
            lastBlockState = level.getBlockState(currentPos);
            boolean validBackExtension = (lastBlockState.getBlock() instanceof PistonExtension && lastBlockState.getValue(PistonExtension.AXIS) == facing.getAxis());
            if (!validBackExtension)
                break;
            backExtensions++;
            structureBlocks.add(currentPos);
        }
        if (backExtensions == 0 && extend)
            // can not extend if there is no extension to back
            return null;

        if (!extend && !lastBlockState.canBeReplaced()) {
            // can not go back if the way is blocked
            return null;
        }
        List<BlockPos> toDestroy = new ArrayList<>();
        List<BlockPos> toMove = new ArrayList<>();
        if (extend) {
            PistonStructureResolver resolver = new PistonStructureResolver(level, headPos, facing, true);
            if (!resolver.resolve())
                return null;
            toDestroy = resolver.getToDestroy();
            toMove = resolver.getToPush();
        }
        // add our structure blocks
        toMove.addAll(structureBlocks);

        return Pair.of(toDestroy, toMove);
    }

    public void destroyBlocks(List<BlockPos> blocks) {
        for (BlockPos p : blocks) {
            level.destroyBlock(p, true);
        }
    }

    public void placeBlocks(HashMap<BlockPos, BlockState> blocks) {
        for (BlockPos p : blocks.keySet()) {
            level.setBlock(p, blocks.get(p), 3);
        }
    }

    public void startPushing(List<BlockPos> toMove, boolean extend) {
        for (BlockPos pos : toMove) {
            Direction toPush = getBlockState().getValue(SPECIALFACING).direction;
            if (!extend)
                toPush = toPush.getOpposite();
            BlockPos targetPos = pos.relative(toPush);
            if (targetPos.equals(getBlockPos())) // skip over piston block
                targetPos = targetPos.relative(toPush);
            movingBlocks.put(targetPos, level.getBlockState(pos));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        movingTicks = 0;
        if(extend)currentAction = 1;
        else currentAction = -1;
        level.setBlock(getBlockPos(), getBlockState().setValue(Piston.STATE1, true), 3);
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        // The following logic runs client & server so that the client already knows when to start
        // moving and placing and does not need to wait for server network packets.

        if (movingBlocks.isEmpty()) { // do never overwrite existing moving blocks, only update once move is complete
            int action = 0;
            if (myMechanicalBlock.currentRotation < 180 && lastRotation >= 180) {
                // extending...
                action = 1;
            }
            if (myMechanicalBlock.currentRotation < -180 && lastRotation >= -180) {
                // unextend
                action = -1;
            }
            if (action != 0) {
                boolean extend = action == 1;
                Pair<List<BlockPos>, List<BlockPos>> toDestroyToMove = getToDestroyToMove(extend);
                if (toDestroyToMove == null) {
                    extraResistance = 0; // <- these values are only processed server side in the mechanical block
                } else {
                    List<BlockPos> toDestroy = toDestroyToMove.getFirst();
                    List<BlockPos> toMove = toDestroyToMove.getSecond();
                    double newResistance = (toDestroy.size() + toMove.size()) * perBlockResistance;
                    if (extraResistance != 0) {
                        // do not initiate on first rotation, I want it to consume some power first
                        // it will require 1 startup rotation to consume some power if it was idle

                        // initiate movement
                        // this is what i want client to register so it doesn't need to wait for network packet
                        destroyBlocks(toDestroy);
                        startPushing(toMove, extend);
                    }
                    extraResistance = newResistance;
                }
            }

        } else {
            movingTicks++;
            if (movingTicks > moveTicksMax) {
                placeBlocks(movingBlocks);
                movingBlocks.clear();
                movingTicks = 0;
                level.setBlock(getBlockPos(), getBlockState().setValue(Piston.STATE1, false), 3);
                currentAction = 0;
            }
        }
        lastRotation = myMechanicalBlock.currentRotation;
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityPiston) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        myMechanicalBlock.mechanicalReadServer(compoundTag, serverPlayer);

    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadClient(compoundTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
    }

    public Direction.Axis getAxleAxis() {
        Piston.SpecialFacing facing = getBlockState().getValue(SPECIALFACING);
        if (facing.direction != Direction.UP && facing.direction != Direction.DOWN) {
            // horizontal facing, axle is on orthogonal side
            return facing.direction.getClockWise().getAxis();
        } else {
            // vertical facing, we need to consider rotation
            if (facing == Piston.SpecialFacing.UP2)
                return Direction.Axis.Z;
            if (facing == Piston.SpecialFacing.UP)
                return Direction.Axis.X;
            if (facing == Piston.SpecialFacing.DOWN2)
                return Direction.Axis.Z;
            if (facing == Piston.SpecialFacing.DOWN)
                return Direction.Axis.X;
        }
        return null;
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction direction) {
        // allow access to the side from normal rotation
        if (direction.getAxis() == getAxleAxis()) {
            return myMechanicalBlock;
        }
        return null;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

}
