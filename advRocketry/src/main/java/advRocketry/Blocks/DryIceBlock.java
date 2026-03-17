package advRocketry.Blocks;

import advRocketry.API;
import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.Registry.Blocks;
import advRocketry.Registry.GasRegistry;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Objects;

public class DryIceBlock extends Block {

    // will be set to true just before the block evaporates so that the evaporation caused
    // by too little co2 on surface does not change the composition again
    public static BooleanProperty PREVENT_COMPOSITION_CHANGE_ON_BREAK = BooleanProperty.create("ignore_composition_change_on_break");
    public static BooleanProperty PREVENT_COMPOSITION_CHANGE_ON_PLACE = BooleanProperty.create("ignore_composition_change_on_place");

    public static String dryIceDataTag = "dryIceDataTag";
    public static float epsilon = 0.001f; // the magnitude of change required to place more dry ice

    public DryIceBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(defaultBlockState().setValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK, false));
        registerDefaultState(defaultBlockState().setValue(PREVENT_COMPOSITION_CHANGE_ON_PLACE, false));
    }

    public static int getTargetThickness(double frozen_co2_level, float noiseTemperature) {
        if (frozen_co2_level == 0)
            return 0;
        float noiseThreshold = (float) (frozen_co2_level - 0.2f);
        float difference = noiseThreshold - noiseTemperature;
        if (difference < 0)
            return 0;
        int targetThickness = 1 + (int) (difference / 0.3f);
        return targetThickness;
    }

    // Converts world coordinates into a 0-255 local chunk index
    public static int getLocalIndex(int blockX, int blockZ) {
        return (blockX & 15) | ((blockZ & 15) << 4);
    }

    // Helper to get or initialize the Long array for doubles
    private static long[] getOrInitDryIceArray(CompoundTag chunkEntry) {
        if (chunkEntry.contains("levels", CompoundTag.TAG_LONG_ARRAY)) {
            long[] levels = chunkEntry.getLongArray("levels");
            if (levels.length == 256) {
                return levels;
            }
        }
        // Initialize new array. Default is 0L, which converts exactly to 0.0d
        return new long[256];
    }

    // returns true if a block was placed, false if the xz position is considered fully worked
    public static boolean placeDryIceIfPossible(PlanetDimension planet, int blockX, int blockZ, int placementFlags) {
        BlockPos pos0 = new BlockPos(blockX, 0, blockZ);
        ServerLevel level = planet.level();
        ChunkAccess chunk = level.getChunk(pos0);
        CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, dryIceDataTag);

        long[] dryIceLevels = getOrInitDryIceArray(chunkEntry);
        int localIndex = getLocalIndex(blockX, blockZ);

        double frozen_co2_level = planet.getGasProperty(GasRegistry.co2).frozen_surface;
        // Convert the long bits back into our double for math
        double frozen_co2_level_at_last_placement = Double.longBitsToDouble(dryIceLevels[localIndex]);

        // Check if the CO2 level increased significantly
        if (frozen_co2_level > frozen_co2_level_at_last_placement + epsilon) {
            float noiseTemperature = ChunkUtils.getNoiseTemperatureAt(planet, pos0); // -1 to 1

            // Calculate the target thickness
            int targetThickness = getTargetThickness(frozen_co2_level, noiseTemperature);

            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
            BlockPos topBlock = new BlockPos(blockX, y - 1, blockZ);

            // Scan downwards to count existing dry ice blocks
            int existingDryIceCount = 0;
            int scanDepth = targetThickness + 10;

            for (int i = 0; i < scanDepth; i++) {
                BlockPos scanPos = topBlock.below(i);
                if (scanPos.getY() <= level.getMinBuildHeight())
                    return false;
                BlockState state = level.getBlockState(scanPos);

                if (state.getBlock() instanceof DryIceBlock) {
                    existingDryIceCount++;
                }
            }

            if (existingDryIceCount < targetThickness) {
                BlockState topBlockState = level.getBlockState(topBlock);
                if (topBlockState.isFaceSturdy(level, topBlock, Direction.UP)) {
                    level.setBlock(topBlock.above(), Blocks.DRY_ICE.get().defaultBlockState().setValue(PREVENT_COMPOSITION_CHANGE_ON_PLACE, true), placementFlags);
                    return true;
                } else if (topBlockState.canBeReplaced() && level.getBlockState(topBlock.below()).isFaceSturdy(level, topBlock.below(), Direction.UP)) {
                    level.setBlock(topBlock, Blocks.DRY_ICE.get().defaultBlockState().setValue(PREVENT_COMPOSITION_CHANGE_ON_PLACE, true), placementFlags);
                    return true;
                }
            }

            // We have enough blocks or placement was not possible
            // Convert the double to raw long bits to store in the array
            dryIceLevels[localIndex] = Double.doubleToRawLongBits(frozen_co2_level);
            chunkEntry.putLongArray("levels", dryIceLevels);
            ChunkUtils.setEntry(chunk, dryIceDataTag, chunkEntry);

        }
        // If the level decreased significantly, immediately update the tag to mark the new lower value as completed
        else if (frozen_co2_level < frozen_co2_level_at_last_placement - epsilon) {
            dryIceLevels[localIndex] = Double.doubleToRawLongBits(frozen_co2_level);
            chunkEntry.putLongArray("levels", dryIceLevels);
            ChunkUtils.setEntry(chunk, dryIceDataTag, chunkEntry);
        }
        return false;
    }


    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PREVENT_COMPOSITION_CHANGE_ON_BREAK);
        builder.add(PREVENT_COMPOSITION_CHANGE_ON_PLACE);
        super.createBlockStateDefinition(builder);
    }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;
        // we remove dry ice if the planet has not enough ice on surface to meet the threshold

        if (level.getBlockState(pos.above()).getBlock() instanceof DryIceBlock)
            return; // do not evaporate when a block is above, the upper one needs to go first

        BlockPos pos0 = new BlockPos(pos.getX(), 0, pos.getZ());
        Dimension dim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (dim instanceof PlanetDimension planet) {
            double frozen_co2_level = planet.getGasProperty(GasRegistry.co2).frozen_surface;
            float noiseTemperature = ChunkUtils.getNoiseTemperatureAt(planet, pos0); // -1 to 1

            int targetThickness = getTargetThickness(frozen_co2_level, noiseTemperature);

            // count how many blocks are below this block to see if it can evaporate
            // if there are less than targetThickness, do not evaporate
            // this block is included in totalBlocks
            int totalBlocks = 0;
            while (level.getBlockState(pos.relative(Direction.DOWN, totalBlocks)).getBlock() instanceof DryIceBlock) {
                totalBlocks++;
            }

            if (totalBlocks > targetThickness) {
                // no longer valid, planet has too little frozen co2 to contain a dry ice block in this poaition

                // set the block to not modify composition on break
                // then perform the break
                level.setBlock(pos, state.setValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK, true), 0);
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        } else {
            // not a planet, can not exist here
            level.setBlock(pos, state.setValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK, true), 0);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }


    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        // dry ice should be added / removed from composition on place / break
        // item entity should replace it back into composition on despawn
        // maybe adjust max stack size so you can not put an entire planet in a chest

        if (level.isClientSide) return;

        if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {
            if (!Objects.equals(oldState.getBlock(), state.getBlock()) && !state.getValue(PREVENT_COMPOSITION_CHANGE_ON_PLACE)) {
                API.addSurfaceIceInBlocks(level.dimension().location(), GasRegistry.co2, 1);
            }
        }
    }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);

        if (level.isClientSide) return;

        if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {
            if (!Objects.equals(newState.getBlock(), state.getBlock()) && !state.getValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK)) {
                API.addSurfaceIceInBlocks(level.dimension().location(), GasRegistry.co2, -1);
            }
        }
    }
}
