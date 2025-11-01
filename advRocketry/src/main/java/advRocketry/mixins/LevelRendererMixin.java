package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    // skip cloud rendering when it can not rain in this dimension
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    public void renderClouds(PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        ResourceLocation dimensionId = Minecraft.getInstance().level.dimension().location();
        Dimension dimension = DimensionManager.get(dimensionId);
        if(dimension!=null){
            if(!dimension.canRain())
                ci.cancel();
        }
    }
}