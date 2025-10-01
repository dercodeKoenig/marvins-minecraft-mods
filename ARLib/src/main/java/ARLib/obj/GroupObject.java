package ARLib.obj;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.ArrayList;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class GroupObject {
    public String name;
    public ArrayList<Face> faces = new ArrayList<>();
    public VertexFormat.Mode drawMode;
    private Matrix4f transformationMatrix=new Matrix4f().identity(); // Start with identity; // Stores the current transformation


    public static VertexFormat POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .build();
    public static VertexFormat POSITION_COLOR_OVERLAY_LIGHT_NORMAL = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .build();


    public GroupObject(String name) {
        this(name, VertexFormat.Mode.DEBUG_LINES);
    }

    public GroupObject(String name, VertexFormat.Mode drawingMode) {
        this.name = name;
        this.drawMode = drawingMode;
    }


    public void render(PoseStack stack, MultiBufferSource bufferSource, VertexFormat vertexFormat, RenderType.CompositeState compositeState, int packedLight, int packedOverlay, int color) {

        RenderType r = RenderType.create("renderer_235646whatever",
                vertexFormat,
                drawMode,
                RenderType.SMALL_BUFFER_SIZE,
                false,
                true,
                compositeState
        );

        VertexConsumer v = bufferSource.getBuffer(r);
        if (faces.size() > 0) {
            for (Face face : faces) {
                face.addFaceForRender(stack, v, packedLight, packedOverlay, color);
            }
        }
    }
}