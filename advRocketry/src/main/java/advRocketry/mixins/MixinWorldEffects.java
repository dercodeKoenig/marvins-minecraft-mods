package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(DimensionSpecialEffects.class)
public class MixinWorldEffects {
    @Inject(method = "skyType", at = @At("HEAD"), cancellable = true)
    private void skyType(CallbackInfoReturnable<DimensionSpecialEffects.SkyType> cir) {
        ResourceLocation loc = Minecraft.getInstance().level.dimension().location();
        Dimension dimension = DimensionManager.get(loc);
        if (dimension == null) return;
        if (dimension.hasCustomSky())
            cir.setReturnValue(DimensionSpecialEffects.SkyType.NONE);
    }
}
