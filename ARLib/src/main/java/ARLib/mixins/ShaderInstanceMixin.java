package ARLib.mixins;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

///  this mixin adds support for NormalMat in some shaders

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {

    @Shadow
    @Nullable
    public abstract Uniform getUniform(String name);

    // 1. Create a unique field to cache the reference
    @Unique
    @Nullable
    private Uniform NormalMat;

    // 2. Inject into the constructor to find the uniform ONCE when the shader loads
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void cacheNormalUniform(ResourceProvider provider, ResourceLocation location, VertexFormat format, CallbackInfo ci) {
        this.NormalMat = this.getUniform("NormalMat");
        System.out.println("NormalMat Shader Mixin for " + location + ":" + (NormalMat != null));
    }

    // 3. Inject into setDefaultUniforms to use the cached field
    @Inject(method = "setDefaultUniforms", at = @At("RETURN"))
    private void setNormalUniform(VertexFormat.Mode mode, Matrix4f frustumMatrix, Matrix4f projectionMatrix, Window window, CallbackInfo ci) {
        if (this.NormalMat != null) {
            // JOML's new Matrix3f() defaults to Identity
            this.NormalMat.set(new Matrix3f());
        }
    }
}