package advRocketry.Render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.ShaderInstance;

public class shaderUtils {

    public static VertexFormat POSITION = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .build();

    public static VertexFormat POSITION_TEXTURE_NORMAL = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Normal", VertexFormatElement.NORMAL)
            .build();

    public static VertexFormat POSITION_NORMAL = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Normal", VertexFormatElement.NORMAL)
            .build();

    public static VertexFormat STAR_BACKGROUND = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Normal", VertexFormatElement.NORMAL) // using the normal space as center for the star cube for warp
            .add("Color", VertexFormatElement.COLOR)
            .build();


    public static ShaderInstance warpTravelShader;
    public static ShaderInstance getWarpTravelShader() {
        return warpTravelShader;
    }

    public static ShaderInstance localAtmosphereShader;
    public static ShaderInstance getLocalAtmosphereShader() {
        return localAtmosphereShader;
    }

    public static ShaderInstance planetShader;
    public static ShaderInstance getPlanetShader() {
        return planetShader;
    }

    public static ShaderInstance planetAtmShader;
    public static ShaderInstance getPlanetAtmShader() {
        return planetAtmShader;
    }

    public static ShaderInstance blitPostProcessingShader;
    public static ShaderInstance getBlitPostProcessingShader() {
        return blitPostProcessingShader;
    }

    public static ShaderInstance blitShader;
    public static ShaderInstance getBlitShader() {
        return blitShader;
    }

    public static ShaderInstance blitExtractBright;
    public static ShaderInstance getBlitExtractBrightShader() {
        return blitExtractBright;
    }
    public static ShaderInstance blitBlur;
    public static ShaderInstance getBlitBlurShader() {return blitBlur;}

    public static ShaderInstance starBackgroundShader;
    public static ShaderInstance getstarBackgroundShader() {
        return starBackgroundShader;
    }

    public static ShaderInstance ringSystemShader;
    public static ShaderInstance getRingSystemShader() {
        return ringSystemShader;
    }
}
