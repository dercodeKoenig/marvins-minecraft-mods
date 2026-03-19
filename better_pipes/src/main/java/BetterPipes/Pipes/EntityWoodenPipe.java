package BetterPipes.Pipes;

import BetterPipes.Config;
import BetterPipes.PipeBase.EntityPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import static BetterPipes.Registry.ENTITY_IRON_PIPE;
import static BetterPipes.Registry.ENTITY_WOODEN_PIPE;

public class EntityWoodenPipe extends EntityPipe {
    public EntityWoodenPipe(BlockPos pos, BlockState blockState) {
        super(ENTITY_WOODEN_PIPE.get(), pos, blockState, Config.INSTANCE.mainCapacityWoodenPipe, Config.INSTANCE.flowRateWoodenPipe);
    }
}
