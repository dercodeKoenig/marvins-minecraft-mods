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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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

public class ConveyorBelt extends Block implements EntityBlock , ItemHammer.HammerInteractionBlock {

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
        tooltipComponents.add(Component.literal("Max Stress: "+ ConveyorConfig.INSTANCE.conveyorMaxStress));
        tooltipComponents.add(Component.literal("Friction: "+ConveyorConfig.INSTANCE.conveyorResistance));
        tooltipComponents.add(Component.literal("Inertia: "+ConveyorConfig.INSTANCE.conveyorInertia));
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
        if (placer != null) {
            state = state.setValue(FACING, placer.getDirection());
            state = state.setValue(DIAGONAL, false);
            level.setBlock(pos, state, 3);
        }
        super.setPlacedBy(level, pos, state, placer, stack); // Call the super method for any additional behavior
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof EntityConveyorBelt conveyorBelt) {
            for(Long id : new ArrayList<>(conveyorBelt.id_items.keySet())) {
                conveyorBelt.popItem(id, Direction.UP);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityConveyorBelt::tick;
    }

    VoxelShape myShape = Shapes.create(0, 0, 0, 1, (double) 2 / 16, 1);

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if(state.getValue(DIAGONAL))
            return Shapes.block();
        else
            return myShape;
    }

    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public InteractionResult onHammer(ItemStack itemStack, Level level, BlockPos blockPos, BlockState blockState, Player player, InteractionHand interactionHand) {
        if(player == null)return InteractionResult.PASS;

        BlockState block = level.getBlockState(blockPos);
        if(block.getBlock() instanceof ConveyorBelt){
            if(player.isShiftKeyDown())
                block = block.setValue(DIAGONAL, !block.getValue(DIAGONAL));
            else
                block = block.setValue(FACING, block.getValue(FACING).getClockWise());
            level.setBlock(blockPos, block, 3);

            // after state change, make sure the BE updates its meshdata for render
            BlockEntity entity = level.getBlockEntity(blockPos);
            if(entity instanceof EntityConveyorBelt entityConveyorBelt)
                entityConveyorBelt.requiresMeshUpdate = true;
        }
        return InteractionResult.SUCCESS;
    }
}
