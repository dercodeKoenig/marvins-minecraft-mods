package advRocketry.Blocks;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.GasRegistry;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Registry.Blocks;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.core.BlockPos;
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
    public static float epsilon = 0.0001f;

    public DryIceBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(defaultBlockState().setValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK, false));
    }

    public static float getNoiseThreshold(float frozen_co2_level) {
        return (frozen_co2_level * 2 - 1); // -1 to +1 (or higher)
    }

    public static void placeDryIceIfPossible(PlanetDimension planet, int blockX, int blockZ) {
        BlockPos pos0 = new BlockPos(blockX, 0, blockZ);
        ServerLevel level = DimensionManager.getServerLevel(planet.getDimensionId());
        ChunkAccess chunk = level.getChunk(pos0);
        CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, dryIceDataTag);

        // we place dry ice based on how much gas is frozen on the surface and the noise temperature

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
        if (chunkEntry.contains(positionKey)) {
            frozen_co2_level_at_last_placement = chunkEntry.getFloat(positionKey);
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
                    level.setBlock(topBlock.above(), Blocks.DRY_ICE.get().defaultBlockState(), 3);
                }
            }

            // increase the level in the tag data so we do not process this position again if the block was broken
            // the dry ice block is responsible to decrease this value for its position on evaporation
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
        if (random.nextIntBetweenInclusive(0, 100) > 2) return;

        // we remove dry ice if the planet has not enough ice on surface to meet the threshold

        BlockPos pos0 = new BlockPos(pos.getX(), 0, pos.getZ());
        ChunkAccess chunk = level.getChunk(pos0);
        CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, dryIceDataTag);
        Dimension dim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (dim instanceof PlanetDimension planet) {
            float frozen_co2_level = planet.getGasProperty(GasRegistry.co2).frozen_surface;

            String positionKey = String.valueOf(pos0.asLong());


            float noiseTemperature = ChunkUtils.getNoiseTemperatureAt(planet, pos0); // -1 to 1
            float noiseThreshold = getNoiseThreshold(frozen_co2_level);
            if (noiseTemperature - epsilon > noiseThreshold) {
                // no longer valid, planet has too little frozen co2 to contain a dry ice block in this region
                // set the block to not modify composition on break
                // then perform the break
                level.setBlock(pos, state.setValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK, true), 0);
                level.destroyBlock(pos, false);

                // update the last processed co2 level so that should planet freeze again, new dry ice can be placed
                chunkEntry.putFloat(positionKey, frozen_co2_level);
                ChunkUtils.setEntry(chunk, dryIceDataTag, chunkEntry);
            }
        } else {
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
