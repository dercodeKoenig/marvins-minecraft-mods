package advRocketry.Worldgen;

import advRocketry.Registry.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.function.Predicate;

/**
 * Generates a single giant, tilted, colored ice-crystal that pokes through the surface.
 *
 * <p>Key properties
 * <ul>
 *   <li>Length: {@link #MIN_LENGTH}..{@link #MAX_LENGTH} blocks (spec: 30-60).</li>
 *   <li>Random tilt {@code 0..MAX_TILT_DEG} (sometimes very high, e.g. 80deg) plus a random
 *       azimuth, so each crystal slants in a unique direction.</li>
 *   <li>~2/3 buried below the surface and ~1/3 above (the "above" fraction is randomized
 *       between {@link #ABOVE_MIN} and {@link #ABOVE_MAX}).</li>
 *   <li>One of the six crystal blocks is chosen at random for the whole structure.</li>
 *   <li>The crystal is anchored at the surface point and its cross-sections are faceted
 *       (diamond) tapering to a point at both the buried base and the exposed tip.</li>
 * </ul>
 *
 * <p>IMPORTANT: a placed feature may only write blocks within a few chunks of the chunk it is
 * placed into (vanilla rejects - and logs - "setBlock in a far chunk" for anything farther).
 * To respect that, the crystal is kept centred on its own column and its horizontal reach in
 * EITHER direction is capped at {@link #HORIZONTAL_LIMIT}. The total length is then shortened
 * for steep crystals so the whole footprint stays inside that cap. This is what lets it have a
 * high tilt without tripping the far-chunk safeguard.
 */
public class BigCrystalFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_LENGTH = 30;
    private static final int MAX_LENGTH = 60;
    private static final int MAX_RADIUS = 6;
    private static final double ABOVE_MIN = 0.30;
    private static final double ABOVE_MAX = 0.70;
    private static final double MAX_TILT_DEG = 80.0;
    // Density of crystals: ~1 per 32 chunks (i.e. about 2 per 8x8 region). Was 1/64 which
    // felt too sparse - a user report of "only find it in the ocean, not on land" made clear
    // the previous 1-per-64 meant most explored land had none at all.
    private static final int DENSITY_DENOMINATOR = 32;
    // Max horizontal distance (in blocks) the crystal may reach from its anchor column (x,z)
    // in either direction. The FEATURES chunk step has blockStateWriteRadius(1) so blocks that
    // land more than 1 chunk away from the generating chunk are rejected (and logged). The
    // feature's origin is at the generating chunk's corner, so a 14-block reach in any
    // direction keeps the whole footprint inside the allowed 3x3 chunk window.
    private static final double HORIZONTAL_LIMIT = 14.0;
    // Never emit a crystal shorter than this (it would render as a dot).
    private static final int MIN_FIT_LEN = 20;

    private static final Block[] CRYSTALS = {
            Blocks.CRYSTAL_RED.get(),
            Blocks.CRYSTAL_ORANGE.get(),
            Blocks.CRYSTAL_YELLOW.get(),
            Blocks.CRYSTAL_GREEN.get(),
            Blocks.CRYSTAL_BLUE.get(),
            Blocks.CRYSTAL_PURPLE.get()
    };

    public BigCrystalFeature() {
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

        // Density: ~1 crystal per DENSITY_DENOMINATOR chunks (about 1 in an 8x8 region).
        if (rnd.nextInt(DENSITY_DENOMINATOR) != 0) {
            return false;
        }

        // Pick one random colour for the whole crystal.
        BlockState crystalState = CRYSTALS[rnd.nextInt(CRYSTALS.length)].defaultBlockState();

        // Surface anchor = the top of the solid ground in this column. The heightmap already
        // gives this robustly for ANY surface (dirt, grass, snow, ice, stone, snow layers on
        // dirt, ...) -- no need to guess which block counts as "the surface". OCEAN_FLOOR's
        // predicate is "blocksMotion" (i.e. is-solid): true for every normal ground block and
        // false for fluids, so over an ocean it skips straight down through the water to the
        // sea floor instead of locking onto the water/ice line. WorldGenLevel.getHeight
        // returns the first empty block above that top solid block - exactly where the crystal
        // emerges. We use the live OCEAN_FLOOR map (not OCEAN_FLOOR_WG), which is the one
        // primed by the FEATURES step per ChunkStatusTasks.generateFeatures.
        int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);

        // Random tilt (0..MAX_TILT_DEG, sometimes very high) and a random azimuth.
        double tilt = rnd.nextDouble() * Math.toRadians(MAX_TILT_DEG);
        double azimuth = rnd.nextDouble() * Math.PI * 2.0;
        double sinT = Math.sin(tilt);
        double cosT = Math.cos(tilt);
        double dirX = sinT * Math.cos(azimuth);
        double dirY = cosT; // always pointing up-ish (cos >= 0)
        double dirZ = sinT * Math.sin(azimuth);

        // Fraction of the length that ends up above the surface (~1/3, randomized).
        double above = ABOVE_MIN + rnd.nextDouble() * (ABOVE_MAX - ABOVE_MIN);

        // Desired length (30-60). For a steeply-tilted crystal the buried (longer) side reaches
        // furthest horizontally, so shorten the crystal if needed to stay inside the cap.
        double desired = MIN_LENGTH + rnd.nextDouble() * (MAX_LENGTH - MIN_LENGTH);
        double worstSide = Math.max(above, 1.0 - above);
        double fitLen = HORIZONTAL_LIMIT / (worstSide * Math.max(sinT, 0.08));
        int totalLen = (int) Math.round(Math.max(MIN_FIT_LEN, Math.min(desired, fitLen)));

        // The crystal is centred AT the surface point (x, surfaceY, z) so it can use the full
        // horizontal radius in both directions. The upper end ("tip") sticks out above the
        // surface and the lower end ("base") is buried below, split ~1/3 above, ~2/3 below.
        double halfUp = totalLen * above;
        double halfDown = totalLen * (1.0 - above);
        double tipX = x + dirX * halfUp;
        double tipY = surfaceY + dirY * halfUp;
        double tipZ = z + dirZ * halfUp;
        double baseX = x - dirX * halfDown;
        double baseY = surfaceY - dirY * halfDown;
        double baseZ = z - dirZ * halfDown;

        Predicate<BlockState> replaceAnything = state -> true;

        for (int s = 0; s <= totalLen; s++) {
            double f = s / (double) totalLen; // 0 at the buried base, 1 at the exposed tip
            double px = baseX + (tipX - baseX) * f;
            double py = baseY + (tipY - baseY) * f;
            double pz = baseZ + (tipZ - baseZ) * f;
            int pxI = (int) Math.round(px);
            int pyI = (int) Math.round(py);
            int pzI = (int) Math.round(pz);
            if (pyI < minY || pyI >= maxY) {
                continue;
            }

            // Faceted cross-section: a diamond that tapers to a point at both ends.
            int radius = (int) Math.round(MAX_RADIUS * Math.sin(Math.PI * f));

            for (int ox = -radius; ox <= radius; ox++) {
                for (int oz = -radius; oz <= radius; oz++) {
                    if (Math.abs(ox) + Math.abs(oz) > radius) {
                        continue; // keep it a diamond, not a square
                    }
                    int nx = pxI + ox;
                    int nz = pzI + oz;
                    if (Math.abs(nx - x) > HORIZONTAL_LIMIT || Math.abs(nz - z) > HORIZONTAL_LIMIT) {
                        continue;
                    }
                    safeSetBlock(level, new BlockPos(nx, pyI, nz), crystalState, replaceAnything);
                }
            }
        }
        return true;
    }
}
