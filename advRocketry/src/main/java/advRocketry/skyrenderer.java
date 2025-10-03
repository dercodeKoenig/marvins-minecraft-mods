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

import static advRocketry.CelestialUtils.*;
import static advRocketry.shaderUtils.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class skyrenderer {

    // can modify fog distance
    public static void renderFog(ViewportEvent.RenderFog event) {
        ResourceLocation dimension = Minecraft.getInstance().level.dimension().location();
        event.setNearPlaneDistance(event.getNearPlaneDistance() * (float) (1d / (DimensionManager.INSTANCE.dimensions.get(dimension).atmosphereDensity + 0.0001)));
        event.setFarPlaneDistance(event.getFarPlaneDistance() * (float) (1d / (DimensionManager.INSTANCE.dimensions.get(dimension).atmosphereDensity + 0.0001)));
        event.setCanceled(true);
    }

    // can modify fog color
    public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
        Vector3f color = DimensionManager.INSTANCE.dimensions.get(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld")).getFogColor();
        event.setRed(color.x);
        event.setGreen(color.y);
        event.setBlue(color.z);
    }

    VertexBuffer vertexBufferSkyBox;
    VertexBuffer vertexBufferPlanet;
    boolean finishedLoading = false;

    public skyrenderer() {
        RenderSystem.recordRenderCall(() -> {
            createSkyBoxBuffer();
            createPlanetBuffer();
            setupRenderTargets();
            finishedLoading = true;
        });
    }

    void createPlanetBuffer() {
        WavefrontObject planetModel;

        vertexBufferPlanet = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            planetModel = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/environment/planet.obj"));
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
        WavefrontObject SkyBoxSphere;

        vertexBufferSkyBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            SkyBoxSphere = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/environment/skybox_sphere.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
        BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_NORMAL);
        for (Face i : SkyBoxSphere.groupObjects.get("Icosphere").faces) {
            i.addFaceForRender(new PoseStack(), b);
        }
        MeshData mesh = b.build();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.upload(mesh);
        byteBuffer.close();
    }


    static skyrenderer INSTANCE = new skyrenderer();

    private TextureTarget planetRenderTarget;
    private TextureTarget atmosphereRenderTarget;

    public void setupRenderTargets() {
        this.planetRenderTarget = new HDRTextureTarget(1000, 1000, true, false);
        this.atmosphereRenderTarget = new HDRTextureTarget(1000, 1000, false, false);
    }

    public void renderSkyBox(Matrix4f proj, Matrix4f view, Matrix4f skyView, float partialTick) {
        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);


        Matrix4f atmMatrix = new Matrix4f(view);
        atmMatrix.scale(Minecraft.getInstance().gameRenderer.getRenderDistance()); // this prevents bobbing by zooming out

        // TODO when i increase y it should slowly go out of atmosphere, task for shader..., also render the planet below

        RenderSystem.setShader(shaderUtils::getAtmosphereShader);
        ShaderInstance shader = RenderSystem.getShader();
        shader.setSampler("planetTexture", planetRenderTarget.getColorTextureId());

        shader.getUniform("ModelViewMat").set(atmMatrix);
        shader.getUniform("ProjMat").set(proj);
        shader.getUniform("skyViewMat").set(skyView); // so it can transform universe global coordinates into view space

        shader.getUniform("screenWidth").set(atmosphereRenderTarget.width);
        shader.getUniform("screenHeight").set(atmosphereRenderTarget.height);
        shader.getUniform("playerHeight").set((float) Minecraft.getInstance().player.position().y - Minecraft.getInstance().level.getSeaLevel());
        shader.getUniform("renderDistance").set(Minecraft.getInstance().gameRenderer.getRenderDistance());


        DimensionProperties starProps = DimensionManager.get(myPlanet.lightSourceDimensionId);
        Vec3 starPos0 = starProps.getPosition(partialTick);
        Vec3 starDir0 = starPos0.subtract(myPlanet.getPosition(partialTick)).normalize();
        shader.getUniform("StarDirection").set((float) starDir0.x, (float) starDir0.y, (float) starDir0.z);
        shader.getUniform("StarColor").set(starProps.emissiveColor.x, starProps.emissiveColor.y, starProps.emissiveColor.z);

        shader.getUniform("SkyColor").set(myPlanet.skyColor.x, myPlanet.skyColor.y, myPlanet.skyColor.z);
        shader.getUniform("SunriseColor").set(myPlanet.sunRiseColor.x, myPlanet.sunRiseColor.y, myPlanet.sunRiseColor.z);

        float[] fogColor = RenderSystem.getShaderFogColor(); // this is probably using custom overwrite anyway but this is where it is supposed to get the fog color from
        shader.getUniform("FogColor").set(fogColor[0], fogColor[1], fogColor[2]);


        // using the real planet radius looks bad because the terrain is not rendered.
        //TODO maybe render the planet sphere below??
        //shader.getUniform("renderDistance").set((float) CelestialUtils.getRealRadiusFromValue(myPlanet.size));

        shader.apply();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.draw();
        shader.clear();
        VertexBuffer.unbind();
    }

    public void renderSpaceBodies(PoseStack poseStack, Matrix4f proj, Matrix4f skyViewMatrix, double partialtick) {


        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);
        Vec3 myPlanetPosition = myPlanet.getPosition((float) partialtick);

        // use custom near / far for rendering
        float fovy = 2f * (float) Math.atan(1.0f / proj.get(1, 1));
        float aspect = proj.get(1, 1) / proj.get(0, 0);
        Matrix4f newProj = new Matrix4f().perspective(fovy, aspect, 0.0001f, 100_00);

        // Setup render states for planets
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();


        // Render planets to FBO
        for (DimensionProperties planet : DimensionManager.INSTANCE.dimensions.values()) {
            if (planet.dimensionId.equals(myPlanet.dimensionId)) continue;

            Matrix4f planetModelMatrix = new Matrix4f();

            Vec3 planetPosition = planet.getPosition((float) partialtick);

            Vec3 relativePos = planetPosition.subtract(myPlanetPosition); // in Astronomical units
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

            double planetRotationAngle = planet.getRotationAngle((float) partialtick);
            planetModelMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 1, 0), (float) planetRotationAngle));

            // to scale correctly we need to convert the radius (in earth radius multiplier) to astronomical units
            double trueRadius = CelestialUtils.fromEarthRadius(planet.earthRadiusMultiplier);
            double scaleAU = CelestialUtils.toAU(trueRadius);
            planetModelMatrix.scale((float) scaleAU);
            planetModelMatrix.scale(10f); // true size is too small, so apply a fixed scale

            RenderSystem.setShader(shaderUtils::getPlanetShader);
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(planet.texture).setFilter(true, true);
            RenderSystem.setShaderTexture(0, planet.texture);
            ShaderInstance shader = RenderSystem.getShader();

            shader.getUniform("ModelViewMat").set(planetModelMatrix);
            shader.getUniform("ProjMat").set(newProj);
            shader.getUniform("skyViewMat").set(skyViewMatrix); // so it can transform universe global coordinates into view space

            shader.getUniform("reflectivity").set(planet.reflectivity);
            shader.getUniform("emissiveColor").set(planet.emissiveColor);

            int totalLights = 0;
            for (ResourceLocation lightSourceId : planet.cachedLightSources.keySet()){
                DimensionProperties star = DimensionManager.INSTANCE.dimensions.get(lightSourceId);
                Vec3 StarPos = star.getPosition((float) partialtick);
                Vec3 LightVector = planetPosition.subtract(StarPos).scale(-1); //shader uses planet to star for dot product
                shader.getUniform("LightVectors["+String.valueOf(totalLights)+"]").set((float) LightVector.x, (float) LightVector.y, (float) LightVector.z);
                shader.getUniform("LightColors["+String.valueOf(totalLights)+"]").set(star.emissiveColor.x, star.emissiveColor.y, star.emissiveColor.z, star.emissiveColor.w);
                totalLights+=1;
            }
            shader.getUniform("LightCount").set(totalLights);


            shader.apply();
            vertexBufferPlanet.bind();
            vertexBufferPlanet.draw();
            shader.clear();
        }

        // Clean up render states
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();

        VertexBuffer.unbind();
    }

    public void renderSky(PoseStack poseStack, Matrix4f proj, Matrix4f view, float partialtick) {
        if (!finishedLoading) return;


        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);

        CelestialUtils.AxisDirections myGlobalAxis = CelestialUtils.getGlobalAxisDirections(myPlanet, partialtick);

        // Create the base orientation for our skybox using the planet's axes.
        // This matrix transforms global space coordinates into our local tilted planet's reference frame.
        Matrix4f planetOrientationMatrix = new Matrix4f().lookAt(
                new Vector3f(0, 0, 0),
                myGlobalAxis.north.toVector3f(),
                myGlobalAxis.up.toVector3f()    // Up direction
        );
        // normal view matrix transforms world global coordinates into view space
        // this matrix can transform universe global coordinates into view space
        Matrix4f skyViewMatrix = new Matrix4f(view).mul(planetOrientationMatrix);

        // adjust frame buffer size for render
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        if (planetRenderTarget.width != windowWidth * 2 || planetRenderTarget.height != windowHeight * 2) {
            if (windowWidth * windowHeight > 20000) { // small screen / minimized could cause crashes otherwise
                planetRenderTarget.resize(windowWidth * 2, windowHeight * 2, false);
            }
        }
        // Bind FBO for planet rendering
        this.planetRenderTarget.bindWrite(true);
        // Clear with transparent black
        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 0.0f);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        renderSpaceBodies(poseStack, proj, skyViewMatrix, partialtick); // use skyView because it is relative to universe 0,0,0

        if (atmosphereRenderTarget.width != windowWidth || atmosphereRenderTarget.height != windowHeight * 2) {
            if (windowWidth * windowHeight > 20000) { // small screen / minimized could cause crashes otherwise
                atmosphereRenderTarget.resize(windowWidth, windowHeight, false);
            }
        }
        // Bind FBO for planet rendering
        this.atmosphereRenderTarget.bindWrite(true);
        // Clear with transparent black
        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 0.0f);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        //renderSkyBox(proj, view, skyViewMatrix, partialtick); // use normal view in skybox because it is relative to player


        // Switch back to main render target
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);

        // Blit the planet texture onto the screen, no blending, overwrite whatever exists (nothing exists)
        //this.atmosphereRenderTarget.blitToScreen(windowWidth, windowHeight, true);
        this.planetRenderTarget.blitToScreen(windowWidth, windowHeight, true);

        // Clear depth buffer for subsequent rendering
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);
    }
}
