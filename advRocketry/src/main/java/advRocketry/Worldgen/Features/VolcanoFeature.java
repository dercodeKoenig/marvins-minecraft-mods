package advRocketry.Worldgen.Features;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

public class VolcanoFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_RADIUS = 20;
    /** Safe within the 3x3 chunk write window only because place() snaps to
     *  chunk centre (24 blocks free to the left of centre, 23 to the right). */
    private static final int MAX_RADIUS = 23;

    private static final int MIN_RIM_WIDTH = 10;
    private static final int MAX_RIM_WIDTH = 16;
    private static final int MIN_RIM_HEIGHT = 10;
    private static final int MAX_RIM_HEIGHT = 20;

    private static final int MIN_ABOVE_MIN_BUILD = 5;
    private static final int TARGET_DEPTH = 60;
    private static final int DEPTH_JITTER = 12;

    public static final BlockState FILL_FLUID = Blocks.LAVA.defaultBlockState();
    public static final BlockState HULL_MATERIAL = Blocks.BASALT.defaultBlockState();

    private static final Predicate<BlockState> PLACEABLE_ON_AIR = BlockState::isAir;
    /** Anything but an unfired deposit block — never bury one mid-generation. */
    private static final Predicate<BlockState> REPLACEABLE =
            state -> !state.is(advRocketry.Registry.Blocks.VOLCANIC_DEPOSIT.get());
    private static final Predicate<BlockState> EXCAVATE =
            state -> !state.isAir() && !state.is(FILL_FLUID.getBlock())
                    && !state.is(advRocketry.Registry.Blocks.VOLCANIC_DEPOSIT.get());
    private static final Predicate<BlockState> PLACE_HULL_EXCEPT_FLUID =
            state -> !state.is(FILL_FLUID.getBlock())
                    && !state.is(advRocketry.Registry.Blocks.VOLCANIC_DEPOSIT.get());

    public VolcanoFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rnd = ctx.random();
        BlockPos origin = ctx.origin();
        int x = Math.floorDiv(origin.getX(), 16) * 16 + 8;
        int z = Math.floorDiv(origin.getZ(), 16) * 16 + 8;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        int totalRadius = MIN_RADIUS + rnd.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        int rimWidth = MIN_RIM_WIDTH + rnd.nextInt(MAX_RIM_WIDTH - MIN_RIM_WIDTH + 1);
        int rimHeight = MIN_RIM_HEIGHT + rnd.nextInt(MAX_RIM_HEIGHT - MIN_RIM_HEIGHT + 1);
        int craterRadius = totalRadius - rimWidth; // always >= 2 given the constants above
        int craterR2 = craterRadius * craterRadius;
        int totalR2 = totalRadius * totalRadius;

        // Cache terrain heights up front (foliage-aware — see findGroundY) so
        // later writes in this method don't skew the rim/lava-level math.
        int cacheSize = totalRadius * 2 + 1;
        int[][] colGroundCache = new int[cacheSize][cacheSize];
        for (int dx = -totalRadius; dx <= totalRadius; dx++) {
            for (int dz = -totalRadius; dz <= totalRadius; dz++) {
                colGroundCache[dx + totalRadius][dz + totalRadius] = findGroundY(level, x + dx, z + dz);
            }
        }

        // Abort on terrain too steep for a flat lava level to look right:
        // compare the 5th/95th percentile of rim-band ground height.
        final int STEEPNESS_THRESHOLD = MAX_RIM_HEIGHT;
        ArrayList<Integer> groundHeights = new ArrayList<>();
        for (int dx = -totalRadius; dx <= totalRadius; dx++) {
            for (int dz = -totalRadius; dz <= totalRadius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 < craterR2 || d2 > totalR2) continue;
                int gy = colGroundCache[dx + totalRadius][dz + totalRadius];
                if (level.getBlockState(new BlockPos(x + dx, gy, z + dz)).is(FILL_FLUID.getBlock())) continue;
                groundHeights.add(gy + 1);
            }
        }
        if (groundHeights.size() >= 10) {
            Collections.sort(groundHeights);
            int lo = groundHeights.get((int) (groundHeights.size() * 0.05));
            int hi = groundHeights.get((int) (groundHeights.size() * 0.95));
            if (hi - lo > STEEPNESS_THRESHOLD) {
                return false;
            }
        }

        // Pre-calculate rim heights so we can accurately check where the lowest edges will be
        int[][] actualRimHeightCache = new int[cacheSize][cacheSize];
        ArrayList<Integer> innerRimTops = new ArrayList<>();

        for (int dx = -totalRadius; dx <= totalRadius; dx++) {
            for (int dz = -totalRadius; dz <= totalRadius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 < craterR2 || d2 > totalR2) continue;

                double distInRim = Math.sqrt(d2) - craterRadius;
                double rimProgress = distInRim / (double) rimWidth;
                int maxVarHeight = (int) Math.round(rimHeight * (1.0 - rimProgress));
                if (maxVarHeight < 0) maxVarHeight = 0;

                int actualRimHeight = 0;
                if (maxVarHeight > 0) {
                    int minHeight = (int) (maxVarHeight / 1.1);
                    actualRimHeight = minHeight + rnd.nextInt(maxVarHeight - minHeight + 1);
                }
                actualRimHeightCache[dx + totalRadius][dz + totalRadius] = actualRimHeight;

                // Track the absolute Y level of the rim tops directly bordering the crater
                if (distInRim <= 2.0 && actualRimHeight > 0) {
                    int colSurface = colGroundCache[dx + totalRadius][dz + totalRadius] + 1;
                    innerRimTops.add(colSurface + actualRimHeight - 1);
                }
            }
        }

        // ~50% chance for a volcano filled to the brim and overflowing
        boolean fillToTop = rnd.nextBoolean();
        int flatLavaLevel;

        if (fillToTop && !innerRimTops.isEmpty()) {
            // Lava sits directly at the 5th percentile of the inner rim.
            // The 5% of the rim that is lower than this level will be submerged,
            // acting as a natural spillway for the fluid tick to overflow.
            Collections.sort(innerRimTops);
            flatLavaLevel = innerRimTops.get((int) (innerRimTops.size() * 0.05));
            flatLavaLevel += rnd.nextInt(3);
        } else {
            // Flat lava level = average crater terrain height, excluding columns
            // already flooded by a neighboring volcano (would drag the average down).
            int totalColGround = 0;
            int colCount = 0;
            for (int dx = -craterRadius; dx <= craterRadius; dx++) {
                for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                    if ((double) (dx * dx + dz * dz) > craterR2) continue;
                    int groundY = colGroundCache[dx + totalRadius][dz + totalRadius];
                    if (level.getBlockState(new BlockPos(x + dx, groundY, z + dz)).is(FILL_FLUID.getBlock())) continue;
                    totalColGround += groundY;
                    colCount++;
                }
            }
            int defaultLevel = colGroundCache[totalRadius][totalRadius];
            flatLavaLevel = colCount > 0 ? totalColGround / colCount : defaultLevel;
            // Raised so the rim is only partly submerged on steep ground
            flatLavaLevel += rimHeight / 2 + rnd.nextInt(3);
        }

        int surfaceY = flatLavaLevel + 1;
        int groundY = flatLavaLevel;
        if (groundY <= minY) {
            return false;
        }

        int targetY = surfaceY - TARGET_DEPTH;
        if (targetY < minY + MIN_ABOVE_MIN_BUILD) {
            targetY = minY + MIN_ABOVE_MIN_BUILD;
        }
        targetY -= rnd.nextInt(DEPTH_JITTER) - DEPTH_JITTER / 2;
        targetY = Math.max(minY + MIN_ABOVE_MIN_BUILD, Math.min(groundY - 1, targetY));

        // ---- 1. Rim: basalt ring, tapering from rimHeight at the crater edge to 0 outward ----
        for (int dx = -totalRadius; dx <= totalRadius; dx++) {
            for (int dz = -totalRadius; dz <= totalRadius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 < craterR2 || d2 > totalR2) continue;

                int actualRimHeight = actualRimHeightCache[dx + totalRadius][dz + totalRadius];
                if (actualRimHeight == 0) continue;

                int colSurface = colGroundCache[dx + totalRadius][dz + totalRadius] + 1;
                BlockState fill = HULL_MATERIAL;

                // Merge with a neighboring volcano
                BlockPos groundSurface = new BlockPos(x + dx, colSurface - 1, z + dz);
                BlockState groundState = level.getBlockState(groundSurface);
                if (groundState.is(FILL_FLUID.getBlock())) {
                    continue;
                }

                for (int y = Math.max(targetY, minY); y < colSurface && y < maxY; y++) {
                    safeSetBlock(level, new BlockPos(x + dx, y, z + dz), fill, PLACE_HULL_EXCEPT_FLUID);
                }
                for (int y = colSurface; y < colSurface + actualRimHeight && y < maxY; y++) {
                    safeSetBlock(level, new BlockPos(x + dx, y, z + dz), fill, PLACEABLE_ON_AIR);
                }
            }
        }

        // ---- 2. Crater: fill with lava up to the flat level ----
        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                if ((double) (dx * dx + dz * dz) > craterR2) continue;
                for (int y = Math.max(targetY, minY); y <= flatLavaLevel && y < maxY; y++) {
                    safeSetBlock(level, new BlockPos(x + dx, y, z + dz), FILL_FLUID, REPLACEABLE);
                }
                // safeSetBlock doesn't notify neighbors, so schedule a fluid tick
                // on the surface explicitly to get the lake flowing once loaded.
                BlockPos topLava = new BlockPos(x + dx, flatLavaLevel, z + dz);
                if (level.getBlockState(topLava).is(FILL_FLUID.getBlock())) {
                    level.scheduleTick(topLava, level.getFluidState(topLava).getType(), 1);
                }
            }
        }

        // ---- 2.5. Clear anything left standing above the flat lava level ----
        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                if ((double) (dx * dx + dz * dz) > craterR2) continue;
                for (int y = flatLavaLevel + 1; y < maxY; y++) {
                    safeSetBlock(level, new BlockPos(x + dx, y, z + dz), Blocks.AIR.defaultBlockState(), EXCAVATE);
                }
            }
        }

        // ---- 3. Deposit block at the shaft bottom, ticks itself into a chamber ----
        BlockPos depositPos = new BlockPos(x, targetY, z);
        safeSetBlock(level, depositPos, advRocketry.Registry.Blocks.VOLCANIC_DEPOSIT.get().defaultBlockState(), REPLACEABLE);
        level.scheduleTick(depositPos, advRocketry.Registry.Blocks.VOLCANIC_DEPOSIT.get(), 1);

        return true;
    }

    /**
     * Ground Y for a column, walking down past leaves/logs so a neighboring
     * chunk's already-decorated tree canopy isn't read as terrain. Starts
     * from OCEAN_FLOOR_WG, the heightmap type reliably tracked pre-FULL status.
     */
    private static int findGroundY(WorldGenLevel level, int x, int z) {
        int minY = level.getMinBuildHeight();
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (y > minY) {
            BlockState state = level.getBlockState(cursor.set(x, y, z));
            if (!state.isAir() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) {
                return y;
            }
            y--;
        }
        return minY;
    }
}