package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getDefaultGravity", at = @At("HEAD"), cancellable = true)
    public void getDefaultGravity(CallbackInfoReturnable<Double> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ResourceLocation dimensionId = entity.level().dimension().location();
        Dimension dimension = DimensionManager.get(dimensionId);
        if (dimension != null) { // registered in DimensionManager
            double massMultiplier = dimension.getEarthMassMultiplier();
           cir.setReturnValue((entity.getAttributeValue(Attributes.GRAVITY) * massMultiplier));
        }
    }
}