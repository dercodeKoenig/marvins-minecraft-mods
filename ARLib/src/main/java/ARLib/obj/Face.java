package ARLib.obj;

import ARLib.obj.TextureCoordinate;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.lang.Math;

public class Face {
    public Vertex[] vertices;
    public Vertex[] vertexNormals;
    public TextureCoordinate[] textureCoordinates;

    // Recalculate face normal
    public Vertex calculateFaceNormal() {
        Vec3 v1 = new Vec3(vertices[1].x - vertices[0].x, vertices[1].y - vertices[0].y, vertices[1].z - vertices[0].z);
        Vec3 v2 = new Vec3(vertices[2].x - vertices[0].x, vertices[2].y - vertices[0].y, vertices[2].z - vertices[0].z);

        double nx = v1.y * v2.z - v1.z * v2.y;
        double ny = v1.z * v2.x - v1.x * v2.z;
        double nz = v1.x * v2.y - v1.y * v2.x;

        Vec3 normalVector = new Vec3(nx, ny, nz).normalize();
        return new Vertex((float) normalVector.x, (float) normalVector.y, (float) normalVector.z);
    }

    public void addFaceForRender(PoseStack stack, VertexConsumer v, int packedLight, int packedOverlay, int color) {
        // We only render if we have normals to use
        if (vertexNormals == null || vertexNormals.length == 0) {
            // Or, you could calculate a flat normal here as a fallback
           Vertex normal =  calculateFaceNormal();
            for (int i = 0; i < vertices.length; ++i) {
                vertexNormals[i] = normal;
            }
        }

        for (int i = 0; i < vertices.length; ++i) {
            // Get the specific normal for this vertex
            Vertex normal = vertexNormals[i];

            if (textureCoordinates != null && textureCoordinates.length > 0) {
                v.addVertex(stack.last(), vertices[i].x, vertices[i].y, vertices[i].z)
                        // Use the per-vertex normal
                        .setNormal(normal.x, normal.y, normal.z)
                        .setColor(color)
                        .setLight(packedLight)
                        .setOverlay(packedOverlay)
                        .setUv(textureCoordinates[i].u, textureCoordinates[i].v);
            } else {
                v.addVertex(stack.last(), vertices[i].x, vertices[i].y, vertices[i].z)
                        // Use the per-vertex normal
                        .setNormal(normal.x, normal.y, normal.z)
                        .setColor(color)
                        .setLight(packedLight)
                        .setOverlay(packedOverlay);
            }
        }
    }

    public void addFaceForRender(PoseStack stack, VertexConsumer v) {
        // We only render if we have normals to use
        if (vertexNormals == null || vertexNormals.length == 0) {
            // Or, you could calculate a flat normal here as a fallback
            Vertex normal =  calculateFaceNormal();
            vertexNormals = new Vertex[vertices.length];
            for (int i = 0; i < vertices.length; ++i) {
                vertexNormals[i] = normal;
            }
        }

        for (int i = 0; i < vertices.length; ++i) {
            // Get the specific normal for this vertex
            Vertex normal = vertexNormals[i];

            if (textureCoordinates != null && textureCoordinates.length > 0) {
                v.addVertex(stack.last(), vertices[i].x, vertices[i].y, vertices[i].z)
                        // Use the per-vertex normal
                        .setNormal(normal.x, normal.y, normal.z)
                        .setUv(textureCoordinates[i].u, textureCoordinates[i].v);
            } else {
                v.addVertex(stack.last(), vertices[i].x, vertices[i].y, vertices[i].z)
                        .setNormal(normal.x, normal.y, normal.z);
            }
        }
    }
}
