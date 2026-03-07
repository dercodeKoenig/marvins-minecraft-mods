package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Utils.ClientUtils;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(DimensionSpecialEffects.class)
public class MixinWorldEffects {
    @Inject(method = "skyType", at = @At("HEAD"), cancellable = true)
    private void skyType(CallbackInfoReturnable<DimensionSpecialEffects.SkyType> cir) {
        Dimension dimension = ClientUtils.getPlayerDimension();
        if (dimension == null) return;
        if (dimension.hasCustomSky())
            cir.setReturnValue(DimensionSpecialEffects.SkyType.NONE);
    }
}
