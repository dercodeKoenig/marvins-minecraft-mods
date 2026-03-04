package AOSWorkshopExpansion.Piston;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static AOSWorkshopExpansion.Piston.Piston.SPECIALFACING;
import static AOSWorkshopExpansion.Piston.PistonExtension.AXIS;

public class PistonHead extends Block implements SimpleWaterloggedBlock {
    public static EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public PistonHead() {
        super(Properties.of().noOcclusion().strength(1));
        BlockState state = this.stateDefinition.any();
        state = state.setValue(FACING, Direction.NORTH);
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = stateDefinition.any();
        state = state.setValue(FACING, context.getNearestLookingDirection().getOpposite());
        state = updateFromNeighbourShapes(state, context.getLevel(), context.getClickedPos());
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        BlockState behind = level.getBlockState(pos.relative(facing.getOpposite()));
        if(behind.getBlock() instanceof Piston && behind.getValue(SPECIALFACING).direction == facing)
            return state;
        if(behind.getBlock() instanceof PistonExtension && behind.getValue(AXIS) == facing.getAxis())
            return state;

        if(neighborState.getBlock() instanceof Piston && neighborState.getValue(SPECIALFACING).direction == direction.getOpposite())
            state = state.setValue(FACING, direction.getOpposite());
        if(neighborState.getBlock() instanceof PistonExtension && neighborState.getValue(AXIS) == direction.getAxis())
            state = state.setValue(FACING, direction.getOpposite());
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // The piston platform (16x16x4) and the central pole (4x4x12) combined for each direction
    protected static final VoxelShape UP_SHAPE = Shapes.or(
            Block.box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 12.0D, 10.0D)
    );
    protected static final VoxelShape DOWN_SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.box(6.0D, 4.0D, 6.0D, 10.0D, 16.0D, 10.0D)
    );
    protected static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 4.0D),
            Block.box(6.0D, 6.0D, 4.0D, 10.0D, 10.0D, 16.0D)
    );
    protected static final VoxelShape SOUTH_SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 12.0D, 16.0D, 16.0D, 16.0D),
            Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 12.0D)
    );
    protected static final VoxelShape WEST_SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 4.0D, 16.0D, 16.0D),
            Block.box(4.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D)
    );
    protected static final VoxelShape EAST_SHAPE = Shapes.or(
            Block.box(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 6.0D, 6.0D, 12.0D, 10.0D, 10.0D)
    );

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }
}
