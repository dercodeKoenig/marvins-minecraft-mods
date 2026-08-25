package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.SpaceStationDimension;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeMixin {

    @Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("HEAD"),
            cancellable = true)
    public void shouldFreeze(LevelReader level, BlockPos water, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> ci) {
        BlockState blockstate = level.getBlockState(water);
        if (!blockstate.getFluidState().isSource()) {
            // default method appears to not check this
            // but it is important or the water composition tracker messes up
            ci.setReturnValue(false);
            return;
        }
    }
}