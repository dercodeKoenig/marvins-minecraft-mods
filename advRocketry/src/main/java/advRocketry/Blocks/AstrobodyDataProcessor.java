package advRocketry.Blocks;

import ARLib.multiblockCore.BlockMultiblockMaster;
import advRocketry.BlockEntities.EntityAstrobodyDataProcessor;
import advRocketry.Registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AstrobodyDataProcessor extends BlockMultiblockMaster {
    public AstrobodyDataProcessor() {
        super(Properties.of());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return BlockEntities.ENTITY_ASTROBODY_DATA_PROCESSOR.get().create(blockPos, blockState);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if(level.getBlockEntity(pos) instanceof  EntityAstrobodyDataProcessor processor && !processor.isValidBlockState(newState)){
            processor.popInventory();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityAstrobodyDataProcessor::tick;
    }
}
