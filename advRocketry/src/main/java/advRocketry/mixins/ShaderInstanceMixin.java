package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Render.test1;
import advRocketry.utils.ClientUtils;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ShaderInstance.class)
public class ShaderInstanceMixin {

    @Inject(method = "setDefaultUniforms", at = @At("HEAD"), cancellable = true)
    public void setDefaultUniforms(CallbackInfo ci) {
        ShaderInstance shader = (ShaderInstance)(Object)this;
        Level l = ClientUtils.getPlayerLevel();
        if(l == null) return;
        Dimension dimension = DimensionManager.get(l.dimension().location());
        if (dimension == null) return;

        test1.setdefaultuniform(shader);
    }
}
