package advRocketry.Blocks;

import advRocketry.Rocket.ICustomWeightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;

public class FuelTank extends Block implements ICustomWeightBlock {
    public FuelTank() {
        super(Properties.of().noOcclusion());
        registerDefaultState(getStateDefinition().any().setValue(BlockStateProperties.DOWN, false).setValue(BlockStateProperties.UP, false));
    }

    public int getFuelCapacity(){
        return 6000;
    }

    public float getWeightMultiplier() {
        return 0.1f;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.DOWN);
        builder.add(BlockStateProperties.UP);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateFromNeighbourShapes(stateDefinition.any(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState otherBlock = level.getBlockState(neighborPos);
        boolean isConnection = false;
        if (otherBlock.getBlock() instanceof FuelTank)
            isConnection = true;
        if (otherBlock.getBlock() instanceof RocketMotor)
            isConnection = true;
        if (direction.equals(Direction.UP))
            state = state.setValue(BlockStateProperties.UP, isConnection);
        if (direction.equals(Direction.DOWN))
            state = state.setValue(BlockStateProperties.DOWN, isConnection);
        return state;
    }
}
