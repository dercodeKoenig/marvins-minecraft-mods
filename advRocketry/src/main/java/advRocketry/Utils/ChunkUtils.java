package advRocketry.Utils;

import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Registry.GeneralRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static advRocketry.Registry.GeneralRegistry.CUSTOM_CHUNK_DATA;

public class ChunkUtils {
    public static CompoundTag getFullChunkTag(ChunkAccess chunk) {
        return chunk.getData(GeneralRegistry.CUSTOM_CHUNK_DATA);
    }

    public static void setFullChunkTag(ChunkAccess chunk, CompoundTag tag) {
        chunk.setData(GeneralRegistry.CUSTOM_CHUNK_DATA, tag);
    }

    public static CompoundTag getEntryOrNew(ChunkAccess chunk, String key) {
        CompoundTag tag = getFullChunkTag(chunk);
        if (tag.contains(key))
            return tag.getCompound(key);
        return new CompoundTag();
    }

    public static void setEntry(ChunkAccess chunk, String key, CompoundTag tag) {
        CompoundTag fullChunkTag = getFullChunkTag(chunk);
        fullChunkTag.put(key, tag);
        setFullChunkTag(chunk, fullChunkTag);
    }


    public static float getNoiseTemperatureAt(PlanetDimension planet, BlockPos pos) {
        ServerLevel level = DimensionManager.getServerLevel(planet.getDimensionId());

        RandomState randomState = level.getChunkSource().randomState();

        // The sampler evaluates the density functions for the given coordinates
        Climate.Sampler sampler = randomState.sampler();

        // Sample the climate target point
        Climate.TargetPoint targetPoint = sampler.sample(
                QuartPos.fromBlock(pos.getX()),
                QuartPos.fromBlock(pos.getY()),
                QuartPos.fromBlock(pos.getZ())
        );

        // The raw target point values are quantized longs.
        // We unquantize them to get the readable float value (typically ranging from -1.0 to 1.0)
        float temperature = Climate.unquantizeCoord(targetPoint.temperature());

        return temperature;
    }
}
