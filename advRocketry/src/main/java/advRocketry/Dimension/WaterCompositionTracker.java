package advRocketry.Dimension;


import advRocketry.API;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;

/// This class should help to track water on a planet
/// Water should be added / removed to / from the composition when water blocks are placed / removed in the world
/// But there are some exceptions, for example melting ice or freezing water to ice should be ignored
/// Actions that reflect composition change like adjusting sea level or boiling water should be ignored
public class WaterCompositionTracker {

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    // called from mixin
    public static void onSetBlock(Level level, BlockPos pos, BlockState newState) {
        if (level.isClientSide)
            return;

        BlockState oldState = level.getBlockState(pos);

        boolean wasH2O = isWaterSourceOrIce(oldState);
        boolean isH2O = isWaterSourceOrIce(newState);
        if (wasH2O == isH2O)
            return;

        if (isWaterSource(newState)) {
            if(!oldState.getBlock().equals(Blocks.WATER) && oldState.getFluidState().is(Fluids.WATER))
                // old state was no water but had a water fluid state (kelp for example)
                // this should not contribute to composition
                return;
            if (!shouldIgnoreCompositionChangeEvent()) {
                API.addLiquidInBuckets(level.dimension().location(), GasRegistry.water, 1);
            }
        }
        else if (isIce(newState)) {
            API.addSurfaceIceInBlocks(level.dimension().location(), GasRegistry.water, 1);
        }

        else if (!isH2O) {
            if (isIce(oldState)) {
                if(!shouldIgnoreCompositionChangeEvent()) {
                    API.addSurfaceIceInBlocks(level.dimension().location(), GasRegistry.water, -1);
                }
            }
            if (isWaterSource(oldState) && newState.isAir()) {
                if (!shouldIgnoreCompositionChangeEvent()) {
                    // only remove water from composition when it was replaced with air
                    // so it ignores kelp growing or placing blocks in water
                    API.addLiquidInBuckets(level.dimension().location(), GasRegistry.water, -1);
                }
            }
        }
    }

    private static boolean isWaterSource(BlockState state) {
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    private static boolean isIce(BlockState state) {
        return state.is(Blocks.ICE);
    }

    private static boolean isWaterSourceOrIce(BlockState state) {
        return isWaterSource(state) || isIce(state);
    }

    private static boolean shouldIgnoreCompositionChangeEvent() {

        // finds out where the setblock call came from and maybe we ignore it
        return WALKER.walk(frames -> frames.anyMatch(frame ->
                frame.getDeclaringClass().equals(FlowingFluid.class) || // water spreading and source creation is skipped
                        frame.getDeclaringClass().equals(SeaLevelAdjustment.class)|| // sea level adjustment is skipped
                        frame.getDeclaringClass().equals(IceBlock.class) ||  // melt will evaporate (removed) when above sea level, ignore
                        frame.getDeclaringClass().equals(PlanetEvents.class) // this has a method to boil water when too hot, a reflection of change - ignore
        ));
    }

}
