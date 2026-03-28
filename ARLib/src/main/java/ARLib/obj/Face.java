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
    public TextureCoordinate[] original_textureCoordinates;

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
        // Ensure vertexNormals array exists
        if (this.vertexNormals == null || this.vertexNormals.length == 0) {
            Vertex normal = this.calculateFaceNormal();
            this.vertexNormals = new Vertex[this.vertices.length];
            for (int i = 0; i < this.vertices.length; ++i) {
                this.vertexNormals[i] = normal;
            }
        }

        // Get the current pose & normal matrix
        PoseStack.Pose pose = stack.last();
        Matrix3f normalMatrix = pose.normal(); // transforms normals
        // (positions are handled by v.addVertex(stack.last(), ...))

        for (int i = 0; i < this.vertices.length; ++i) {
            // Convert your normal to a Vector3f (adjust depending on your Vertex type)
            Vertex srcNormal = this.vertexNormals[i];
            Vector3f n = new Vector3f(srcNormal.x, srcNormal.y, srcNormal.z);

            // Transform & normalize
            normalMatrix.transform(n);
            n.normalize();

            if (this.textureCoordinates != null && this.textureCoordinates.length > 0) {
                v.addVertex(pose, this.vertices[i].x, this.vertices[i].y, this.vertices[i].z)
                        .setNormal(n.x(), n.y(), n.z())
                        .setColor(color)
                        .setLight(packedLight)
                        .setOverlay(packedOverlay)
                        .setUv(this.textureCoordinates[i].u, this.textureCoordinates[i].v);
            } else {
                v.addVertex(pose, this.vertices[i].x, this.vertices[i].y, this.vertices[i].z)
                        .setNormal(n.x(), n.y(), n.z())
                        .setColor(color)
                        .setLight(packedLight)
                        .setOverlay(packedOverlay);
            }
        }
    }


    public void addFaceForRender(PoseStack stack, VertexConsumer v) {
        addFaceForRender(stack,v,0,0,0);
    }

    public void scaleUV(float u0, float v0, float u1, float v1){
        if(original_textureCoordinates != null) {
            for (int i = 0; i < original_textureCoordinates.length; i++) {
                textureCoordinates[i].u = u0 + original_textureCoordinates[i].u * (u1 - u0);
                textureCoordinates[i].v = v0 + original_textureCoordinates[i].v * (v1 - v0);
            }
        }
    }
}
