package advRocketry.Blocks;

import advRocketry.Registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LaunchStationAsteroidMissions extends LaunchStation {

    public LaunchStationAsteroidMissions() {
        super(Properties.of()
            .destroyTime(2.0f)
            .requiresCorrectToolForDrops()
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return BlockEntities.ENTITY_LAUNCH_STATION_ASTEROID_MISSIONS.get().create(blockPos, blockState);
    }
}
