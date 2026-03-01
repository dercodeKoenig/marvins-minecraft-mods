package advRocketry.Blocks;

import advRocketry.BlockEntities.EntityRocketItemLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static advRocketry.Registry.ENTITY_ROCKET_ITEM_LOADER;

public class RocketItemLoader extends Block implements EntityBlock {

    public static BooleanProperty IS_DRAIN = BooleanProperty.create("is_drain");

    public RocketItemLoader() {
        super(Properties.of());
        BlockState state = getStateDefinition().any().setValue(IS_DRAIN, false);
        registerDefaultState(state);
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(IS_DRAIN);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_ROCKET_ITEM_LOADER.get().create(blockPos, blockState);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EntityRocketItemLoader rocketItemLoader) {
            if (rocketItemLoader.shouldOutputSignal) {
                return 15;
            }
        }
        return 0;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide){
            if (level.getBlockEntity(pos) instanceof EntityRocketItemLoader itemLoader) {
                itemLoader.guiHandler.openGui(176, 148, true);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof EntityRocketItemLoader itemLoader) {
                if (!newState.getBlock().equals(state.getBlock())) {
                    itemLoader.popInventory();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityRocketItemLoader::tick;
    }
}
