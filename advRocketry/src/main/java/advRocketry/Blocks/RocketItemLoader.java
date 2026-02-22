package advRocketry.Blocks;

import ARLib.blocks.BlockItemInputBlock;
import ARLib.multiblockCore.BlockMultiblockMaster;
import advRocketry.BlockEntities.EntityFuelingStation;
import advRocketry.BlockEntities.EntityRocketItemLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import static advRocketry.Registry.ENTITY_ROCKET_ITEM_LOADER;

public class RocketItemLoader extends BlockItemInputBlock implements EntityBlock {

    public static BooleanProperty IS_DRAIN = BooleanProperty.create("is_drain");

    public RocketItemLoader() {
        super(Properties.of());
        BlockState state = getStateDefinition().any()
                .setValue(IS_DRAIN, false)
                .setValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED, false);
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityRocketItemLoader::tick;
    }
}
