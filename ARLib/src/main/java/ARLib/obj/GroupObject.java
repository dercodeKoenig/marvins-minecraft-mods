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

    public GroupObject(String name) {
        this(name, VertexFormat.Mode.DEBUG_LINES);
    }

    public GroupObject(String name, VertexFormat.Mode drawingMode) {
        this.name = name;
        this.drawMode = drawingMode;
    }


    public void render(PoseStack stack, VertexConsumer v, int packedLight, int packedOverlay, int color) {
        if (!faces.isEmpty()) {
            for (Face face : faces) {
                face.addFaceForRender(stack, v, packedLight, packedOverlay, color);
            }
        }
    }



    public void scaleUV(float u0, float v0, float u1, float v1){
        for (Face i : faces)
            i.scaleUV(u0,v0,u1,v1);
    }
}