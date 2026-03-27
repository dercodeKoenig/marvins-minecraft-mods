package advRocketry.Utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

public class RenderUtils {

    // Helper to transform the normal based on the current Pose
    private static Vector3f getTransformedNormal(PoseStack.Pose pose, float nx, float ny, float nz) {
        Vector3f normal = new Vector3f(nx, ny, nz);
        return normal.mul(pose.normal());
    }

    public static void renderTopFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x0, float x1, float z0, float z1, float y, float u0, float u1, float v0, float v1, int light, int overlay, int color) {
        Vector3f n = getTransformedNormal(pose, 0, 1, 0);
        vertexConsumer.addVertex(pose, x0, y, z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x1, y, z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x1, y, z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x0, y, z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
    }

    // rotates uv 90°
    public static void renderTopFace2(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x0, float x1, float z0, float z1, float y, float u0, float u1, float v0, float v1, int light, int overlay, int color) {
        Vector3f n = getTransformedNormal(pose, 0, 1, 0);
        vertexConsumer.addVertex(pose, x0, y, z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x1, y, z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x1, y, z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x0, y, z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
    }

    public static void renderBottomFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x0, float x1, float z0, float z1, float y, float u0, float u1, float v0, float v1, int light, int overlay, int color) {
        Vector3f n = getTransformedNormal(pose, 0, -1, 0);
        vertexConsumer.addVertex(pose, x0, y, z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x1, y, z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x1, y, z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x0, y, z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
    }

    public static void renderEastFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float y0, float y1, float z0, float z1, float x, float u0, float u1, float v0, float v1, int light, int overlay, int color) {
        Vector3f n = getTransformedNormal(pose, 1, 0, 0);
        vertexConsumer.addVertex(pose, x, y0, z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x, y1, z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x, y1, z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x, y0, z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
    }

    public static void renderWestFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float y0, float y1, float z0, float z1, float x, float u0, float u1, float v0, float v1, int light, int overlay, int color) {
        Vector3f n = getTransformedNormal(pose, -1, 0, 0);
        vertexConsumer.addVertex(pose, x, y0, z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x, y1, z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x, y1, z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x, y0, z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
    }

    public static void renderNorthFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float y0, float y1, float x0, float x1, float z, float u0, float u1, float v0, float v1, int light, int overlay, int color) {
        Vector3f n = getTransformedNormal(pose, 0, 0, -1);
        vertexConsumer.addVertex(pose, x0, y0, z).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x0, y1, z).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x1, y1, z).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x1, y0, z).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
    }

    public static void renderSouthFace(VertexConsumer vertexConsumer, PoseStack.Pose pose, float y0, float y1, float x0, float x1, float z, float u0, float u1, float v0, float v1, int light, int overlay, int color) {
        Vector3f n = getTransformedNormal(pose, 0, 0, 1);
        vertexConsumer.addVertex(pose, x1, y0, z).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x1, y1, z).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x0, y1, z).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
        vertexConsumer.addVertex(pose, x0, y0, z).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
    }

    public static int packColor(float r, float g, float b, float a) {
        int R = (int) (Math.max(0, Math.min(1, r)) * 255.0f + 0.5f);
        int G = (int) (Math.max(0, Math.min(1, g)) * 255.0f + 0.5f);
        int B = (int) (Math.max(0, Math.min(1, b)) * 255.0f + 0.5f);
        int A = (int) (Math.max(0, Math.min(1, a)) * 255.0f + 0.5f);

        return (A << 24) | (R << 16) | (G << 8) | B;
    }


    public static Vector3f gamma_reverse(Vector3f color) {
        return new Vector3f(
                (float) Math.pow(color.x, 2.2),
                (float) Math.pow(color.y, 2.2),
                (float) Math.pow(color.z, 2.2)
        );
    }

    public static Vector3f gamma_correcct(Vector3f color) {
        return new Vector3f(
                (float) Math.pow(color.x, 1 / 2.2),
                (float) Math.pow(color.y, 1 / 2.2),
                (float) Math.pow(color.z, 1 / 2.2)
        );
    }

    public static Vector3f reinhard(Vector3f color) {
        return new Vector3f(
                color.x / (1.0f + color.x),
                color.y / (1.0f + color.y),
                color.z / (1.0f + color.z)
        );
    }
}
