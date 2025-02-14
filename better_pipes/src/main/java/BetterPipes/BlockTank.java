package BetterPipes;

import BetterPipes.BlockPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static BetterPipes.Registry.ENTITY_TANK;


public class BlockTank extends Block implements EntityBlock {

    public static BooleanProperty connectedBelow = BooleanProperty.create("connected_down");
    public static BooleanProperty connectedAbove = BooleanProperty.create("connected_up");

    public BlockTank() {
        super(Properties.of().noOcclusion());
        BlockState defaultState = this.stateDefinition.any();
        defaultState =        defaultState.setValue(connectedBelow, false);
        defaultState =        defaultState.setValue(connectedAbove, false);

        this.registerDefaultState(defaultState);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos, updateFromNeighbourShapes(state, level, pos),3) ;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {



        if (direction == Direction.DOWN) {
            if (neighborState.getBlock() instanceof BlockTank) {
                state = state.setValue(connectedBelow, true);
            }else{
                state = state.setValue(connectedBelow, false);
            }
        }

        if (direction == Direction.UP) {
            if (neighborState.getBlock() instanceof BlockTank) {
                state = state.setValue(connectedAbove, true);
            }else{
                state = state.setValue(connectedAbove, false);
            }
        }
        return state;
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(connectedBelow);
        builder.add(connectedAbove);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_TANK.get().create(blockPos,blockState);
    }

    VoxelShape notFullBlock = Shapes.create(0.125, 0, 0.125, 1-0.125, 1, 1-0.125);
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.notFullBlock;
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityTank::tick;
    }
}
