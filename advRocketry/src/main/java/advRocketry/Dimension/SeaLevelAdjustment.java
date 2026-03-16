package advRocketry.Dimension;

import advRocketry.Blocks.CompositionLiquidBlock;
import advRocketry.Registry.GasRegistry;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class SeaLevelAdjustment {
    public static String tagKey = "SeaLevelAdjustment";

    // saves the original sea level used by the chunk generator to the chunk tag
    public static void saveInitialWaterLevelOnChunkGeneration(ServerLevel level, ChunkAccess chunk, int blockX, int blockZ) {
        CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, tagKey);
        BlockPos pos0 = new BlockPos(blockX, 0, blockZ);
        String positionKey = pos0.asLong() + GasRegistry.water;
        int originalSeaLevel = level.getChunkSource().getGenerator().getSeaLevel();
        chunkEntry.putInt(positionKey, originalSeaLevel);
        ChunkUtils.setEntry(chunk, tagKey, chunkEntry);
    }

    // returns true if a block was placed, false if the xz position is considered fully worked
    public static boolean adjustSeaLevelIfRequired(PlanetDimension planet, GasRegistry.Gas fluid, int blockX, int blockZ, int placementFlags) {

        if(fluid.id.equals(GasRegistry.co2))
            // co2 has its own logic in dry ice block because it can not exist as liquid
            return false;

        Block fluidBlock = fluid.fluidBlock;
        if (fluidBlock == null)
            return false;

        BlockPos pos0 = new BlockPos(blockX, 0, blockZ);
        ServerLevel level = planet.level();
        ChunkAccess chunk = level.getChunk(pos0);
        CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, tagKey);
        String positionKey = pos0.asLong() + fluid.id;

        int seaLevelTarget = planet.getGasProperty(fluid.id).worldGenSeaLevel;
        int seaLevelExisting = -1000;
        if (chunkEntry.contains(positionKey))
            seaLevelExisting = chunkEntry.getInt(positionKey);

        if (seaLevelTarget != seaLevelExisting) {
            // sea level has to be possibly adjusted

            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
            BlockPos blockPos = new BlockPos(blockX, y - 1, blockZ);
            BlockState blockState = level.getBlockState(blockPos);

            // remove any fluid above its sea level
            for (int scanY = blockPos.getY(); scanY > seaLevelTarget; scanY--) {
                BlockPos scanPos = new BlockPos(blockX, scanY, blockZ);
                BlockState scanState = level.getBlockState(scanPos);
                if (scanState.getBlock().equals(fluidBlock) &&
                        scanState.getFluidState().isSource()) {
                    // fluid above target sea level requires to be removed
                    if(fluidBlock instanceof CompositionLiquidBlock){
                        // make sure it doesn't modify atmosphere before deleting it
                        level.setBlock(scanPos, scanState.setValue(CompositionLiquidBlock.PREVENT_COMPOSITION_CHANGE_ON_BREAK, true), placementFlags);
                    }
                    level.setBlock(scanPos, Blocks.AIR.defaultBlockState(), placementFlags);
                    planet.setClearWeather();
                    return true;
                }
            }

            // adjust top block pos lower while there is not full block
            while (!blockState.isRedstoneConductor(level, blockPos)) {
                blockPos = blockPos.below();
                blockState = level.getBlockState(blockPos);
                if (blockPos.getY() <= level.getMinBuildHeight())
                    return false;
            }
            //System.out.println(blockState + ":" + topSolidOrWaterBlockPos);

            // scan back up to find the first replaceable block above the first solid block that is not already a fluid source
            for (int scanY = blockPos.above().getY(); scanY <= seaLevelTarget; scanY++) {
                BlockPos scanPos = new BlockPos(blockX, scanY, blockZ);
                BlockState scanState = level.getBlockState(scanPos);

                if (scanState.getBlock().equals(Blocks.LAVA)) {
                    // special case: lave is replaced with obsidian
                    level.setBlock(scanPos, Blocks.OBSIDIAN.defaultBlockState(), placementFlags);
                    planet.setRaining();
                    return true;
                } else if (scanState.canBeReplaced() && !scanState.getFluidState().isSource()) {
                    BlockState state = fluidBlock.defaultBlockState();
                    if(fluidBlock instanceof CompositionLiquidBlock){
                        state = state.setValue(CompositionLiquidBlock.PREVENT_COMPOSITION_CHANGE_ON_PLACE, true);
                    }
                    level.setBlock(scanPos, state, placementFlags);
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
