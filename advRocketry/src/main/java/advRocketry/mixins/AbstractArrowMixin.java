package advRocketry.mixins;

import advRocketry.Utils.CelestialUtils;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Inject(method = "getDefaultGravity", at = @At("RETURN"), cancellable = true)
    public void modifyDefaultGravity(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValue() * CelestialUtils.getGravityMultiplier(this));
    }
}