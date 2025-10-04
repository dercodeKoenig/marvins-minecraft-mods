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


    public static ShaderInstance atmosphereShader;
    public static ShaderInstance getAtmosphereShader() {
        return atmosphereShader;
    }

    public static ShaderInstance planetShader;
    public static ShaderInstance getPlanetShader() {
        return planetShader;
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
}
