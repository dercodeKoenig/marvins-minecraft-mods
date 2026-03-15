package advRocketry.Dimension;

import advRocketry.GlobalTime;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class SeaLevelAdjustment {
    public static String tagKey = "SeaLevelAdjustment";

    // saves the original sea level used by the chunk generator to the chunk tag
    public static void saveInitialSeaLevelOnChunkGeneration(ServerLevel level, ChunkAccess chunk, int blockX, int blockZ) {
        CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, tagKey);
        BlockPos pos0 = new BlockPos(blockX, 0, blockZ);
        String positionKey = String.valueOf(pos0.asLong());
        int originalSeaLevel = level.getChunkSource().getGenerator().getSeaLevel();
        chunkEntry.putInt(positionKey, originalSeaLevel);
        ChunkUtils.setEntry(chunk, tagKey, chunkEntry);
    }

    // returns true if a block was placed, false if the xz position is considered fully worked
    public static boolean adjustSeaLevelIfRequired(PlanetDimension planet, int blockX, int blockZ, int placementFlags) {
        BlockPos pos0 = new BlockPos(blockX, 0, blockZ);
        ServerLevel level = planet.level();
        ChunkAccess chunk = level.getChunk(pos0);
        CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, tagKey);
        String positionKey = String.valueOf(pos0.asLong());

        int seaLevelTarget = planet.getWorldgenSeaLevel();
        int seaLevelExisting = -100;
        if (chunkEntry.contains(positionKey))
            // the position should be initially saved on chunk generation, so it should always be here
            seaLevelExisting = chunkEntry.getInt(positionKey);


        if (seaLevelTarget != seaLevelExisting) {
            //System.out.println(GlobalTime.getGlobalTime() + ":" + planet.getName() + ":" + blockX + ":" + blockZ + " requires sea level update: " + seaLevelExisting + ":" + seaLevelTarget);
            // sea level has to be possibly adjusted

            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
            BlockPos topSolidOrWaterBlockPos = new BlockPos(blockX, y - 1, blockZ);
            BlockState blockState = level.getBlockState(topSolidOrWaterBlockPos);

            // remove any water above sea level
            for (int scanY = topSolidOrWaterBlockPos.getY(); scanY > seaLevelTarget; scanY--) {
                BlockPos scanPos = new BlockPos(blockX, scanY, blockZ);
                BlockState scanState = level.getBlockState(scanPos);
                if (scanState.getBlock().equals(Blocks.WATER) &&
                        scanState.getFluidState().isSource()) {
                    // water above target sea level requires to be removed
                    level.setBlock(scanPos, Blocks.AIR.defaultBlockState(), placementFlags);
                    planet.setClearWeather();
                    return true;
                }
            }

            // adjust top block pos lower while there is not full blocks or no water source
            while (!blockState.isRedstoneConductor(level, topSolidOrWaterBlockPos) &&
                    !(blockState.getBlock().equals(Blocks.WATER) && blockState.getFluidState().isSource())) {
                topSolidOrWaterBlockPos = topSolidOrWaterBlockPos.below();
                blockState = level.getBlockState(topSolidOrWaterBlockPos);
                if (topSolidOrWaterBlockPos.getY() <= level.getMinBuildHeight())
                    return false;
            }
            //System.out.println(blockState + ":" + topSolidOrWaterBlockPos);

            // scan back up to find the first replaceable block above the first solid block
            for (int scanY = topSolidOrWaterBlockPos.above().getY(); scanY <= seaLevelTarget; scanY++) {
                BlockPos scanPos = new BlockPos(blockX, scanY, blockZ);
                BlockState scanState = level.getBlockState(scanPos);

                if (scanState.getBlock().equals(Blocks.LAVA)) {
                    level.setBlock(scanPos, Blocks.OBSIDIAN.defaultBlockState(), placementFlags);
                    planet.setRaining();
                    return true;
                } else if (scanState.canBeReplaced()) {
                    level.setBlock(scanPos, Blocks.WATER.defaultBlockState(), placementFlags);
                    planet.setRaining();
                    return true;
                }
            }
        }

        // if no replacement happen, mark the position as completed
        chunkEntry.putInt(positionKey, seaLevelTarget);
        ChunkUtils.setEntry(chunk, tagKey, chunkEntry);

        return false;
    }
}
