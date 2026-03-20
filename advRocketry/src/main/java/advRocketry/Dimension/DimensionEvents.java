package advRocketry.Dimension;

import advRocketry.Blocks.DryIceBlock;
import advRocketry.Config;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.level.ChunkEvent;

public class DimensionEvents {

    // called from server level mixin
    public static void performRandomTickEvents(Dimension dimension, ServerLevel level, LevelChunk chunk) {

        ChunkPos chunkPos = chunk.getPos();

        int localX = level.random.nextIntBetweenInclusive(0,15);
        int localZ = level.random.nextIntBetweenInclusive(0,15);

        int blockX = chunkPos.getBlockX(localX);
        int blockZ = chunkPos.getBlockZ(localZ);
        int blockY = level.random.nextIntBetweenInclusive(level.getMinBuildHeight() + 1, level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ));

        BlockPos randomPos = new BlockPos(blockX, blockY, blockZ);
        BlockState randomBlockState = level.getBlockState(randomPos);

        double temp = dimension.getCurrentTemp();
        double pressure = dimension.getAtmosphereDensity();
        if(LifeSupportSystem.isTemperatureRegulated(level,randomPos))
            temp = 300;
        if(LifeSupportSystem.isPressureRegulated(level,randomPos))
            pressure = 1;

        // boil away water blocks when too hot
        // the other custom liquids / dry ice have random tick, water has not
        if (randomBlockState.getBlock().equals(Blocks.WATER) && temp > 1 + GasRegistry.gases.get(GasRegistry.water).getBoilingTemp(pressure)) {
            level.setBlock(randomPos, Blocks.AIR.defaultBlockState(), 3);
            randomBlockState = level.getBlockState(randomPos);
        }
    }
}
