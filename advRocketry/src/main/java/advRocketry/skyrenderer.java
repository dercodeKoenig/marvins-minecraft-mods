package advRocketry;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.*;
import org.lwjgl.opengl.GL30;

import java.lang.Math;

import static advRocketry.shaderUtils.POSITION;
import static advRocketry.shaderUtils.POSITION_TEXTURE_NORMAL;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class skyrenderer {

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        ResourceLocation dimension = Minecraft.getInstance().level.dimension().location();
            event.setNearPlaneDistance(event.getNearPlaneDistance() * (float) (1d / (DimensionManager.INSTANCE.dimensions.get(dimension).atmosphereDensity + 0.0001)));
            event.setFarPlaneDistance( event.getFarPlaneDistance() * (float) (1d / (DimensionManager.INSTANCE.dimensions.get(dimension).atmosphereDensity + 0.0001)));
            event.setCanceled(true);
    }

    VertexBuffer vertexBufferSkyBox;
    VertexBuffer vertexBufferPlanet;
    boolean finishedLoading = false;

    public skyrenderer() {
        RenderSystem.recordRenderCall(() -> {
            createSkyBoxBuffer();
            createPlanetBuffer();
            setupRenderTarget();
            finishedLoading = true;
        });
    }

    void createPlanetBuffer() {
        WavefrontObject planetModel;

        vertexBufferPlanet = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            planetModel = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/planet/planet.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
        BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_TEXTURE_NORMAL);
        for (Face i : planetModel.groupObjects.get("Icosphere").faces) {
            i.addFaceForRender(new PoseStack(), b);
        }
        MeshData meshPlanet = b.build();
        vertexBufferPlanet.bind();
        vertexBufferPlanet.upload(meshPlanet);
        byteBuffer.close();
    }

    void createSkyBoxBuffer() {
        vertexBufferSkyBox = new VertexBuffer(VertexBuffer.Usage.STATIC);

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
        BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, POSITION);

        // Top face
        b.addVertex(100, 100, -100);
        b.addVertex(100, 100, 100);
        b.addVertex(-100, 100, 100);
        b.addVertex(-100, 100, -100);

        // Bottom face
        b.addVertex(-100, -100, -100);
        b.addVertex(-100, -100, 100);
        b.addVertex(100, -100, 100);
        b.addVertex(100, -100, -100);

        // Front face
        b.addVertex(-100, 100, -100);
        b.addVertex(-100, -100, -100);
        b.addVertex(100, -100, -100);
        b.addVertex(100, 100, -100);

        // Back face
        b.addVertex(100, 100, 100);
        b.addVertex(100, -100, 100);
        b.addVertex(-100, -100, 100);
        b.addVertex(-100, 100, 100);

        // Right face
        b.addVertex(100, 100, -100);
        b.addVertex(100, -100, -100);
        b.addVertex(100, -100, 100);
        b.addVertex(100, 100, 100);

        // Left face (fixed duplicate)
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

    private TextureTarget planetRenderTarget;

    public void setupRenderTarget() {
        this.planetRenderTarget = new TextureTarget(1000, 1000, true, Minecraft.ON_OSX);
    }

    public void renderSkyBox(PoseStack poseStack, Matrix4f proj, Matrix4f view, double partialtick) {
        if (!finishedLoading) return;


        // Save current viewport dimensions
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        if(planetRenderTarget.width != windowWidth * 2 ||planetRenderTarget.height != windowHeight * 2 ){
            planetRenderTarget.resize(windowWidth*2, windowHeight*2, true);
            System.out.println("planet render framebuffer resized to " + planetRenderTarget.width+":"+planetRenderTarget.height);
        }

        ShaderInstance shader;

        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);

        // Render skybox first (to the main framebuffer)
        RenderSystem.setShader(shaderUtils::getAtmosphereShader);
        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.QUADS, view, proj, Minecraft.getInstance().getWindow());
        Uniform color = shader.getUniform("Color");
        color.set((float)myPlanet.skyColor.x, (float)myPlanet.skyColor.y, (float)myPlanet.skyColor.z, 1f);

        shader.apply();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.draw();
        shader.clear();
        VertexBuffer.unbind();


        double lat = 50;

        // Calculate observer's view matrix
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

        double latRad = Math.toRadians(lat);
        double myPlanetRotation = myPlanet.getSelfRotationDegrees(partialtick);
        double lonRad = Math.toRadians(-myPlanetRotation);

        Vec3 localUp = new Vec3(Math.cos(latRad) * Math.cos(lonRad), Math.sin(latRad), Math.cos(latRad) * Math.sin(lonRad));
        Vec3 localForward = new Vec3(-Math.sin(latRad) * Math.cos(lonRad), Math.cos(latRad), -Math.sin(latRad) * Math.sin(lonRad));

        Vector4f upWorld4 = tiltMatrix.transform(new Vector4f((float) localUp.x, (float) localUp.y, (float) localUp.z, 0.0f));
        Vector4f forwardWorld4 = tiltMatrix.transform(new Vector4f((float) localForward.x, (float) localForward.y, (float) localForward.z, 0.0f));

        Vec3 observerUpWorld = new Vec3(upWorld4.x, upWorld4.y, upWorld4.z).normalize();
        Vec3 observerForwardWorld = new Vec3(forwardWorld4.x, forwardWorld4.y, forwardWorld4.z).normalize();

        Matrix4f observerViewMatrix = new Matrix4f().lookAt(
                new Vector3f(0, 0, 0),
                observerForwardWorld.toVector3f(),
                observerUpWorld.toVector3f()
        );

        // Bind FBO for planet rendering
        this.planetRenderTarget.bindWrite(true);

        // Clear with transparent black
        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 0.0f);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        // Setup render states for planets
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();


        // Render planets to FBO
        for (DimensionProperties planet : DimensionManager.INSTANCE.dimensions.values()) {
            if (planet.dimensionId.equals(myPlanet.dimensionId)) continue;

            Matrix4f planetModelMatrix = new Matrix4f();

            Vec3 relativePos = planet.position.subtract(myPlanet.position).normalize().scale(200.0f);
            planetModelMatrix.translate((float) relativePos.x, (float) relativePos.y, (float) relativePos.z);

            Vec3 modelUp = new Vec3(0, 1, 0);
            Vec3 targetNorth = planet.rotationAxis.normalize();
            Vec3 rotAxis = modelUp.cross(targetNorth);
            if (rotAxis.length() > 1e-9) {
                double rotAngleRad = Math.asin(rotAxis.length());
                planetModelMatrix.rotate(new Quaternionf().fromAxisAngleRad(rotAxis.toVector3f(), (float) rotAngleRad));
            } else if (modelUp.dot(targetNorth) < 0) {
                planetModelMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vec3(1, 0, 0).toVector3f(), 180f));
            }
            planetModelMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 1, 0), (float) planet.getSelfRotationDegrees(partialtick)));

            double distance = myPlanet.position.distanceTo(planet.position);
            double scale = planet.size / distance * 10;
            planetModelMatrix.scale((float) scale);

            Matrix4f modelViewMatrix = new Matrix4f(view)
                    .mul(observerViewMatrix)
                    .mul(planetModelMatrix);

            RenderSystem.setShader(shaderUtils::getPlanetShader);
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(planet.texture).setFilter(true, true);
            RenderSystem.setShaderTexture(0, planet.texture);
            shader = RenderSystem.getShader();
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, modelViewMatrix, proj, Minecraft.getInstance().getWindow());

            DimensionProperties star = DimensionManager.INSTANCE.dimensions.get(planet.lightSourceDimensionId);
            if (star != null) {
                Vec3 lightWorld = star.position.subtract(planet.position);
                Matrix4f finalViewMatrix = new Matrix4f(view).mul(observerViewMatrix);
                Vector4f lightView4 = new Vector4f((float) lightWorld.x, (float) lightWorld.y, (float) lightWorld.z, 0.0f);
                finalViewMatrix.transform(lightView4);
                Vec3 lightView = new Vec3(lightView4.x(), lightView4.y(), lightView4.z()).normalize();
                if (shader.LIGHT0_DIRECTION != null) {
                    shader.LIGHT0_DIRECTION.set((float) lightView.x, (float) lightView.y, (float) lightView.z);
                }
            }

            Uniform AtmColor = shader.getUniform("AtmColor");
            AtmColor.set(myPlanet.skyColor.toVector3f());

            Uniform reflectivity = shader.getUniform("reflectivity");
            reflectivity.set(planet.reflectivity);

            Uniform emissiveColor = shader.getUniform("emissiveColor");
            emissiveColor.set(planet.emissiveColor.toVector3f());

            shader.apply();
            vertexBufferPlanet.bind();
            vertexBufferPlanet.draw();
            shader.clear();
        }


        VertexBuffer.unbind();

        // Clean up render states
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();

        // Switch back to main render target
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);

        // Restore the viewport to the window dimensions
        RenderSystem.viewport(0, 0, windowWidth, windowHeight);

        // Enable blending for compositing
        TRANSLUCENT_TRANSPARENCY.setupRenderState();

        // Blit the planet texture onto the screen
        this.planetRenderTarget.blitToScreen(windowWidth, windowHeight, false);

        TRANSLUCENT_TRANSPARENCY.clearRenderState();

        // Clear depth buffer for subsequent rendering
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);


    }
}