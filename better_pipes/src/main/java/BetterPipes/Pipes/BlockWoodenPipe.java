package BetterPipes.Pipes;

import BetterPipes.PipeBase.BlockPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static BetterPipes.Registry.ENTITY_IRON_PIPE;
import static BetterPipes.Registry.ENTITY_WOODEN_PIPE;

public class BlockWoodenPipe extends BlockPipe {
    public BlockWoodenPipe() {
        super();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ENTITY_WOODEN_PIPE.get().create(pos, state);
    }
}
