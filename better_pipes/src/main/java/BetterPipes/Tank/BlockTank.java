package BetterPipes.Tank;

import BetterPipes.PipeBase.BlockPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static BetterPipes.Registry.ENTITY_TANK;


public class BlockTank extends Block implements EntityBlock {

    public static BooleanProperty connectedBelow = BooleanProperty.create("connected_down");
    public static BooleanProperty connectedAbove = BooleanProperty.create("connected_up");
    public static Map<Direction, IntegerProperty> otherConnections = new HashMap<>();

    static {

        otherConnections.put(Direction.EAST, IntegerProperty.create("c_east", 0, 1));
        otherConnections.put(Direction.WEST, IntegerProperty.create("c_west", 0, 1));
        otherConnections.put(Direction.NORTH, IntegerProperty.create("c_north", 0, 1));
        otherConnections.put(Direction.SOUTH, IntegerProperty.create("c_south", 0, 1));

    }

    VoxelShape notFullBlock = Shapes.create(0.125, 0, 0.125, 1 - 0.125, 1, 1 - 0.125);

    public BlockTank() {
        super(Properties.of().noOcclusion());
        BlockState defaultState = this.stateDefinition.any();
        defaultState = defaultState.setValue(connectedBelow, false);
        defaultState = defaultState.setValue(connectedAbove, false);

        for (Direction i : otherConnections.keySet()) {
            defaultState = defaultState.setValue(otherConnections.get(i), 0);
        }
        this.registerDefaultState(defaultState);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        if (otherConnections.containsKey(direction)) {
            if (neighborState.getBlock() instanceof BlockPipe) {
                if (neighborState.getValue(BlockPipe.connections.get(direction.getOpposite())) == BlockPipe.ConnectionState.CONNECTED || neighborState.getValue(BlockPipe.connections.get(direction.getOpposite())) == BlockPipe.ConnectionState.EXTRACTION)
                    state = state.setValue(otherConnections.get(direction), 1);
            } else {
                state = state.setValue(otherConnections.get(direction), 0);
            }
        }

        if (direction == Direction.DOWN) {
            if (neighborState.getBlock() instanceof BlockTank) {
                state = state.setValue(connectedBelow, true);
            } else {
                state = state.setValue(connectedBelow, false);
            }
        }

        if (direction == Direction.UP) {
            if (neighborState.getBlock() instanceof BlockTank) {
                state = state.setValue(connectedAbove, true);
            } else {
                state = state.setValue(connectedAbove, false);
            }
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(connectedBelow);
        builder.add(connectedAbove);
        for (Direction i : otherConnections.keySet()) {
            builder.add(otherConnections.get(i));
        }
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_TANK.get().create(blockPos, blockState);
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.notFullBlock;
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityTank::tick;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {


        // Make a single-item copy to operate on
        ItemStack stackCopy = stack.copyWithCount(1);
        IFluidHandlerItem fluidHandlerCopy = stackCopy.getCapability(Capabilities.FluidHandler.ITEM);

        if (fluidHandlerCopy == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (stack.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EntityTank tank) {

            FluidStack fluidInTank = tank.myTank.getFluidInTank(0);
            int tankCapacity = tank.myTank.getCapacity();

            // 1. Try to drain fluid from the item into the tank

            int maxFill = tankCapacity - fluidInTank.getAmount();
            FluidStack drained = fluidHandlerCopy.drain(maxFill, IFluidHandler.FluidAction.EXECUTE);
            int canFill = tank.myTank.fill(drained, IFluidHandler.FluidAction.SIMULATE);
            ItemStack resultItem = fluidHandlerCopy.getContainer();

            // If all drained can fit into the tank, commit!
            if (!drained.isEmpty() && canFill == drained.getAmount()) {
                // Commit the drain, fluid transfer, and item movement
                tank.myTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                stack.shrink(1);
                player.getInventory().placeItemBackInInventory(resultItem);
                return ItemInteractionResult.SUCCESS;
            }

            // 2. If draining did not work, try filling the item instead

            //make new copy because it may have been modified in tee code above
            stackCopy = stack.copyWithCount(1);
            fluidHandlerCopy = stackCopy.getCapability(Capabilities.FluidHandler.ITEM);

            // Execute the fill operation and get the transformed container item
            int filled = fluidHandlerCopy.fill(fluidInTank.copy(), IFluidHandler.FluidAction.EXECUTE);
            resultItem = fluidHandlerCopy.getContainer();

            // Commit the fill, fluid transfer, and item movement
            tank.myTank.drain(fluidInTank.copyWithAmount(filled), IFluidHandler.FluidAction.EXECUTE);
            stack.shrink(1);
            player.getInventory().placeItemBackInInventory(resultItem);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.FAIL;
    }
}