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
    public static VertexFormat POSITION_COLOR = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .build();




    public static ShaderInstance localAtmosphereShader;
    public static ShaderInstance getLocalAtmosphereShader() {
        return localAtmosphereShader;
    }

    public static ShaderInstance planetShader;
    public static ShaderInstance getPlanetShader() {
        return planetShader;
    }

    public static ShaderInstance planetAtmosphereShader;
    public static ShaderInstance getPlanetAtmosphereShader() {
        return planetAtmosphereShader;
    }

    public static ShaderInstance blitAddTonemapShader;
    public static ShaderInstance getBlitAddTonemapShader() {
        return blitAddTonemapShader;
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
}
