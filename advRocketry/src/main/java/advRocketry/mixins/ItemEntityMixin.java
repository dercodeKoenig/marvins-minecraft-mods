package advRocketry.mixins;

import advRocketry.utils.CelestialUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "getDefaultGravity", at = @At("RETURN"), cancellable = true)
    public void modifyDefaultGravity(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValue() * CelestialUtils.getGravityMultiplier(this));
    }
}