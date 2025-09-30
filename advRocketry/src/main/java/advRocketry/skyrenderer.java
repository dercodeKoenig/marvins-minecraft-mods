package advRocketry;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.*;
import org.lwjgl.opengl.GL30;

import java.lang.Math;
import java.util.Set;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class skyrenderer {


    public static final Set<ResourceLocation> CUSTOM_SKY_DIMENSIONS = Set.of(
            BuiltinDimensionTypes.OVERWORLD.location(),
            ResourceLocation.fromNamespaceAndPath("mymod", "skylands"),
            ResourceLocation.fromNamespaceAndPath("advrocketry", "space")
    );

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        ResourceLocation dimension = Minecraft.getInstance().level.dimension().location();

        if (CUSTOM_SKY_DIMENSIONS.contains(dimension)) {
            // Double the fog distance (makes it thinner/further away)
            //event.setNearPlaneDistance(event.getNearPlaneDistance() * 2.0f);
            //event.setFarPlaneDistance(event.getFarPlaneDistance() * 2.0f);

            // Or half it (makes it thicker/closer)
            // event.setNearPlaneDistance(event.getNearPlaneDistance() * 0.5f);
            // event.setFarPlaneDistance(event.getFarPlaneDistance() * 0.5f);

            // Or make it fully transparent (very far away)
            event.setNearPlaneDistance(Float.MAX_VALUE);
            event.setFarPlaneDistance(Float.MAX_VALUE);

            // Cancel to apply custom values
            event.setCanceled(true);
        }
    }


    VertexBuffer vertexBufferSkyBox;
    VertexBuffer vertexBufferPlanet;
    boolean finishedLoading = false;
    public skyrenderer() {
        RenderSystem.recordRenderCall(() -> {
            createSkyBoxBuffer();
            createPlanetBuffer();
            finishedLoading = true;
        });
    }


    void createPlanetBuffer(){
        WavefrontObject planetModel;

        vertexBufferPlanet = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            planetModel = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/planet/planet.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }

        // the obj renderer expects all this values
       VertexFormat POSITION_COLOR_TEXTURE_NORMAL_LIGHT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("Color", VertexFormatElement.COLOR).add("UV0", VertexFormatElement.UV0).add("UV1", VertexFormatElement.UV1).add("UV2", VertexFormatElement.UV2).add("Normal", VertexFormatElement.NORMAL).build();
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(8192*2);
        BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : planetModel.groupObjects.get("Sphere").faces) {
            i.addFaceForRender(new PoseStack(), b, 0xF000F0, 0, 0xffffffff);
        }
        MeshData meshPlanet = b.build();
        vertexBufferPlanet.bind();
        vertexBufferPlanet.upload(meshPlanet);
        byteBuffer.close();
    }

    void createSkyBoxBuffer(){
        vertexBufferSkyBox = new VertexBuffer(VertexBuffer.Usage.STATIC);

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
        VertexFormat vertexFormat = VertexFormat.builder()
                .add("Position", VertexFormatElement.POSITION)
                .build();

        BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, vertexFormat);

        b.addVertex(100, 100, -100);
        b.addVertex(100, 100, 100);
        b.addVertex(-100, 100, 100);
        b.addVertex(-100, 100, -100);


        b.addVertex(-100, -100, -100);
        b.addVertex(-100, -100, 100);
        b.addVertex(100, -100, 100);
        b.addVertex(100, -100, -100);


        b.addVertex(-100, 100, -100);
        b.addVertex(-100, -100, -100);
        b.addVertex(100, -100, -100);
        b.addVertex(100, 100, -100);


        b.addVertex(100, 100, 100);
        b.addVertex(100, -100, 100);
        b.addVertex(-100, -100, 100);
        b.addVertex(-100, 100, 100);


        b.addVertex(100, 100, -100);
        b.addVertex(100, -100, -100);
        b.addVertex(100, -100, 100);
        b.addVertex(100, 100, 100);


        b.addVertex(100, 100, -100);
        b.addVertex(100, -100, -100);
        b.addVertex(100, -100, 100);
        b.addVertex(100, 100, 100);


        b.addVertex(-100, 100, 100);
        b.addVertex(-100, -100, 100);
        b.addVertex(-100, -100, -100);
        b.addVertex(-100, 100, -100);

        MeshData mesh = b.build();

        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.upload(mesh);
        byteBuffer.close();
    }

    static skyrenderer INSTANCE = new skyrenderer();

    public void renderSkyBox(PoseStack poseStack, Matrix4f proj, Matrix4f view) {
        if (!finishedLoading)return;

        ShaderInstance shader;

        // render skybox
        RenderSystem.setShader(GameRenderer::getPositionShader);
        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.QUADS, view, proj, Minecraft.getInstance().getWindow());
        Uniform color = shader.getUniform("ColorModulator");
        color.set(0.1f,0.1f,0.2f,1f);
        shader.apply();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.draw();
        shader.clear();
        VertexBuffer.unbind();

        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();
        LIGHTMAP.setupRenderState();


        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();

        double lat = 30;

        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);


        // --- This part is calculated ONCE PER FRAME outside the planet-drawing loop ---
        // It represents the orientation of the entire sky based on the observer's home planet.

        // 1. Calculate myPlanet's AXIAL TILT Matrix.
        // This matrix ONLY aligns the planet's axis. The spin is handled separately.
        Matrix4f tiltMatrix = new Matrix4f();
        Vec3 mymodelUp = new Vec3(0, 1, 0);
        Vec3 mytargetNorth = myPlanet.rotationAxis.normalize();
        Vec3 myrotAxis = mymodelUp.cross(mytargetNorth);
        if (myrotAxis.length() > 1e-9) {
            double myrotAngleRad = Math.asin(myrotAxis.length());
            tiltMatrix.rotate(new Quaternionf().fromAxisAngleRad(myrotAxis.toVector3f(), (float) myrotAngleRad));
        } else if (mymodelUp.dot(mytargetNorth) < 0) {
            tiltMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vec3(1, 0, 0).toVector3f(), 180f));
        }

        // 2. Determine Observer's position on the planet using latitude and the CURRENT spin angle (longitude).
        double latRad = Math.toRadians(lat);
        // The spin angle directly controls the observer's longitude.
        double lonRad = Math.toRadians(-myPlanet.selfRotationDegrees + 90);

        // These vectors represent the observer's orientation in the planet's simple, tilted coordinate system.
        // As lonRad changes, these vectors "spin" around the planet's axis.
        Vec3 localUp = new Vec3(Math.cos(latRad) * Math.cos(lonRad), Math.sin(latRad), Math.cos(latRad) * Math.sin(lonRad));
        Vec3 localForward = new Vec3(-Math.sin(latRad) * Math.cos(lonRad), Math.cos(latRad), -Math.sin(latRad) * Math.sin(lonRad));

        // 3. Transform these spinning local vectors by the tiltMatrix to get their final world orientation.
        Vector4f upWorld4 = tiltMatrix.transform(new Vector4f((float)localUp.x, (float)localUp.y, (float)localUp.z, 0.0f));
        Vector4f forwardWorld4 = tiltMatrix.transform(new Vector4f((float)localForward.x, (float)localForward.y, (float)localForward.z, 0.0f));

        Vec3 observerUpWorld = new Vec3(upWorld4.x, upWorld4.y, upWorld4.z).normalize();
        Vec3 observerForwardWorld = new Vec3(forwardWorld4.x, forwardWorld4.y, forwardWorld4.z).normalize();

        // 4. Create the final view matrix for the observer.
        // As selfRotationDegrees changes, this matrix will now rotate the entire sky.
        Matrix4f observerViewMatrix = new Matrix4f().lookAt(
                new Vector3f(0, 0, 0),
                observerForwardWorld.toVector3f(),
                observerUpWorld.toVector3f()
        );
        // --- End of per-frame calculation ---

        for (DimensionProperties planet : DimensionManager.INSTANCE.dimensions.values()) {
            if (planet.dimensionId.equals(myPlanet.dimensionId)) continue;

            // 1. Create the other planet's MODEL matrix.
            // This places the planet in our celestial sphere, tilts it, spins it, and scales it.
            // IMPORTANT: It starts from an identity matrix, NOT from the game's view matrix.
            Matrix4f planetModelMatrix = new Matrix4f();

            // Position relative to the observer's planet. Scale to a large, fixed distance for the sky.
            Vec3 relativePos = planet.position.subtract(myPlanet.position).normalize().scale(200.0f);
            planetModelMatrix.translate((float)relativePos.x, (float)relativePos.y, (float)relativePos.z);

            // Create the planet's self-rotation (spin) and axial tilt.
            // This is your original, working logic for tilting/spinning the *other* planet.
            Vec3 modelUp = new Vec3(0, 1, 0);
            Vec3 targetNorth = planet.rotationAxis.normalize();
            Vec3 rotAxis = modelUp.cross(targetNorth);
            if (rotAxis.length() > 1e-9) {
                double rotAngleRad = Math.asin(rotAxis.length());
                planetModelMatrix.rotate(new Quaternionf().fromAxisAngleRad(rotAxis.toVector3f(),(float)rotAngleRad));
            } else if (modelUp.dot(targetNorth) < 0) {
                planetModelMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vec3(1,0,0).toVector3f(),180f));
            }
            planetModelMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 1, 0), (float) planet.selfRotationDegrees));

            // Calculate apparent size and scale the model.
            double distance = myPlanet.position.distanceTo(planet.position);
            double scale = planet.size / distance*10;
            planetModelMatrix.scale((float)scale);


            // 2. Combine all matrices for the final ModelView matrix.
            // The transformation order (read right-to-left) is:
            // A vertex is transformed by the planet's model matrix (put into the sky).
            // Then, the whole sky is rotated by the observer's view matrix.
            // Finally, the player's camera rotation is applied.
            Matrix4f modelViewMatrix = new Matrix4f(view)
                    .mul(observerViewMatrix)
                    .mul(planetModelMatrix);


            // 3. Set shader uniforms and draw.
            RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(planet.texture).setFilter(true, true);
            RenderSystem.setShaderTexture(0, planet.texture);

            shader = RenderSystem.getShader();
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, modelViewMatrix, proj, Minecraft.getInstance().getWindow());
            shader.apply();

            vertexBufferPlanet.bind();
            vertexBufferPlanet.draw();
        }



        shader.clear();
        VertexBuffer.unbind();

        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();
        LIGHTMAP.clearRenderState();

        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);

    }
}
