package advRocketry.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

public class RenderUtils {

    public static void renderTopFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x0, float x1, float z0, float z1, float y, float u0, float u1, float v0, float v1, int light, int overlay, int color){
        vertexConsumer.addVertex(pose,new Vector3f(x0,y,z1)).setColor(color).setLight(light).setUv(u0,v1).setOverlay(overlay).setNormal(0,1,0);
        vertexConsumer.addVertex(pose,new Vector3f(x1,y,z1)).setColor(color).setLight(light).setUv(u1,v1).setOverlay(overlay).setNormal(0,1,0);
        vertexConsumer.addVertex(pose,new Vector3f(x1,y,z0)).setColor(color).setLight(light).setUv(u1,v0).setOverlay(overlay).setNormal(0,1,0);
        vertexConsumer.addVertex(pose,new Vector3f(x0,y,z0)).setColor(color).setLight(light).setUv(u0,v0).setOverlay(overlay).setNormal(0,1,0);
    }
    // rotates uv
    public static void renderTopFace2(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x0, float x1, float z0, float z1, float y, float u0, float u1, float v0, float v1, int light, int overlay, int color){
        vertexConsumer.addVertex(pose,new Vector3f(x0,y,z1)).setColor(color).setLight(light).setUv(u1,v0).setOverlay(overlay).setNormal(0,1,0);
        vertexConsumer.addVertex(pose,new Vector3f(x1,y,z1)).setColor(color).setLight(light).setUv(u1,v1).setOverlay(overlay).setNormal(0,1,0);
        vertexConsumer.addVertex(pose,new Vector3f(x1,y,z0)).setColor(color).setLight(light).setUv(u0,v1).setOverlay(overlay).setNormal(0,1,0);
        vertexConsumer.addVertex(pose,new Vector3f(x0,y,z0)).setColor(color).setLight(light).setUv(u0,v0).setOverlay(overlay).setNormal(0,1,0);
    }

    public static void renderBottomFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x0, float x1, float z0, float z1, float y, float u0, float u1, float v0, float v1, int light, int overlay, int color){
        vertexConsumer.addVertex(pose,new Vector3f(x0,y,z0)).setColor(color).setLight(light).setUv(u0,v0).setOverlay(overlay).setNormal(0,-1,0);
        vertexConsumer.addVertex(pose,new Vector3f(x1,y,z0)).setColor(color).setLight(light).setUv(u1,v0).setOverlay(overlay).setNormal(0,-1,0);
        vertexConsumer.addVertex(pose,new Vector3f(x1,y,z1)).setColor(color).setLight(light).setUv(u1,v1).setOverlay(overlay).setNormal(0,-1,0);
        vertexConsumer.addVertex(pose,new Vector3f(x0,y,z1)).setColor(color).setLight(light).setUv(u0,v1).setOverlay(overlay).setNormal(0,-1,0);
    }

    public static void renderEastFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float y0, float y1, float z0, float z1, float x, float u0, float u1, float v0, float v1, int light, int overlay, int color){
        vertexConsumer.addVertex(pose,new Vector3f(x,y0,z0)).setColor(color).setLight(light).setUv(u0,v0).setOverlay(overlay).setNormal(1,0,0);
        vertexConsumer.addVertex(pose,new Vector3f(x,y1,z0)).setColor(color).setLight(light).setUv(u0,v1).setOverlay(overlay).setNormal(1,0,0);
        vertexConsumer.addVertex(pose,new Vector3f(x,y1,z1)).setColor(color).setLight(light).setUv(u1,v1).setOverlay(overlay).setNormal(1,0,0);
        vertexConsumer.addVertex(pose,new Vector3f(x,y0,z1)).setColor(color).setLight(light).setUv(u1,v0).setOverlay(overlay).setNormal(1,0,0);
    }

    public static void renderWestFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float y0, float y1, float z0, float z1, float x, float u0, float u1, float v0, float v1, int light, int overlay, int color){
        vertexConsumer.addVertex(pose,new Vector3f(x,y0,z1)).setColor(color).setLight(light).setUv(u1,v0).setOverlay(overlay).setNormal(-1,0,0);
        vertexConsumer.addVertex(pose,new Vector3f(x,y1,z1)).setColor(color).setLight(light).setUv(u1,v1).setOverlay(overlay).setNormal(-1,0,0);
        vertexConsumer.addVertex(pose,new Vector3f(x,y1,z0)).setColor(color).setLight(light).setUv(u0,v1).setOverlay(overlay).setNormal(-1,0,0);
        vertexConsumer.addVertex(pose,new Vector3f(x,y0,z0)).setColor(color).setLight(light).setUv(u0,v0).setOverlay(overlay).setNormal(-1,0,0);
    }

    public static void renderNorthFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float y0, float y1, float x0, float x1, float z, float u0, float u1, float v0, float v1, int light, int overlay, int color){
        vertexConsumer.addVertex(pose,new Vector3f(x0,y0,z)).setColor(color).setLight(light).setUv(u0,v0).setOverlay(overlay).setNormal(0,0,-1);
        vertexConsumer.addVertex(pose,new Vector3f(x0,y1,z)).setColor(color).setLight(light).setUv(u0,v1).setOverlay(overlay).setNormal(0,0,-1);
        vertexConsumer.addVertex(pose,new Vector3f(x1,y1,z)).setColor(color).setLight(light).setUv(u1,v1).setOverlay(overlay).setNormal(0,0,-1);
        vertexConsumer.addVertex(pose,new Vector3f(x1,y0,z)).setColor(color).setLight(light).setUv(u1,v0).setOverlay(overlay).setNormal(0,0,-1);
    }

    public static void renderSouthFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float y0, float y1, float x0, float x1, float z, float u0, float u1, float v0, float v1, int light, int overlay, int color){
        vertexConsumer.addVertex(pose,new Vector3f(x1,y0,z)).setColor(color).setLight(light).setUv(u1,v0).setOverlay(overlay).setNormal(0,0,1);
        vertexConsumer.addVertex(pose,new Vector3f(x1,y1,z)).setColor(color).setLight(light).setUv(u1,v1).setOverlay(overlay).setNormal(0,0,1);
        vertexConsumer.addVertex(pose,new Vector3f(x0,y1,z)).setColor(color).setLight(light).setUv(u0,v1).setOverlay(overlay).setNormal(0,0,1);
        vertexConsumer.addVertex(pose,new Vector3f(x0,y0,z)).setColor(color).setLight(light).setUv(u0,v0).setOverlay(overlay).setNormal(0,0,1);
    }


    public static Vector3f gamma_reverse(Vector3f color){
        return new Vector3f(
                (float) Math.pow(color.x, 2.2),
                (float) Math.pow(color.y, 2.2),
                (float) Math.pow(color.z, 2.2)
        );
    }

}
