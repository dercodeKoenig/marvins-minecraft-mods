package advRocketry.Blocks;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.GasRegistry;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Registry.Blocks;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;

public class DryIceBlock extends Block {

    // will be set to true just before the block evaporates so that the evaporation caused
    // by too little co2 on surface does not change the composition again
    public static BooleanProperty PREVENT_COMPOSITION_CHANGE_ON_BREAK = BooleanProperty.create("ignore_composition_change_on_break");

    public static String dryIceDataTag = "dryIceDataTag";
    public static float epsilon = 0.001f; // the magnitude of change required to place more dry ice

    public DryIceBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(defaultBlockState().setValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK, false));
    }

    public static float getNoiseThreshold(float frozen_co2_level) {
        return (frozen_co2_level * 2 - 1); // -1 to +1 (or higher)
    }

    public static int getTargetThickness(float noiseThreshold, float noiseTemperature) {
        float difference = noiseThreshold - noiseTemperature;
        if (difference < 0)
            return 0;
        int targetThickness = 1 + (int) (difference / 1f);
        return targetThickness;
    }

    public static void placeDryIceIfPossible(PlanetDimension planet, int blockX, int blockZ) {
        BlockPos pos0 = new BlockPos(blockX, 0, blockZ);
        ServerLevel level = DimensionManager.getServerLevel(planet.getDimensionId());
        ChunkAccess chunk = level.getChunk(pos0);
        CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, dryIceDataTag);

        // we place dry ice based on how much gas is frozen on the surface and the noise temperature

        // we only place dry ice if the frozen value increased since last placement
        // this avoids re-placing blocks that the player mined away

        // we do not place a new dry ice block if there is already one at surface to avoid stacking
        // them up to infinity every time the value increases a bit

        // dry ice should remove itself on block tick when the frozen gas coverage no longer meets the noise temperature

        float frozen_co2_level = planet.getGasProperty(GasRegistry.co2).frozen_surface;
        float frozen_co2_level_at_last_placement = 0; // default
        String positionKey = String.valueOf(pos0.asLong());
        if (chunkEntry.contains(positionKey)) {
            frozen_co2_level_at_last_placement = chunkEntry.getFloat(positionKey);
        }

        // Check if the CO2 level increased significantly
        if (frozen_co2_level > frozen_co2_level_at_last_placement + epsilon) {
            float noiseTemperature = ChunkUtils.getNoiseTemperatureAt(planet, pos0); // -1 to 1
            float noiseThreshold = getNoiseThreshold(frozen_co2_level);

            // Check if it's cold enough (temperature is below the threshold)
            if (noiseTemperature < noiseThreshold) {

                // Calculate the target thickness
                // 1 block base + 1 block for every x units the threshold is above the temperature
                int targetThickness = getTargetThickness(noiseThreshold, noiseTemperature);

                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
                BlockPos topBlock = new BlockPos(blockX, y - 1, blockZ);

                // Scan downwards to count existing dry ice blocks within the upper (targetThickness + 10) blocks
                int existingDryIceCount = 0;
                int scanDepth = targetThickness + 10;

                for (int i = 0; i < scanDepth; i++) {
                    BlockPos scanPos = topBlock.below(i);
                    BlockState state = level.getBlockState(scanPos);

                    if (state.getBlock() instanceof DryIceBlock) {
                        existingDryIceCount++;
                    }
                }

                if (existingDryIceCount < targetThickness) {
                    if(level.getBlockState(topBlock).isFaceSturdy(level, topBlock, Direction.UP)) {
                        // Not enough blocks: place exactly ONE more block on top of the surface.
                        // We do NOT update the tag here, which ensures this logic runs again in the future to place more blocks if needed.
                        level.setBlock(topBlock.above(), Blocks.DRY_ICE.get().defaultBlockState(), 3);
                    }else{
                        // do not place ice blocks on things like flowers or water.
                        // mark the corrent co2 level as completed
                        chunkEntry.putFloat(positionKey, frozen_co2_level);
                        ChunkUtils.setEntry(chunk, dryIceDataTag, chunkEntry);
                    }
                } else {
                    // We have enough blocks: mark the position as completed by updating the tag
                    chunkEntry.putFloat(positionKey, frozen_co2_level);
                    ChunkUtils.setEntry(chunk, dryIceDataTag, chunkEntry);
                }
            } else {
                // The CO2 increased, but it's not cold enough in noise temperature to place dry ice.
                // Update the tag immediately to prevent endless unneeded checks.
                chunkEntry.putFloat(positionKey, frozen_co2_level);
                ChunkUtils.setEntry(chunk, dryIceDataTag, chunkEntry);
            }
        }
        // If the level decreased significantly, immediately update the tag to mark the new lower value as completed
        else if (frozen_co2_level < frozen_co2_level_at_last_placement - epsilon) {
            chunkEntry.putFloat(positionKey, frozen_co2_level);
            ChunkUtils.setEntry(chunk, dryIceDataTag, chunkEntry);
        }
    }


    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PREVENT_COMPOSITION_CHANGE_ON_BREAK);
        super.createBlockStateDefinition(builder);
    }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;
        if(level.random.nextInt() % 100 > 0) return;
        // we remove dry ice if the planet has not enough ice on surface to meet the threshold

        if (level.getBlockState(pos.above()).getBlock() instanceof DryIceBlock)
            return; // do not evaporate when a block is above, the upper one needs to go first

        BlockPos pos0 = new BlockPos(pos.getX(), 0, pos.getZ());
        Dimension dim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (dim instanceof PlanetDimension planet) {
            float frozen_co2_level = planet.getGasProperty(GasRegistry.co2).frozen_surface;
            float noiseTemperature = ChunkUtils.getNoiseTemperatureAt(planet, pos0); // -1 to 1
            float noiseThreshold = getNoiseThreshold(frozen_co2_level);

            int targetThickness = getTargetThickness(noiseThreshold, noiseTemperature);

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
                level.destroyBlock(pos, false);
            }
        } else {
            // not a planet, can not exist here
            level.setBlock(pos, state.setValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK, true), 0);
            level.destroyBlock(pos, false);
        }
    }


    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if(level.isClientSide)return;
        // increase surface value for the planet
        // mostly for testing, this usually should not happen too often...
        // reduce surface value for the gas
        Dimension dim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (dim instanceof PlanetDimension planet) {
            planet.getGasProperty(GasRegistry.co2).frozen_surface += GasRegistry.singleBlockWeight / planet.getGravitationalMultiplier();
        }
    }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);

        if (state.getBlock() != newState.getBlock() && !state.getValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK)) {
            // reduce surface value for the gas
            Dimension dim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
            if (dim instanceof PlanetDimension planet) {
                planet.getGasProperty(GasRegistry.co2).frozen_surface -= GasRegistry.singleBlockWeight / planet.getGravitationalMultiplier();
            }
        }
    }
}
