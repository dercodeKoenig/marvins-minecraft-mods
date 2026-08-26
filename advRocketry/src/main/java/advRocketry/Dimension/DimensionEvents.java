package advRocketry.Dimension;

import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.Registry.GasRegistry;
import advRocketry.mixins.IceBlockMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public class DimensionEvents {

    // ice block property, can not be defined in ice block mixin so it has to go somewhere...
    // this is used to be able to restore original water / ice based on original world gen in a biome like frozen ocean
    // every water that was frozen to ice from low temperature gets this flag
    // when temperature rises, only ice blocks with this flag will melt into water
    // so player placed ice and original ice generation like ice spikes remains
    public static final BooleanProperty water_frozen_by_low_planet_temp = BooleanProperty.create("water_frozen_by_low_planet_temp");

    // return true if smth meaningful happened and this chunk should be ticked more often over the next seconds
    public static boolean performRandomTickEvents(Dimension dimension, ServerLevel level, ChunkPos chunkPos) {

        boolean requiresIncreasedTickFrequency = false;

        for (int i = 0; i < 6; i++) {

            int localX = level.random.nextIntBetweenInclusive(0, 15);
            int localZ = level.random.nextIntBetweenInclusive(0, 15);

            int blockX = chunkPos.getBlockX(localX);
            int blockZ = chunkPos.getBlockZ(localZ);
            int worldHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
            int blockY = level.random.nextIntBetweenInclusive(
                    // tick only randomly in the top 64 blocks to save on ticks
                    Math.max(level.getMinBuildHeight(), worldHeight - 64),
                    worldHeight
            );

            BlockPos randomPos = new BlockPos(blockX, blockY, blockZ);
            BlockState randomBlockState = level.getBlockState(randomPos);

            // boil away water / ice blocks when too hot
            // the other custom liquids / dry ice have random tick, water has not
            if (randomBlockState.is(Blocks.WATER) || randomBlockState.is(Blocks.ICE)) {
                if(dimension.shouldBoilBlocks(GasRegistry.water, randomPos)){
                    level.setBlock(randomPos, Blocks.AIR.defaultBlockState(), 3);
                    randomBlockState = level.getBlockState(randomPos);
                    requiresIncreasedTickFrequency = true;
                }
            }

            // freeze water to ice with custom blockstate
            if (randomBlockState.is(Blocks.WATER)) {
                if(dimension.shouldFreezeBlocks(GasRegistry.water, randomPos)){
                    boolean hasWaterAllAround = level.isWaterAt(randomPos.west()) && level.isWaterAt(randomPos.east()) && level.isWaterAt(randomPos.north()) && level.isWaterAt(randomPos.south());
                    if (!hasWaterAllAround) { // freeze from edge first
                        level.setBlock(randomPos, Blocks.ICE.defaultBlockState().setValue(water_frozen_by_low_planet_temp, true), 3);
                        randomBlockState = level.getBlockState(randomPos);
                        requiresIncreasedTickFrequency = true;
                    }
                }
            }

            // melt any ice placed by the force freeze logic above back into water
            if (randomBlockState.is(Blocks.ICE)) {
                if (!dimension.shouldFreezeBlocks(GasRegistry.water, randomPos)) {
                    if (randomBlockState.getValue(water_frozen_by_low_planet_temp)) {
                        if (dimension instanceof PlanetDimension planet && planet.getGasProperty(GasRegistry.water).worldGenSeaLevel < randomPos.getY()) {
                            // above sea level melt into air and not water or it would look really strange to have water flowing everywhere
                            level.setBlock(randomPos, Blocks.AIR.defaultBlockState(), 3);
                        } else {
                            level.setBlock(randomPos, Blocks.WATER.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
        return requiresIncreasedTickFrequency;
    }
}
