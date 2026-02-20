package AOSWorkshopExpansion.Conveyor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static AOSWorkshopExpansion.Registry.ENTITY_CONVEYOR_ENGINE;


public class ConveyorEngine extends Block implements EntityBlock {
    public static EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public ConveyorEngine() {
        super(Properties.of().noOcclusion().strength(1.0f));
        BlockState state = this.stateDefinition.any();
        state = state.setValue(AXIS, Direction.Axis.X);
        this.registerDefaultState(state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Max Stress: "+ ConveyorConfig.INSTANCE.conveyorEngineMaxStress));
        tooltipComponents.add(Component.literal("Friction: "+ConveyorConfig.INSTANCE.conveyorEngineResistance));
        tooltipComponents.add(Component.literal("Inertia: "+ConveyorConfig.INSTANCE.conveyorEngineInertia));
        tooltipComponents.add(Component.literal("place below conveyor belt"));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_CONVEYOR_ENGINE.get().create(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
        super.createBlockStateDefinition(builder);
    }


    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer != null) {
            state = state.setValue(AXIS, placer.getDirection().getAxis());
            level.setBlock(pos, state, 3);
        }
        super.setPlacedBy(level, pos, state, placer, stack); // Call the super method for any additional behavior
    }



    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityConveyorEngine::tick;
    }
}
