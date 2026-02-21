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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static AOSWorkshopExpansion.Piston.Piston.SPECIALFACING;
import static AOSWorkshopExpansion.Piston.PistonHead.FACING;

public class PistonExtension extends Block {

    public static EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public PistonExtension() {
        super(Properties.of().noOcclusion().dynamicShape());
        BlockState state = this.stateDefinition.any();
        state = state.setValue(AXIS, Direction.Axis.X);
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player) {
            state = state.setValue(AXIS, player.getNearestViewDirection().getAxis());
            state = updateFromNeighbourShapes(state, level, pos);
            level.setBlock(pos, state, 3);
        }
    }

    public boolean canConnectTo(Direction.Axis axis, BlockState neighbor) {
        if (neighbor.getBlock() instanceof Piston && neighbor.getValue(SPECIALFACING).direction.getAxis() == axis)
            return true;
        if (neighbor.getBlock() instanceof PistonHead && neighbor.getValue(FACING).getAxis() == axis)
            return true;
        if (neighbor.getBlock() instanceof PistonExtension && neighbor.getValue(AXIS) == axis)
            return true;
        return false;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis axis = state.getValue(AXIS);
        for (Direction facing : Direction.values()) {
            if (facing.getAxis() == axis) {
                BlockState neighbor = level.getBlockState(pos.relative(facing));
                if (canConnectTo(axis, neighbor))
                    return state;
            }
        }
        if (canConnectTo(direction.getAxis(), neighborState))
            return state.setValue(AXIS, direction.getAxis());
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
    }

    static VoxelShape shapeX = Shapes.create(0, 0.25, 0.25, 1, 0.75, 0.75);
    static VoxelShape shapeZ = Shapes.create(0.25, 0.25, 0, 0.75, 0.75, 1);
    static VoxelShape shapeY = Shapes.create(0.25, 0, 0.25, 0.75, 1, 0.75);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis axis = state.getValue(AXIS);
        if (axis == Direction.Axis.X)
            return shapeX;
        if (axis == Direction.Axis.Z)
            return shapeZ;
        if (axis == Direction.Axis.Y)
            return shapeY;
        return Shapes.empty();
    }
}
