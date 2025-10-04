package advRocketry.Render;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import advRocketry.*;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.DimensionProperties;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.CelestialUtils;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.opengl.GL30;

import java.lang.Math;

import static advRocketry.Render.shaderUtils.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class skyrenderer {

    // true scale is way too small, for example earth would only cover 8px on a 1080p screen.
    // solution: artificially scale up planet size for rendering
    static final float PLANET_RENDER_SCALE_MULTIPLIER = 10f;

    VertexBuffer vertexBufferSkyBox;
    VertexBuffer vertexBufferPlanet;
    VertexBuffer vertexBufferSquare;
    boolean finishedLoading = false;

    public skyrenderer() {
        RenderSystem.recordRenderCall(() -> {
            createSkyBoxBuffer();
            createPlanetBuffer();
            createSquareBuffer();
            setupRenderTargets();
            finishedLoading = true;
        });
    }

    void createSquareBuffer(){
        vertexBufferSquare = new VertexBuffer(VertexBuffer.Usage.STATIC);
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(64);
        BufferBuilder bufferbuilder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, POSITION);
        bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
        bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);
        bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);
        bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);
        MeshData mesh = bufferbuilder.build();
        vertexBufferSquare.bind();
        vertexBufferSquare.upload(mesh);
        byteBuffer.close();
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


    public static skyrenderer INSTANCE = new skyrenderer();

    private TextureTarget PlanetsTarget;
    private TextureTarget AtmosphereTarget;
    private TextureTarget bloomLowTarget;
    private TextureTarget bloomHighTarget;
    private TextureTarget bloomBlurTarget;

    public void setupRenderTargets() {
        this.PlanetsTarget = new HDRTextureTarget(1000, 1000, true, false);
        this.AtmosphereTarget = new HDRTextureTarget(1000, 1000, false, false);
        this.bloomLowTarget = new HDRTextureTarget(1000, 1000, false, false);
        this.bloomHighTarget = new HDRTextureTarget(1000, 1000, false, false);
        this.bloomBlurTarget = new HDRTextureTarget(1000, 1000, false, false);
    }

    public void renderSkyBox(Matrix4f proj, Matrix4f view, Matrix4f skyView, float partialTick) {
        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        Dimension myPlanet = DimensionManager.get(myId);
        Vec3 myPlanetPosition = myPlanet.getPosition(partialTick);


        Matrix4f atmMatrix = new Matrix4f(view);
        atmMatrix.scale(Minecraft.getInstance().gameRenderer.getRenderDistance()); // this prevents bobbing by zooming out

        // TODO when i increase y it should slowly go out of atmosphere, task for shader..., also render the planet below

        RenderSystem.setShader(shaderUtils::getAtmosphereShader);
        ShaderInstance shader = RenderSystem.getShader();

        shader.getUniform("ModelViewMat").set(atmMatrix);
        shader.getUniform("ProjMat").set(proj);
        shader.getUniform("skyViewMat").set(skyView); // so it can transform universe global coordinates into view space

        //shader.getUniform("screenWidth").set(skyRenderTarget.width);
        //shader.getUniform("screenHeight").set(skyRenderTarget.height);
        //shader.getUniform("playerHeight").set((float) Minecraft.getInstance().player.position().y - Minecraft.getInstance().level.getSeaLevel());
        //shader.getUniform("renderDistance").set(Minecraft.getInstance().gameRenderer.getRenderDistance());

        int totalLights = 0;
        for (ResourceLocation lightSourceId : myPlanet.planetRenderCache.significantLightSourcesCache.keySet()){
            Dimension star = DimensionManager.get(lightSourceId);
            Vec3 StarPos = star.getPosition(partialTick);
            Vec3 LightVector = myPlanetPosition.subtract(StarPos).scale(-1); //shader uses planet to star for dot product
            shader.getUniform("LightVectors["+ totalLights +"]").set((float) LightVector.x, (float) LightVector.y, (float) LightVector.z);
            shader.getUniform("LightColors["+ totalLights +"]").set(star.getEmissiveColor().x, star.getEmissiveColor().y, star.getEmissiveColor().z, star.getEmissiveColor().w);
            totalLights+=1;
        }
        shader.getUniform("LightCount").set(totalLights);

        shader.getUniform("SkyColor").set(myPlanet.getSkyColor().x, myPlanet.getSkyColor().y, myPlanet.getSkyColor().z);
        shader.getUniform("SunriseColor").set(myPlanet.getSkyColor().x, myPlanet.getSkyColor().y, myPlanet.getSkyColor().z);

        float[] fogColor = RenderSystem.getShaderFogColor(); // this is probably using custom overwrite anyway but this is where it is supposed to get the fog color from
        //shader.getUniform("FogColor").set(fogColor[0], fogColor[1], fogColor[2]);


        // using the real planet radius looks bad because the terrain is not rendered.
        //TODO maybe render the planet sphere below??
        //shader.getUniform("renderDistance").set((float) CelestialUtils.getRealRadiusFromValue(myPlanet.size));

        shader.apply();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.draw();
        shader.clear();
        VertexBuffer.unbind();
    }

    public void renderSpaceBodies(PoseStack poseStack, Matrix4f proj, Matrix4f skyViewMatrix, float partialtick) {


        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        Dimension myPlanet = DimensionManager.get(myId);
        Vec3 myPlanetPosition = myPlanet.getPosition(partialtick);

        // use custom near / far for rendering planets and stars, depth precision error should not be significant as space objects are sparsely distributed
        float fovy = 2f * (float) Math.atan(1.0f / proj.get(1, 1));
        float aspect = proj.get(1, 1) / proj.get(0, 0);
        Matrix4f newProj = new Matrix4f().perspective(fovy, aspect, 0.0001f, 100_00);

        // Setup render states for planets
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();


        // Render planets / stars
        // TODO: cache what dimensions should be rendered and not render all for efficiency
        for (Dimension otherDimension : DimensionManager.INSTANCE.dimensions.values()) {
            // skip self
            if (otherDimension.getDimensionId().equals(myPlanet.getDimensionId())) continue;

            Matrix4f planetModelMatrix = new Matrix4f();

            Vec3 otherPosition = otherDimension.getPosition(partialtick);

            Vec3 relativePos = otherPosition.subtract(myPlanetPosition); // in Astronomical units
            planetModelMatrix.translate((float) relativePos.x, (float) relativePos.y, (float) relativePos.z);

            Vec3 modelUp = new Vec3(0, 1, 0);
            Vec3 targetNorth = otherDimension.getRotationAxis().normalize();
            Vec3 rotAxis = modelUp.cross(targetNorth);
            if (rotAxis.length() > 1e-9) {
                double rotAngleRad = Math.asin(rotAxis.length());
                planetModelMatrix.rotate(new Quaternionf().fromAxisAngleRad(rotAxis.toVector3f(), (float) rotAngleRad));
            } else if (modelUp.dot(targetNorth) < 0) {
                planetModelMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vec3(1, 0, 0).toVector3f(), 180f));
            }

            double planetRotationAngle = otherDimension.getRotationAngle(partialtick);
            planetModelMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 1, 0), (float) planetRotationAngle));

            // to scale correctly we need to convert the radius (in earth radius multiplier) to astronomical units
            double trueRadius = CelestialUtils.fromEarthRadius(otherDimension.getEarthRadiusMultiplier());
            double scaleAU = CelestialUtils.toAU(trueRadius);
            planetModelMatrix.scale((float) scaleAU);
            planetModelMatrix.scale(PLANET_RENDER_SCALE_MULTIPLIER); // true size is too small, so apply a fixed scale

            RenderSystem.setShader(shaderUtils::getPlanetShader);
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(otherDimension.getTexture()).setFilter(true, true);
            RenderSystem.setShaderTexture(0, otherDimension.getTexture());
            ShaderInstance shader = RenderSystem.getShader();

            shader.getUniform("ModelViewMat").set(planetModelMatrix);
            shader.getUniform("ProjMat").set(newProj);
            shader.getUniform("skyViewMat").set(skyViewMatrix); // so it can transform universe global coordinates into view space

            shader.getUniform("reflectivity").set(otherDimension.getReflectivity());
            shader.getUniform("emissiveColor").set(otherDimension.getEmissiveColor());

            int totalLights = 0;
            for (ResourceLocation lightSourceId : otherDimension.planetRenderCache.significantLightSourcesCache.keySet()){
                Dimension star = DimensionManager.get(lightSourceId);
                Vec3 StarPos = star.getPosition(partialtick);
                Vec3 LightVector = otherPosition.subtract(StarPos).scale(-1); //shader uses planet to star for dot product
                shader.getUniform("LightVectors["+ totalLights +"]").set((float) LightVector.x, (float) LightVector.y, (float) LightVector.z);
                shader.getUniform("LightColors["+ totalLights +"]").set(star.getEmissiveColor().x, star.getEmissiveColor().y, star.getEmissiveColor().z, star.getEmissiveColor().w);
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

    public void adjustRenderTargetSize(RenderTarget renderTarget, int w, int h, float multiplier){
        int targetW = (int) (w  *multiplier);
        int targetH = (int) (h  *multiplier);
        if (renderTarget.width != targetW || renderTarget.height != targetH * 2) {
            if (w * h > 20000) { // small screen / minimized could cause crashes otherwise
                renderTarget.resize(targetW, targetH, false);
            }
        }
    }

    public void renderSky(PoseStack poseStack, Matrix4f proj, Matrix4f view, float partialtick) {
        if (!finishedLoading) return;


        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        Dimension myPlanet = DimensionManager.get(myId);

        AxisDirections myGlobalAxis = myPlanet.getGlobalAxisDirections(partialtick);

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

        adjustRenderTargetSize(PlanetsTarget,windowWidth,windowHeight, 1);
        adjustRenderTargetSize(AtmosphereTarget,windowWidth,windowHeight, 1);

        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 0.0f);

        this.PlanetsTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        renderSpaceBodies(poseStack, proj, skyViewMatrix, partialtick);

        this.AtmosphereTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        renderSkyBox(proj, view, skyViewMatrix, partialtick);

        // Switch back to main render target
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);

        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        RenderSystem.setShader(shaderUtils:: getBlitAddTonemapShader);
        ShaderInstance shader = RenderSystem.getShader();
        shader.setSampler("SpaceBackground", PlanetsTarget.getColorTextureId());
        shader.setSampler("Atmosphere", AtmosphereTarget.getColorTextureId());
        shader.apply();
        vertexBufferSquare.bind();
        vertexBufferSquare.draw();
        shader.clear();
        VertexBuffer.unbind();
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);
        // Clear depth buffer for subsequent rendering
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);
    }
}
