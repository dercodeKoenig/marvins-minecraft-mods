package advRocketry.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nullable;

public class LaunchPad extends Block {
    public static BooleanProperty east = BooleanProperty.create("east");
    public static BooleanProperty west = BooleanProperty.create("west");
    public static BooleanProperty north = BooleanProperty.create("north");
    public static BooleanProperty south = BooleanProperty.create("south");

    public LaunchPad() {
        super(Properties.of());
        BlockState state = getStateDefinition().any();
        state = state.setValue(east, false);
        state = state.setValue(north, false);
        state = state.setValue(west, false);
        state = state.setValue(south, false);
        registerDefaultState(state);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(east);
        builder.add(north);
        builder.add(west);
        builder.add(south);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        state = level.getBlockState(pos);
        level.setBlock(pos, updateFromNeighbourShapes(state, level, pos), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState otherBlock = level.getBlockState(neighborPos);
        boolean isConnection = false;
        if (otherBlock.getBlock() instanceof LaunchPad)
            isConnection = true;
        if (direction.equals(Direction.EAST))
            state = state.setValue(east, isConnection);
        if (direction.equals(Direction.SOUTH))
            state = state.setValue(south, isConnection);
        if (direction.equals(Direction.WEST))
            state = state.setValue(west, isConnection);
        if (direction.equals(Direction.NORTH))
            state = state.setValue(north, isConnection);
        return state;
    }

}
