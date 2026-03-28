package BetterPipes.Pipes;

import BetterPipes.PipeBase.BlockPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static BetterPipes.Registry.ENTITY_IRON_PIPE;

public class BlockIronPipe extends BlockPipe {
    public BlockIronPipe() {
        super();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ENTITY_IRON_PIPE.get().create(pos, state);
    }
}
