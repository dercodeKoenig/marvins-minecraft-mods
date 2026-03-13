package advRocketry.Dimension.Terraforming;

import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.GasRegistry;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class DryIcePlacement {

    public static String dryIceDataTag = "dryIceDataTag";
    public static float epsilon = 0.0001f;

    public static float getNoiseThreshold(float frozen_co2_level) {
        return (frozen_co2_level * 2 - 1); // -1 to +1 (or higher)
    }

    public static void placeDryIceIfPossible(PlanetDimension planet, int blockX, int blockZ) {
        BlockPos pos0 = new BlockPos(blockX, 0, blockZ);
        ServerLevel level = DimensionManager.getServerLevel(planet.getDimensionId());
        ChunkAccess chunk = level.getChunk(pos0);
        CompoundTag chunkData = ChunkUtils.getEntryOrNew(chunk, dryIceDataTag);

        // we place dry ice based on how much gas is frozen on the surface and the niose temperature

        // we only place dry ice if the frozen value increased since last placement
        // this avoids re-placing blocks that the player mined away

        // but it should place blocks again if the frozen co2 level increased since last placement
        // this would mean it has snowed new dry ice so a mined position can be covered up again

        // we do not place a new dry ice block if there is already one at surface to avoid stacking
        // them up to infinity every time the value increases a bit

        // dry ice should remove itself on block tick when the frozen gas coverage no longer meets the noise temperature

        float frozen_co2_level = planet.getGasProperty(GasRegistry.co2).frozen_surface;
        float frozen_co2_level_at_last_placement = 0; // default
        String positionKey = String.valueOf(pos0.asLong());
        if (chunkData.contains(positionKey)) {
            frozen_co2_level_at_last_placement = chunkData.getFloat(positionKey);
        }

        if (frozen_co2_level > frozen_co2_level_at_last_placement + epsilon) {
            // requires consideration for placement because co2 level on surface increased since last placement
            float noiseTemperature = ChunkUtils.getNoiseTemperatureAt(planet, pos0); // -1 to 1
            float noiseThreshold = getNoiseThreshold(frozen_co2_level);
            if (noiseTemperature < noiseThreshold) {
                // this regions is valid to have a dry ice block
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
                BlockPos topBlock = new BlockPos(blockX, y - 1, blockZ);
                BlockState existingState = level.getBlockState(topBlock);
                if (!(existingState.getBlock() instanceof DryIceBlock)) {
                    // the top block is no dry ice block, place one above surface!
                    level.setBlock(topBlock.above(), DryIceBlock.defaultBlockState(), 3);
                }
            }

            // increase the level in the tag data so we do not process this position again if the block was broken
            // the dry ice block is responsible to decrease this value for its position on evaporation
            chunkData.putFloat(positionKey, frozen_co2_level);
        }
    }
}
