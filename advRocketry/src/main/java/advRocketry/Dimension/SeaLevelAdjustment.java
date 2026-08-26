package advRocketry.Dimension;

import advRocketry.Blocks.CompositionFluidLiquidBlock;
import advRocketry.Registry.GasRegistry;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

        // the world generator sea level actually is the block above the water block level so -1 to get true level
        int originalSeaLevel = level.getChunkSource().getGenerator().getSeaLevel() - 1;
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
            // System.out.println("adjusting sea level for "+fluid.id+":"+seaLevelExisting+":"+seaLevelTarget+":"+blockX+":"+blockZ);

            // remove fluid above its sea level
            if (seaLevelTarget < seaLevelExisting) {
                for (int scanY = seaLevelExisting; scanY > Math.max(seaLevelTarget, level.getMinBuildHeight()); scanY--) {
                    BlockPos scanPos = new BlockPos(blockX, scanY, blockZ);
                    BlockState scanState = level.getBlockState(scanPos);

                    // special case for water
                    if (fluidBlock.equals(Blocks.WATER) && !scanState.getBlock().equals(Blocks.WATER)) {
                        // remove waterlogged state
                        if (scanState.hasProperty(BlockStateProperties.WATERLOGGED) && scanState.getValue(BlockStateProperties.WATERLOGGED)) {
                            level.setBlock(scanPos, scanState.setValue(BlockStateProperties.WATERLOGGED, false), placementFlags);
                            return true;
                        }
                        // remove special sea plants
                        if (scanState.is(Blocks.KELP_PLANT) || scanState.is(Blocks.KELP) || scanState.is(Blocks.SEAGRASS) || scanState.is(Blocks.TALL_SEAGRASS)) {
                            level.setBlock(scanPos, Blocks.AIR.defaultBlockState(), placementFlags);
                            return true;
                        }
                        // floating ice / entire floating ice structures on water should move down
                        BlockState belowState = level.getBlockState(scanPos.below());
                        if (scanState.is(Blocks.ICE) && (belowState.is(Blocks.WATER) || belowState.is(Blocks.ICE))) {
                            for (int iceY = 0; iceY < 9999; iceY++) {
                                BlockPos icePos = scanPos.relative(Direction.UP, iceY);
                                BlockState iceState = level.getBlockState(icePos);
                                if (iceState.is(Blocks.ICE)) {
                                    // preserve mixin BlockState
                                    level.setBlock(icePos, Blocks.AIR.defaultBlockState(), 3);
                                    level.setBlock(icePos.below(), iceState, 3);
                                } else {
                                    break;
                                }
                            }
                            return true;
                        }
                    }

                    if (scanState.getBlock().equals(fluidBlock) && scanState.getFluidState().isSource()) {
                        if (fluidBlock instanceof CompositionFluidLiquidBlock) {
                            level.setBlock(scanPos, scanState.setValue(CompositionFluidLiquidBlock.PREVENT_COMPOSITION_CHANGE_ON_BREAK, true), placementFlags);
                        }

                        // important to instantly save the lowered sea level in case we increase it again
                        // (it would not increase sea level in future)
                        if (scanPos.getY() < seaLevelExisting) {
                            seaLevels[localIndex] = scanPos.getY();
                            chunkEntry.putIntArray(fluid.id, seaLevels);
                            ChunkUtils.setEntry(chunk, tagKey, chunkEntry);
                        }

                        level.setBlock(scanPos, Blocks.AIR.defaultBlockState(), placementFlags);
                        return true;
                    }
                }
            }

            // place blocks up to sea level
            if (seaLevelTarget > seaLevelExisting) {

                BlockState stateToPlace = fluidBlock.defaultBlockState();
                if (fluidBlock instanceof CompositionFluidLiquidBlock) {
                    stateToPlace = stateToPlace.setValue(CompositionFluidLiquidBlock.PREVENT_COMPOSITION_CHANGE_ON_PLACE, true);
                }

                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
                BlockPos blockPos = new BlockPos(blockX, y, blockZ);
                BlockState blockState = level.getBlockState(blockPos);

                while (!blockState.isRedstoneConductor(level, blockPos)) {
                    blockPos = blockPos.below();
                    blockState = level.getBlockState(blockPos);
                    if (blockPos.getY() <= level.getMinBuildHeight())
                        break;
                }

                for (int scanY = blockPos.above().getY(); scanY <= seaLevelTarget; scanY++) {
                    BlockPos scanPos = new BlockPos(blockX, scanY, blockZ);
                    BlockState scanState = level.getBlockState(scanPos);

                    // correct block is already placed at this position
                    if (scanState.getFluidState().isSource() && scanState.is(fluidBlock))
                        continue;

                    if (scanState.getBlock().equals(Blocks.LAVA) && level.getBlockState(scanPos.above()).isAir()) {
                        // only replace lava with obsidian if it is top block to allow for lava below surface
                        level.setBlock(scanPos, Blocks.OBSIDIAN.defaultBlockState(), placementFlags);
                        planet.setRaining(5);
                        return true;
                    }

                    if (!scanState.canBeReplaced()) {
                        continue;
                    }

                    // only replace source blocks of gases with lower boiling temp
                    // so that when the low boiling temp gases evaporate, they leave behind the other fluids
                    // not perfect but should be good enough....
                    // best would be to invalidate the current sea level for all fluids when any fluid lowers its sea level
                    // so that all others can maybe fill the void. but thats a bit too complicated for now... this approximation will do
                    if (scanState.getFluidState().isSource()) {
                        double atmDensity = planet.getAtmosphereDensity();
                        boolean canReplaceSource = true;
                        for (GasRegistry.Gas otherGas : GasRegistry.gases.values()) {
                            if (scanState.getBlock().equals(otherGas.fluidBlock)) {
                                if (otherGas.getBoilingTemp(atmDensity) > fluid.getBoilingTemp(atmDensity)) {
                                    canReplaceSource = false;
                                    break;
                                }
                            }
                        }
                        if (!canReplaceSource)
                            continue;
                    }

                    // prevent composition change on replacing
                    if (scanState.hasProperty(CompositionFluidLiquidBlock.PREVENT_COMPOSITION_CHANGE_ON_BREAK)) {
                        level.setBlock(scanPos, scanState.setValue(CompositionFluidLiquidBlock.PREVENT_COMPOSITION_CHANGE_ON_BREAK, true), placementFlags);
                    }

                    level.setBlock(scanPos, stateToPlace, placementFlags);
                    planet.setRaining(5);

                    if (scanPos.getY() > seaLevelExisting) {
                        seaLevels[localIndex] = scanPos.getY();
                        chunkEntry.putIntArray(fluid.id, seaLevels);
                        ChunkUtils.setEntry(chunk, tagKey, chunkEntry);
                    }
                    return true;
                }
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