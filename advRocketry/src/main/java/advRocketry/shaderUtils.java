package advRocketry;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public class shaderUtils {

    public static VertexFormat POSITION = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .build();

    public static VertexFormat POSITION_COLOR_TEXTURE_NORMAL_LIGHT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
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
}
