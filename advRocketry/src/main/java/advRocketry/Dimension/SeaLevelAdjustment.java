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
            System.out.println(GlobalTime.getGlobalTime() + ":" + planet.getName() + ":" + blockX + ":" + blockZ + " requires sea level update: " + seaLevelExisting + ":" + seaLevelTarget);
            // sea level has to be possibly adjusted

            // rules:
            // only top blocks will be evaporated or placed
            // when lava is top block, we replace lava with stone downward until we hit a non lava top block

            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
            BlockPos topBlockPos = new BlockPos(blockX, y - 1, blockZ);
            BlockState blockState = level.getBlockState(topBlockPos);

            // adjust top block pos lower while there is lava
            while (blockState.getBlock().equals(Blocks.LAVA)) {
                BlockPos below = topBlockPos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.getBlock().equals(Blocks.LAVA)) {
                    blockState = belowState;
                    topBlockPos = below;
                }else
                    // breaks once the block below the adjusted top block is not lava
                    break;
            }
            System.out.println(blockState);

            if (topBlockPos.getY() > seaLevelTarget) {
                if (blockState.getBlock().equals(Blocks.WATER) && blockState.getFluidState().isSource()) {
                    // water above target sea level requires to be removed
                    level.setBlock(topBlockPos, Blocks.AIR.defaultBlockState(), placementFlags);
                    return true;
                }
            } else {
                // when below or equal to target sea level,
                // replace lava with obsidian
                // replace air with water
                if (blockState.getBlock().equals(Blocks.LAVA)) {
                    level.setBlock(topBlockPos, Blocks.OBSIDIAN.defaultBlockState(), placementFlags);
                    planet.setRaining();
                    return true;
                } else if (blockState.getBlock().equals(Blocks.WATER) && !blockState.getFluidState().isSource()) {
                    level.setBlock(topBlockPos, Blocks.WATER.defaultBlockState(), placementFlags);
                    planet.setRaining();
                    return true;
                }else if(!blockState.getBlock().equals(Blocks.WATER) && blockState.canBeReplaced()){
                    level.setBlock(topBlockPos, Blocks.WATER.defaultBlockState(), placementFlags);
                    planet.setRaining();
                    return true;
                }
                else{
                    // place water above the top block position, as the top block is never air
                    //the blockstate above top block should always be air
                    BlockPos aboveTopBlockPos = topBlockPos.above();
                    if (aboveTopBlockPos.getY() <= seaLevelTarget) {
                        level.setBlock(aboveTopBlockPos, Blocks.WATER.defaultBlockState(), placementFlags);
                        planet.setRaining();
                        return true;
                    }
                }
            }
        }

        // if no replacement happen, mark the position as completed
        chunkEntry.putInt(positionKey, seaLevelTarget);
        ChunkUtils.setEntry(chunk, tagKey, chunkEntry);

        return false;
    }
}
