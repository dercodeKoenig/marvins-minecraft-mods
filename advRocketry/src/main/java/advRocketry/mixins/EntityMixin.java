package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "applyGravity", at = @At("HEAD"), cancellable = true)
    public void applyGravity(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        ResourceLocation dimensionId = entity.level().dimension().location();
        Dimension dimension = DimensionManager.get(dimensionId);
        double massMultiplier = 1;
        if (dimension != null) { // registered in DimensionManager
            massMultiplier = dimension.getEarthMassMultiplier();
        }

        double d0 = entity.getGravity() * massMultiplier;
        if (d0 != (double)0.0F) {
            entity.setDeltaMovement(entity.getDeltaMovement().add((double)0.0F, -d0, (double)0.0F));
        }
        ci.cancel();
    }
}