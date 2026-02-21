package AOSWorkshopExpansion.Piston;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.Nullable;

import static AOSWorkshopExpansion.Piston.Piston.SPECIALFACING;
import static AOSWorkshopExpansion.Piston.PistonExtension.AXIS;

public class PistonHead extends Block implements SimpleWaterloggedBlock {
    public static EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public PistonHead() {
        super(Properties.of().noOcclusion());
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player) {
            state = state.setValue(FACING, player.getNearestViewDirection().getOpposite());
            state = updateFromNeighbourShapes(state,level,pos);
            level.setBlock(pos, state, 3);
        }
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

    VoxelShape notFullBlock = Shapes.create(new AABB(0.01,0.01,0.01,0.99,0.99,0.99));
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return notFullBlock;
    }
}
