package advRocketry.Dimension;

import advRocketry.Blocks.CompositionFluidLiquidBlock;
import advRocketry.Registry.GasRegistry;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;

public class SeaLevelAdjustment {
    public static String tagKey = "SeaLevelAdjustment";

    // Converts world coordinates into a 0-255 local chunk index
    public static int getLocalIndex(int blockX, int blockZ) {
        return (blockX & 15) | ((blockZ & 15) << 4);
    }

    // Helper to get or initialize the array
    public static int[] getOrInitSeaLevelArray(CompoundTag chunkEntry, String gasId) {
        int[] levels;
        if (chunkEntry.contains(gasId, CompoundTag.TAG_INT_ARRAY)) {
            levels = chunkEntry.getIntArray(gasId);
            if (levels.length == 256) {
                return levels;
            }
        }
        // Initialize new array with your default "no data" value
        levels = new int[256];
        Arrays.fill(levels, -1000);
        return levels;
    }

    // saves the original sea level used by the chunk generator to the chunk tag
    public static void saveInitialWaterLevelOnChunkGeneration(ServerLevel level, ChunkAccess chunk, int blockX, int blockZ) {
        CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, tagKey);
        int[] seaLevels = getOrInitSeaLevelArray(chunkEntry, GasRegistry.water);

        int originalSeaLevel = level.getChunkSource().getGenerator().getSeaLevel();
        seaLevels[getLocalIndex(blockX, blockZ)] = originalSeaLevel;

        chunkEntry.putIntArray(GasRegistry.water, seaLevels);
        ChunkUtils.setEntry(chunk, tagKey, chunkEntry);
    }

    // returns true if a block was placed, false if the xz position is considered fully worked
    public static boolean adjustSeaLevelIfRequired(PlanetDimension planet, GasRegistry.Gas fluid, int blockX, int blockZ, int placementFlags) {


        if (fluid.id.equals(GasRegistry.co2))
            // co2 has its own logic in dry ice block because it can not exist as liquid
            return false;

        Block fluidBlock = fluid.fluidBlock;
        if (fluidBlock == null)
            return false;

        ServerLevel level = planet.level();
        ChunkAccess chunk = level.getChunkAt(new BlockPos(blockX, 0, blockZ));
        CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, tagKey);

        int[] seaLevels = getOrInitSeaLevelArray(chunkEntry, fluid.id);
        int localIndex = getLocalIndex(blockX, blockZ);

        int seaLevelTarget = planet.getGasProperty(fluid.id).worldGenSeaLevel;
        int seaLevelExisting = seaLevels[localIndex];

        if (seaLevelTarget != seaLevelExisting) {
            // sea level has to be possibly adjusted

            // remove fluid above its sea level
            if (seaLevelTarget < seaLevelExisting) {
                for (int scanY = seaLevelExisting; scanY > Math.max(seaLevelTarget, level.getMinBuildHeight()); scanY--) {
                    BlockPos scanPos = new BlockPos(blockX, scanY, blockZ);
                    BlockState scanState = level.getBlockState(scanPos);

                    // special case for water
                    if (fluidBlock.equals(Blocks.WATER) && !scanState.getBlock().equals(Blocks.WATER)) {
                        if (scanState.hasProperty(BlockStateProperties.WATERLOGGED) && scanState.getValue(BlockStateProperties.WATERLOGGED)) {
                            level.setBlock(scanPos, scanState.setValue(BlockStateProperties.WATERLOGGED, false), placementFlags);
                        }
                        if (scanState.is(Blocks.KELP_PLANT) || scanState.is(Blocks.KELP) || scanState.is(Blocks.SEAGRASS) || scanState.is(Blocks.TALL_SEAGRASS)) {
                            level.setBlock(scanPos, Blocks.AIR.defaultBlockState(), placementFlags);
                        }
                    }

                    if (scanState.getBlock().equals(fluidBlock) && scanState.getFluidState().isSource()) {
                        if (fluidBlock instanceof CompositionFluidLiquidBlock) {
                            level.setBlock(scanPos, scanState.setValue(CompositionFluidLiquidBlock.PREVENT_COMPOSITION_CHANGE_ON_BREAK, true), placementFlags);
                        }

                        if (scanPos.getY() < seaLevelExisting) {
                            seaLevels[localIndex] = scanPos.getY();
                            chunkEntry.putIntArray(fluid.id, seaLevels);
                            ChunkUtils.setEntry(chunk, tagKey, chunkEntry);
                        }

                        level.setBlock(scanPos, Blocks.AIR.defaultBlockState(), placementFlags);
                        planet.setClearWeather();
                        return true;
                    }
                }
            }

            // place blocks up to sea level
            if (seaLevelTarget > seaLevelExisting) {
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
                BlockPos blockPos = new BlockPos(blockX, y, blockZ);
                BlockState blockState = level.getBlockState(blockPos);

                while (!blockState.isRedstoneConductor(level, blockPos)) {
                    blockPos = blockPos.below();
                    blockState = level.getBlockState(blockPos);
                    if (blockPos.getY() <= level.getMinBuildHeight())
                        return false;
                }

                for (int scanY = blockPos.above().getY(); scanY <= seaLevelTarget; scanY++) {
                    BlockPos scanPos = new BlockPos(blockX, scanY, blockZ);
                    BlockState scanState = level.getBlockState(scanPos);

                    if (scanState.getBlock().equals(Blocks.LAVA) && level.getBlockState(scanPos.above()).isAir()) {
                        // only replace lava with obsidian if it is top block to allow for lava below surface
                        level.setBlock(scanPos, Blocks.OBSIDIAN.defaultBlockState(), placementFlags);
                        planet.setRaining(5);
                        return true;
                    } else if (scanState.canBeReplaced() && !scanState.getFluidState().isSource()) {
                        BlockState state = fluidBlock.defaultBlockState();
                        if (fluidBlock instanceof CompositionFluidLiquidBlock) {
                            state = state.setValue(CompositionFluidLiquidBlock.PREVENT_COMPOSITION_CHANGE_ON_PLACE, true);
                        }

                        level.setBlock(scanPos, state, placementFlags);
                        planet.setRaining(5);

                        if (scanPos.getY() > seaLevelExisting) {
                            seaLevels[localIndex] = scanPos.getY();
                            chunkEntry.putIntArray(fluid.id, seaLevels);
                            ChunkUtils.setEntry(chunk, tagKey, chunkEntry);
                        }
                        return true;
                    }
                }
                /*
                BlockState existingState1 = level.getBlockState(new BlockPos(blockX, seaLevelTarget, blockZ));
                if(existingState1.getBlock().equals(Blocks.WATER))
                    System.out.println(planet.getName()+" could not place water at "+blockX+":"+seaLevelTarget+":"+blockZ+":"+existingState1);
                 */

            }

            // if no replacement happens, mark completed
            seaLevels[localIndex] = seaLevelTarget;
            chunkEntry.putIntArray(fluid.id, seaLevels);
            ChunkUtils.setEntry(chunk, tagKey, chunkEntry);
            return true;
        }
        return false;
    }
}