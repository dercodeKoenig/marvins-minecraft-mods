package AOSWorkshopExpansion.Conveyor;

import AgeOfSteam.Items.Hammer.ItemHammer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static AOSWorkshopExpansion.Registry.ENTITY_CONVEYOR_BELT;

public class ConveyorBelt extends Block implements EntityBlock, ItemHammer.HammerInteractionBlock {

    public static EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static BooleanProperty DIAGONAL = BooleanProperty.create("diagonal");

    public ConveyorBelt() {
        super(Properties.of().noOcclusion());
        BlockState state = this.stateDefinition.any();
        state = state.setValue(FACING, Direction.NORTH).setValue(DIAGONAL, false);
        this.registerDefaultState(state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Max Stress: " + ConveyorConfig.INSTANCE.conveyorMaxStress));
        tooltipComponents.add(Component.literal("Friction: " + ConveyorConfig.INSTANCE.conveyorResistance));
        tooltipComponents.add(Component.literal("Inertia: " + ConveyorConfig.INSTANCE.conveyorInertia));
        tooltipComponents.add(Component.literal("place above conveyor engine"));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_CONVEYOR_BELT.get().create(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(DIAGONAL);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos, updateFromNeighbourShapes(state, level, pos),3);
        // this should now update neighbor blocks of the change
        // but diagonal blocks could update too, so we need to update them ourselves
        for(Direction i : Direction.values()){
            if(i == Direction.UP || i == Direction.DOWN) continue;
            for (int y : List.of(-1,1)) {
                BlockPos otherPos = (pos.relative(i).relative(Direction.UP, y));
                BlockState otherState = level.getBlockState(otherPos);
                otherState = Block.updateFromNeighbourShapes(otherState,level,otherPos);
                level.setBlock(otherPos,otherState,3);
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        state = state.setValue(FACING, context.getHorizontalDirection());
        state = state.setValue(DIAGONAL, false);
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        if (direction == Direction.DOWN || direction == Direction.UP)
            return state;

        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof EntityConveyorBelt conveyorBelt) {
            if (!conveyorBelt.getConnectedParts(conveyorBelt, null).isEmpty() && direction.getAxis() != state.getValue(FACING).getAxis()) {
                // can not rotate to a different direction if already connected
                // but we still can see if we might need to change the diagonal
                return state;
            }
        }

        for (int y : List.of(-1, 1, 0)) {
            BlockEntity other = level.getBlockEntity(neighborPos.relative(Direction.UP, y));
            if (other instanceof EntityConveyorBelt otherBelt) {
                // if the neighbor aligns with the axis we rotate towards it
                // if the neighbor has no connected parts, we rotate there even if it doesn't align because it will align in next tick
                if (otherBelt.getConnectedParts(otherBelt, null).isEmpty() || other.getBlockState().getValue(FACING).getAxis() == direction.getAxis()) {
                    if (y == 1) {
                        // rotate in the direction to align the diagonal
                        state = state.setValue(DIAGONAL, true);
                        state = state.setValue(FACING, direction);
                    } else {
                        // diagonal is not required, so rotate away in case the diagonal is required on other side
                        state = state.setValue(FACING, direction.getOpposite());
                        // to know if we should be diagonal we need to check the block on the other direction and the one above
                        BlockState otherDirectionAbove = level.getBlockState(pos.relative(direction.getOpposite()).above());
                        if (otherDirectionAbove.getBlock() instanceof ConveyorBelt)
                            state = state.setValue(DIAGONAL, true);
                        else
                            state = state.setValue(DIAGONAL, false);
                    }
                }
            }
        }
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof EntityConveyorBelt conveyorBelt) {
            for (Long id : new ArrayList<>(conveyorBelt.id_items.keySet())) {
                ItemStack item = conveyorBelt.id_items.get(id);
                conveyorBelt.popItem(item, Direction.UP);
                conveyorBelt.removeItem(id, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityConveyorBelt::tick;
    }

    VoxelShape myShape = Shapes.create(0, 0, 0, 1, (double) 2 / 16, 1);
    VoxelShape myShapeDiagonal = Shapes.create(0, 0, 0, 1, (double) 8 / 16, 1);

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if(state.getValue(DIAGONAL))
            return myShapeDiagonal;
        else
            return myShape;
    }

    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public InteractionResult onHammer(ItemStack itemStack, Level level, BlockPos blockPos, BlockState blockState, Player player, InteractionHand interactionHand) {
        if (player == null) return InteractionResult.PASS;
        BlockState block = level.getBlockState(blockPos);
        if (block.getBlock() instanceof ConveyorBelt) {
            if (player.isShiftKeyDown()) {
                block = block.setValue(DIAGONAL, !block.getValue(DIAGONAL));
            }else
                block = block.setValue(FACING, block.getValue(FACING).getClockWise());
            level.setBlock(blockPos, block, 3);
        }
        return InteractionResult.SUCCESS;
    }
}
