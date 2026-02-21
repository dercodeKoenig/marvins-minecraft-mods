package AgeOfSteam.Blocks.Mechanics.FlyWheel;

import AgeOfSteam.Core.IMechanicalBlockProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class BlockFlyWheelBase extends Block implements EntityBlock, SimpleWaterloggedBlock {

    public static EnumProperty<Direction.Axis> ROTATION_AXIS = BlockStateProperties.AXIS;

    public BlockFlyWheelBase(Properties p) {
        super(p);
        BlockState state = this.stateDefinition.any();
        state = state.setValue(ROTATION_AXIS, Direction.Axis.Y);
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

            if (Math.abs(lookVec.y)< 0.8) {
                newAxis = placer.getDirection().getClockWise().getAxis();
            } else {
                newAxis = Direction.Axis.Y; // Dominant Y-axis
            }
            // Set the block state with the correct axis
            level.setBlock(pos, state.setValue(ROTATION_AXIS, newAxis), 3);
        }

        state = level.getBlockState(pos);
        level.setBlock(pos, updateFromNeighbourShapes(state, level, pos),3) ;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof IMechanicalBlockProvider provider) {
            if(provider.getConnectedParts(provider, null).isEmpty()){
                BlockEntity neighbor = tile.getLevel().getBlockEntity(tile.getBlockPos().relative(direction));
                if (neighbor instanceof IMechanicalBlockProvider otherProvider) {
                    if(otherProvider.getMechanicalBlock(direction.getOpposite()) != null){
                        state = state.setValue(ROTATION_AXIS, direction.getAxis());
                    }
                }
            }
        }
        return state;
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(ROTATION_AXIS) == Direction.Axis.Y)
            return Shapes.create(0.25, 0, 0.25, 0.75, 1, 0.75);
        else if (state.getValue(ROTATION_AXIS) == Direction.Axis.X)
            return Shapes.create(0, 0.25, 0.25, 1, 0.75, 0.75);
        else if (state.getValue(ROTATION_AXIS) == Direction.Axis.Z)
            return Shapes.create(0.25, 0.25, 0, 0.75, 0.75, 1);

        else return Shapes.create(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
    }
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
    
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityFlyWheelBase::tick;
    }

}