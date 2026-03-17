package advRocketry.Dimension;


import advRocketry.API;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

import java.awt.*;

/// This class should help to track water on a planet
/// Water should be added / removed to / from the composition when water blocks are placed / removed in the world
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
            if (!shouldIgnoreEvent()) {
                API.addLiquidInBuckets(level.dimension().location(), GasRegistry.water, 1);
            }
        }
        if (isIce(newState)) {
            API.addSurfaceIceInBlocks(level.dimension().location(), GasRegistry.water, 1);
        }

        if (!isH2O) {
            if (isIce(oldState)) {
                API.addSurfaceIceInBlocks(level.dimension().location(), GasRegistry.water, -1);
            }
            if (isWaterSource(oldState)) {
                if (!shouldIgnoreEvent()) {
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

    private static boolean shouldIgnoreEvent() {

        // finds out where the setblock call came from and maybe we ignore it
        return WALKER.walk(frames -> frames.anyMatch(frame ->
                frame.getDeclaringClass().equals(FlowingFluid.class) || // water spreading and source creation is skipped
                        frame.getDeclaringClass().equals(SeaLevelAdjustment.class) // sea level adjustment is skipped
        ));
    }

}
