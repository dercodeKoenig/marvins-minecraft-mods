package BetterPipes.Pipes;

import BetterPipes.Config;
import BetterPipes.PipeBase.EntityPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import static BetterPipes.Registry.ENTITY_IRON_PIPE;

public class EntityIronPipe extends EntityPipe {
    public EntityIronPipe(BlockPos pos, BlockState blockState) {
        super(ENTITY_IRON_PIPE.get(), pos, blockState, Config.INSTANCE.mainCapacityIronPipe, Config.INSTANCE.flowRateIronPipe);
    }
}
