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

    public static boolean placeDryIceIfPossible(PlanetDimension planet, int blockX, int blockZ) {
        // normal flags
        return placeDryIceIfPossible(planet, blockX, blockZ, 3);
    }

    // returns true if a block was placed, false if the xz position is considered fully worked
    public static boolean placeDryIceIfPossible(PlanetDimension planet, int blockX, int blockZ, int placementFlags) {
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

        double frozen_co2_level = planet.getGasProperty(GasRegistry.co2).frozen_surface;
        double frozen_co2_level_at_last_placement = 0; // default
        String positionKey = String.valueOf(pos0.asLong());
        if (chunkEntry.contains(positionKey)) {
            frozen_co2_level_at_last_placement = chunkEntry.getDouble(positionKey);
        }

        // Check if the CO2 level increased significantly
        if (frozen_co2_level > frozen_co2_level_at_last_placement + epsilon) {
            float noiseTemperature = ChunkUtils.getNoiseTemperatureAt(planet, pos0); // -1 to 1

            // Calculate the target thickness
            // 1 block base + 1 block for every x units the threshold is above the temperature
            int targetThickness = getTargetThickness(frozen_co2_level, noiseTemperature);

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
                BlockState topBlockState = level.getBlockState(topBlock);
                if (topBlockState.isFaceSturdy(level, topBlock, Direction.UP)) {
                    // Not enough blocks: place exactly ONE more block on top of the surface.
                    // We do NOT update the tag here, which ensures this logic runs again in the future to place more blocks if needed.
                    level.setBlock(topBlock.above(), Blocks.DRY_ICE.get().defaultBlockState(), placementFlags);
                    return true;
                } else if (topBlockState.canBeReplaced() && level.getBlockState(topBlock.below()).isFaceSturdy(level, topBlock.below(), Direction.UP)) {
                    // directly replace the top block like grass
                    // but still requires solid block below
                    level.setBlock(topBlock, Blocks.DRY_ICE.get().defaultBlockState(), placementFlags);
                    return true;
                } else {
                    // do not place ice blocks on things like flowers or water.
                    // mark the corrent co2 level as completed
                    chunkEntry.putDouble(positionKey, frozen_co2_level);
                    ChunkUtils.setEntry(chunk, dryIceDataTag, chunkEntry);
                }
            } else {
                // We have enough blocks: mark the position as completed by updating the tag
                chunkEntry.putDouble(positionKey, frozen_co2_level);
                ChunkUtils.setEntry(chunk, dryIceDataTag, chunkEntry);
            }
        }
        // If the level decreased significantly, immediately update the tag to mark the new lower value as completed
        else if (frozen_co2_level < frozen_co2_level_at_last_placement - epsilon) {
            chunkEntry.putDouble(positionKey, frozen_co2_level);
            ChunkUtils.setEntry(chunk, dryIceDataTag, chunkEntry);
        }
        return false;
    }


    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PREVENT_COMPOSITION_CHANGE_ON_BREAK);
        super.createBlockStateDefinition(builder);
    }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;
        if (level.random.nextInt(20) != 0) return;
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


    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        // TODO: increase or reduce level when rocket or railgun adds / removes to the planet
    }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
