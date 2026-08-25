package advRocketry.mixins;

import advRocketry.Registry.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WaterFluid.class)
public abstract class WaterFluidMixin {

    @Inject(method = "canBeReplacedWith", at = @At("HEAD"), cancellable = true)
    public void canBeReplacedWith(FluidState fluidState, BlockGetter blockReader, BlockPos pos, Fluid fluid, Direction direction, CallbackInfoReturnable<Boolean> ci) {
        // water should not be replaced with the light fluids
        // this should ensure that when multiple sea fluids stack the upper does not replace the lower
        if (fluid.isSame(Fluids.METHANE.get()))
            ci.setReturnValue(false);
        if (fluid.isSame(Fluids.OXYGEN.get()))
            ci.setReturnValue(false);
        if (fluid.isSame(Fluids.HYDROGEN.get()))
            ci.setReturnValue(false);
        if (fluid.isSame(Fluids.NITROGEN.get()))
            ci.setReturnValue(false);
        if (fluid.isSame(Fluids.CO2.get()))
            ci.setReturnValue(false);
    }
}