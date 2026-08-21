package advRocketry.Blocks;

import ARLib.multiblockCore.BlockMultiblockMaster;
import advRocketry.BlockEntities.EntityLaserDrill;
import advRocketry.Registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LaserDrill extends BlockMultiblockMaster {
    public LaserDrill() {
        super(Properties.of().destroyTime(0.5f));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return BlockEntities.ENTITY_LASERDRILL.get().create(blockPos, blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityLaserDrill::tick;
    }
}
