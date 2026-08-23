package advRocketry.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import advRocketry.Worldgen.Features.VolcanoFeature;

/**
 * Placed at the bottom of a volcano shaft by {@link VolcanoFeature}. On its
 * first scheduled tick, seals a random sphere with a basalt hull and fills
 * the interior with lava, forming the magma chamber.
 */
public class VolcanicDepositBlock extends Block {

    public static final int MIN_RADIUS = 20;
    public static final int MAX_RADIUS = 35;
    private static final int MIN_HULL = 3;
    private static final int MAX_HULL = 5;

    private static final BlockState FILL_FLUID = VolcanoFeature.FILL_FLUID;
    private static final BlockState HULL_MATERIAL = VolcanoFeature.HULL_MATERIAL;

    public VolcanicDepositBlock() {
        super(Properties.of()
                .strength(2.0f, 6.0f)
                .sound(SoundType.BASALT)
                .requiresCorrectToolForDrops());
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // May already have been overwritten by a neighboring chamber's eruption.
        if (!level.getBlockState(pos).is(this)) return;
        int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        createLavaChamber(level, pos, radius, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, state.getBlock(), 10);
        }
    }

    /**
     * Seals a hull around a sphere of {@code radius} centred on {@code center},
     * then fills the interior with lava.
     */
    private void createLavaChamber(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        // Force-load every chunk the sphere can touch so setBlock never no-ops.
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                level.getChunkSource().getChunk(cx, cz, true);
            }
        }

        int hullThickness = MIN_HULL + random.nextInt(MAX_HULL - MIN_HULL + 1);
        int lavaRadius = radius - hullThickness; // always > 0: MIN_RADIUS > MAX_HULL, so the
        // hull zone below can never contain the centre

        int minYs = level.getMinBuildHeight();
        int maxYs = level.getMaxBuildHeight();
        int centerY = center.getY();
        int sphereBottom = Math.max(minYs, centerY - radius);
        int sphereTop = Math.min(maxYs - 1, centerY + radius);

        double lavaR2 = (double) (lavaRadius * lavaRadius);
        double radiusR2 = (double) (radius * radius);

        // Phase 1: hull shell (lavaR2 < distSq <= radiusR2). The two phases'
        // zones are disjoint, so which one runs first doesn't actually matter.
        for (int y = sphereBottom; y <= sphereTop; y++) {
            double dy = y - centerY;
            double rSq = radiusR2 - dy * dy;
            if (rSq < 0) continue;
            int r = (int) Math.floor(Math.sqrt(rSq));

            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > rSq) continue;
                    double distSq = (double) (dx * dx) + dy * dy + (double) (dz * dz);
                    if (distSq <= lavaR2 || distSq > radiusR2) continue;

                    BlockPos p = center.offset(dx, y - centerY, dz);
                    BlockState current = level.getBlockState(p);
                    if (current.is(FILL_FLUID.getBlock())) continue; // keep the shaft's own lava
                    if (current.is(this)) continue; // don't bury a neighboring unfired deposit
                    level.setBlock(p, HULL_MATERIAL, Block.UPDATE_CLIENTS);
                }
            }
        }

        // Phase 2: lava fill (distSq <= lavaR2). Overwrites rock and any shaft
        // hull it finds — that's how the chamber "injects" into the shaft wall.
        for (int y = sphereBottom; y <= sphereTop; y++) {
            double dy = y - centerY;
            double rSq = radiusR2 - dy * dy;
            if (rSq < 0) continue;
            int r = (int) Math.floor(Math.sqrt(rSq));

            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > rSq) continue;
                    double distSq = (double) (dx * dx) + dy * dy + (double) (dz * dz);
                    if (distSq > lavaR2) continue;

                    BlockPos p = center.offset(dx, y - centerY, dz);
                    BlockState current = level.getBlockState(p);
                    if (current.isAir() || current.is(FILL_FLUID.getBlock())) continue;
                    if (current.is(this)) continue; // preserve a neighboring unfired deposit
                    level.setBlock(p, FILL_FLUID, Block.UPDATE_ALL);
                }
            }
        }
    }
}