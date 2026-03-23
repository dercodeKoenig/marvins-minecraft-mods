package advRocketry.Render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;

// builtin simple texture has no mipmap enabled
public class MipmapSimpleTexture extends SimpleTexture {
    private final int mipmapLevels;

    public MipmapSimpleTexture(ResourceLocation location, int mipmapLevels) {
        super(location);
        this.mipmapLevels = mipmapLevels;
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        // 1. Copy the exact logic from SimpleTexture's load method
        TextureImage textureImage = this.getTextureImage(resourceManager);
        textureImage.throwIfError();
        TextureMetadataSection metadata = textureImage.getTextureMetadata();

        boolean blur = metadata != null && metadata.isBlur();
        boolean clamp = metadata != null && metadata.isClamp();

        NativeImage nativeImage = textureImage.getImage();

        // 2. Redirect the render call to our OWN custom method instead of the private doLoad
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> this.doLoadWithMipmaps(nativeImage, blur, clamp));
        } else {
            this.doLoadWithMipmaps(nativeImage, blur, clamp);
        }
    }

    private void doLoadWithMipmaps(NativeImage image, boolean blur, boolean clamp) {
        // 3. Allocate space for the mipmaps on the GPU (pass in mipmapLevels instead of 0)
        TextureUtil.prepareImage(this.getId(), this.mipmapLevels, image.getWidth(), image.getHeight());

        // 4. Upload the base image (level 0).
        // Notice the second-to-last boolean is now 'true' (this tells NativeImage to set the correct Mipmap Min/Mag filters)
        image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), blur, clamp, true, true);

        // 5. Instruct the GPU to automatically generate the mipmap levels we allocated
        RenderSystem.bindTexture(this.getId());
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        setFilter(true, true);
    }
}