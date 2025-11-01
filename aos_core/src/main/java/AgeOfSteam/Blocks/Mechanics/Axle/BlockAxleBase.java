package AgeOfSteam.Blocks.Mechanics.Axle;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.BlockMultiblockPart;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class BlockAxleBase extends BlockMultiblockPart implements EntityBlock {

    public static EnumProperty<Direction.Axis> ROTATION_AXIS = EnumProperty.create("axis", Direction.Axis.class);

    public BlockAxleBase(Properties p) {
        super(p);
        BlockState state = this.stateDefinition.any();
        state = state.setValue(ROTATION_AXIS, Direction.Axis.Y).setValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED, false);
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION_AXIS);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer != null) {
            Vec3 lookVec = placer.getLookAngle();
            Direction.Axis newAxis;

            if (Math.abs(lookVec.y) < 0.8) {
                newAxis = placer.getDirection().getClockWise().getAxis();
            } else {
                newAxis = Direction.Axis.Y; // Dominant Y-axis
            }
            // Set the block state with the correct axis
            level.setBlock(pos, state.setValue(ROTATION_AXIS, newAxis), 3);
        }

        state = level.getBlockState(pos);
        level.setBlock(pos, updateFromNeighbourShapes(state, level, pos), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof IMechanicalBlockProvider provider) {
            // before checking if we should auto rotate, make sure we are not already connected
            for (Direction i : Direction.values()) {
                if (i.getAxis() == state.getValue(ROTATION_AXIS)) {
                    BlockEntity b1 = level.getBlockEntity(pos.relative(i));
                    if (b1 instanceof IMechanicalBlockProvider mb1) {
                        if (mb1.getMechanicalBlock(i.getOpposite()) != null)
                            return state; // already connected
                    }
                }
            }

            BlockEntity neighbor = tile.getLevel().getBlockEntity(tile.getBlockPos().relative(direction));
            if (neighbor instanceof IMechanicalBlockProvider otherProvider) {
                if (otherProvider.getMechanicalBlock(direction.getOpposite()) != null) {
                    state = state.setValue(ROTATION_AXIS, direction.getAxis());
                }
            }

        }
        return state;
    }


    VoxelShape shapeX = Shapes.create(0.25, 0, 0.25, 0.75, 1, 0.75);
    VoxelShape shapeY = Shapes.create(0, 0.25, 0.25, 1, 0.75, 0.75);
    VoxelShape shapeZ = Shapes.create(0.25, 0.25, 0, 0.75, 0.75, 1);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(ROTATION_AXIS) == Direction.Axis.Y)
            return shapeX;
        else if (state.getValue(ROTATION_AXIS) == Direction.Axis.X)
            return shapeY;
        else if (state.getValue(ROTATION_AXIS) == Direction.Axis.Z)
            return shapeZ;

        else return Shapes.empty();
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityAxleBase::tick;
    }

    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
}