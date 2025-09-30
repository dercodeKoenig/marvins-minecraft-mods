package advRocketry.mixins;

import advRocketry.DimensionManager;
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
    private void disableSky(CallbackInfoReturnable<DimensionSpecialEffects.SkyType> cir) {
       ResourceLocation loc =  Minecraft.getInstance().level.dimension().location();
       if (DimensionManager.INSTANCE.dimensions.containsKey(loc)) {
           cir.setReturnValue(DimensionSpecialEffects.SkyType.NONE);
       }
    }
}
