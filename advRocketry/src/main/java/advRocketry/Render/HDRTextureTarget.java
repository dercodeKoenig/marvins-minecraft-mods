package advRocketry.Render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForgeConfig;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;
import java.util.Objects;

/**
 * a copy of the builtin TextureTarget, but with HDR support
 */
@OnlyIn(Dist.CLIENT)
public class HDRTextureTarget extends TextureTarget {
    public HDRTextureTarget(int width, int height, boolean useDepth, boolean clearError) {
        super(width, height, useDepth, clearError);
    }

    // almost identical to parent, but uses GL_RGBA16F for hdr support
    public void createBuffers(int width, int height, boolean clearError) {
        RenderSystem.assertOnRenderThreadOrInit();
        int i = RenderSystem.maxSupportedTextureSize();
        if (width > 0 && width <= i && height > 0 && height <= i) {
            this.viewWidth = width;
            this.viewHeight = height;
            this.width = width;
            this.height = height;
            this.frameBufferId = GlStateManager.glGenFramebuffers();
            this.colorTextureId = TextureUtil.generateTextureId();
            if (this.useDepth) {
                this.depthBufferId = TextureUtil.generateTextureId();
                GlStateManager._bindTexture(this.depthBufferId);
                GlStateManager._texParameter(3553, 10241, 9728);
                GlStateManager._texParameter(3553, 10240, 9728);
                GlStateManager._texParameter(3553, 34892, 0);
                GlStateManager._texParameter(3553, 10242, 33071);
                GlStateManager._texParameter(3553, 10243, 33071);
                if (!this.isStencilEnabled()) {
                    GlStateManager._texImage2D(3553, 0, 6402, this.width, this.height, 0, 6402, 5126, null);
                } else {
                    GlStateManager._texImage2D(3553, 0, 36013, this.width, this.height, 0, 34041, 36269, null);
                }
            }

            //this.setFilterMode(9728, true); // <- has private access
            // equivalent to setFilterMode(xxx, true)
            this.filterMode = GL11.GL_LINEAR;
            GlStateManager._bindTexture(this.colorTextureId);
            GlStateManager._texParameter(3553, 10241, filterMode);
            GlStateManager._texParameter(3553, 10240, filterMode);
            GlStateManager._bindTexture(0);
            // setFilterMode replacement end


            GlStateManager._bindTexture(this.colorTextureId);
            GlStateManager._texParameter(3553, 10242, 33071);
            GlStateManager._texParameter(3553, 10243, 33071);

            // change to GL_RGBA16F and GL11.GL_FLOAT
            GlStateManager._texImage2D(3553, 0, GL30.GL_RGBA16F, this.width, this.height, 0, 6408, GL11.GL_FLOAT, null);
            // change end

            GlStateManager._glBindFramebuffer(36160, this.frameBufferId);
            GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, this.colorTextureId, 0);
            if (this.useDepth) {
                if (!this.isStencilEnabled()) {
                    GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.depthBufferId, 0);
                } else if (NeoForgeConfig.CLIENT.useCombinedDepthStencilAttachment.get()) {
                    GlStateManager._glFramebufferTexture2D(36160, 33306, 3553, this.depthBufferId, 0);
                } else {
                    GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.depthBufferId, 0);
                    GlStateManager._glFramebufferTexture2D(36160, 36128, 3553, this.depthBufferId, 0);
                }
            }

            this.checkStatus();
            this.clear(clearError);
            this.unbindRead();
        } else {
            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
        }
    }
}
