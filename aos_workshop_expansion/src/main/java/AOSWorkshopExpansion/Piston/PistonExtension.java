package AOSWorkshopExpansion.Piston;

import it.unimi.dsi.fastutil.booleans.BooleanBooleanPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static AOSWorkshopExpansion.Piston.Piston.SPECIALFACING;
import static AOSWorkshopExpansion.Piston.PistonHead.FACING;

public class PistonExtension extends Block {

    public static EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    //public static BooleanProperty PLACEHOLDER = BooleanProperty.create("placeholder");

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
            state = updateFromNeighbourShapes(state,level,pos);
            level.setBlock(pos, state, 3);
        }
    }

    public boolean canConnectTo(Direction.Axis axis, BlockState neighbor){
        if(neighbor.getBlock() instanceof Piston && neighbor.getValue(SPECIALFACING).direction.getAxis() == axis)
            return true;
        if(neighbor.getBlock() instanceof PistonHead && neighbor.getValue(FACING).getAxis() == axis)
            return true;
        if(neighbor.getBlock() instanceof PistonExtension && neighbor.getValue(AXIS) == axis)
            return true;
        return false;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis axis = state.getValue(AXIS);
        for (Direction facing : Direction.values()){
            if(facing.getAxis() == axis) {
                BlockState neighbor = level.getBlockState(pos.relative(facing));
                if(canConnectTo(axis, neighbor))
                    return state;
            }
        }
        if(canConnectTo(direction.getAxis(), neighborState))
            return state.setValue(AXIS, direction.getAxis());
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
