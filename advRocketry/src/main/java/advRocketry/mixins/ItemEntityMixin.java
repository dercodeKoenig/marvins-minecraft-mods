package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "getDefaultGravity", at = @At("HEAD"), cancellable = true)
    public void getDefaultGravity(CallbackInfoReturnable<Double> cir) {
        ItemEntity entity = (ItemEntity) (Object) this;
        ResourceLocation dimensionId = entity.level().dimension().location();
        Dimension dimension = DimensionManager.get(dimensionId);
        if (dimension != null) { // registered in DimensionManager
            double massMultiplier = dimension.getEarthMassMultiplier();
            cir.setReturnValue((0.04 * massMultiplier));
        }
    }
}