package advRocketry.Worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.function.Predicate;

/**
 * Carves a single, random-sized impact crater into the surface.
 *
 * <p>Key properties
 * <ul>
 *   <li>Radius: {@link #MIN_RADIUS}..{@link #MAX_RADIUS} blocks. The maximum is
 *       intentionally hard-capped at {@link #MAX_RADIUS} (16) because every
 *       feature placed in the FEATURES chunk step may only safely write blocks
 *       within a 3x3 chunk window (blockStateWriteRadius(1)) centred on the
 *       generating chunk. A crater reaches {@link #MAX_RADIUS} blocks in every
 *       direction from its centre, so 16 is the largest radius that still fits
 *       entirely inside that window regardless of where the centre lands within
 *       the generating chunk &mdash; any larger and a crater could poke into a
 *       chunk two away, which gets rejected (and logged) as a "setBlock in a
 *       far chunk". This mirrors the reasoning in {@link BigCrystalFeature}, which
 *       caps its own reach a little more conservatively.</li>
 *   <li>Shape: a smooth paraboloid bowl ({@code depth &asymp; radius * DEPTH_FACTOR})
 *       with a thin, raised rim built from the terrain's own surface block, so it
 *       reads as a real ejecta rim rather than a hole punched in the ground.</li>
 *   <li>The rim block is sampled from the ground at the crater's edge (and the
 *       centre as a fallback), so the crater automatically matches the moon /
 *       dark-moon surface (moon turf / dark moon turf).</li>
 *   <li>Density is gated inside the feature (~1 per {@link #DENSITY_DENOMINATOR}
 *       chunks), mirroring {@link BigCrystalFeature}, so the sparse
 *       {@code minecraft:count(1)} placed feature still yields a scenic spread
 *       instead of one crater per chunk.</li>
 * </ul>
 */
public class CraterFeature extends Feature<NoneFeatureConfiguration> {

    /** Smallest crater radius (blocks). */
    private static final int MIN_RADIUS = 4;
    /** Largest crater radius (blocks). Capped to stay within the FEATURES step's
     * 3x3 chunk write window (blockStateWriteRadius(1)). See class javadoc. */
    public static final int MAX_RADIUS = 16;

    /** Bowl depth as a fraction of the radius. Real lunar craters are ~0.2-0.3,
     * but a slightly deeper bowl keeps them visually clear as depressions. */
    private static final double DEPTH_FACTOR = 0.4;
    /** Shallowest crater floor (blocks below the surface) at the centre. */
    private static final int MIN_DEPTH = 2;
    /** Per-crater depth jitter applied on top of the radius-scaled depth. */
    private static final int DEPTH_JITTER = 1;
    /** ~1 crater per this many chunks (i.e. about one per 4x4 region at 16). */
    private static final int DENSITY_DENOMINATOR = 64;
    /** Half-width (in blocks) of the raised rim band at the crater's edge. */
    private static final int RIM_WIDTH = 2;

    /** Carve predicate: remove anything that isn't air (turf, stone, ores...). */
    private static final Predicate<BlockState> CARVEABLE = state -> !state.isAir();
    /** Rim predicate: only place onto air so we never overwrite existing ground. */
    private static final Predicate<BlockState> PLACEABLE_ON_AIR = BlockState::isAir;

    public CraterFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rnd = ctx.random();
        BlockPos origin = ctx.origin();
        int x = origin.getX();
        int z = origin.getZ();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        // Density gate: only a fraction of placement attempts actually spawn.
        if (rnd.nextInt(DENSITY_DENOMINATOR) != 0) {
            return false;
        }

        // Random size, never exceeding MAX_RADIUS (the chunk-safety cap).
        int radius = MIN_RADIUS + rnd.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        int depth = Math.max(MIN_DEPTH, (int) Math.round(radius * DEPTH_FACTOR));
        depth += rnd.nextInt(DEPTH_JITTER * 2 + 1) - DEPTH_JITTER;
        if (depth < MIN_DEPTH) {
            depth = MIN_DEPTH;
        }

        // Surface anchor: the air block just above the top solid ground at the centre.
        int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
        int groundY = surfaceY - 1;
        if (groundY <= minY) {
            return false; // nothing solid to carve into
        }

        // Rim block is sampled from the terrain so it matches the biome's surface
        // (moon turf for the lit moon, dark moon turf for the dark moon). If the
        // centre has no solid ground we bail out entirely.
        BlockState centreSurface = level.getBlockState(new BlockPos(x, groundY, z));
        if (centreSurface.isAir()) {
            return false;
        }

        // Bigger craters get a taller rim so they stay recognisable.
        int rimHeight = depth >= 5 ? 2 : 1;
        int r2 = radius * radius;
        // Rim band is left untouched while carving so the lip is built on intact
        // surface (and therefore matches the biome's surface block cleanly).
        int innerRimSq = (radius - RIM_WIDTH) * (radius - RIM_WIDTH);
        boolean placedAny = false;

        // ---- 1. Carve a paraboloid bowl (the rim band is spared for the lip) ----
        // At the centre (t2 = 0) the bowl floor sits `depth` below the surface;
        // it rises back to the surface near the rim, so the outer edge stays intact.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r2 || d2 >= innerRimSq) {
                    continue; // outside the circle, or inside the rim band
                }
                double t2 = (double) d2 / (double) r2; // 0 at centre, 1 at rim
                int floorY = groundY - (int) Math.round(depth * (1.0 - t2));

                // Terrain isn't perfectly flat, so look the surface up per-column.
                int colGround = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x + dx, z + dz) - 1;
                int top = Math.max(floorY + 1, minY);
                for (int y = top; y <= colGround && y < maxY; y++) {
                    safeSetBlock(level, new BlockPos(x + dx, y, z + dz), Blocks.AIR.defaultBlockState(), CARVEABLE);
                    placedAny = true;
                }
            }
        }

        // ---- 2. Raise a rim along the outer edge ----
        // A ~2-block-wide ring of surface blocks stacked on top of the existing
        // ground gives the crater a clear ejecta lip.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 < innerRimSq || d2 > r2) {
                    continue; // only the outer ring band
                }
                int colSurface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x + dx, z + dz); // air above ground
                BlockState localBlock = level.getBlockState(new BlockPos(x + dx, colSurface - 1, z + dz));
                BlockState fill = localBlock.isAir() ? centreSurface : localBlock;
                for (int y = colSurface; y < colSurface + rimHeight && y < maxY; y++) {
                    safeSetBlock(level, new BlockPos(x + dx, y, z + dz), fill, PLACEABLE_ON_AIR);
                    placedAny = true;
                }
            }
        }

        return placedAny;
    }
}
