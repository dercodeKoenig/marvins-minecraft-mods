package advRocketry.Blocks;

import ARLib.multiblockCore.BlockMultiblockMaster;
import advRocketry.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class Observatory extends BlockMultiblockMaster {
    public Observatory() {
        super(Properties.of());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return Registry.ENTITY_OBSERVATORY.get().create(blockPos, blockState);
    }
}
