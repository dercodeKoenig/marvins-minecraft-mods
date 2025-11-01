package advRocketry.Render;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import advRocketry.*;
import advRocketry.Dimension.*;
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
    VertexBuffer vertexBufferStarBackground;
    boolean finishedLoading = false;

    public skyrenderer() {
        RenderSystem.recordRenderCall(() -> {
            createSkyBoxBuffer();
            createPlanetBuffer();
            createSquareBuffer();
            setupRenderTargets();
            createStarBackgroundBuffer();
            finishedLoading = true;
        });
    }

    void createStarBackgroundBuffer() {
        int starCount = 1000;
        vertexBufferStarBackground = new VertexBuffer(VertexBuffer.Usage.STATIC);
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(starCount * 4 * 16);
        BufferBuilder bufferbuilder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, POSITION_COLOR);
        float radius = 1000;
        Random random = new Random(42);
        for (int i = 0; i < starCount; i++) {
            double theta = random.nextFloat() * 2.0 * Math.PI; // azimuth
            double phi = Math.acos(2.0 * random.nextFloat() - 1.0); // polar angle
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);

            Vec3 position = new Vec3(x, y, z).normalize().scale(radius);

            Vec3 normal1 = position.cross(new Vec3(0, 1, 0)).normalize().scale(1);
            Vec3 normal2 = normal1.cross(position).normalize().scale(1);

            Vector3f point1 = position.add(normal1.scale(-1)).add(normal2.scale(-1)).toVector3f();
            Vector3f point2 = position.add(normal1.scale(1)).add(normal2.scale(-1)).toVector3f();
            Vector3f point3 = position.add(normal1.scale(1)).add(normal2.scale(1)).toVector3f();
            Vector3f point4 = position.add(normal1.scale(-1)).add(normal2.scale(1)).toVector3f();

            Vector4f color = new Vector4f(0.9f + random.nextFloat() * 0.1f, 0.9f + random.nextFloat() * 0.1f, 0.9f + random.nextFloat() * 0.1f, 1f);
            color.mul(0.0f + random.nextFloat() * 1f);

            bufferbuilder.addVertex(point1.x, point1.y, point1.z).setColor(color.x, color.y, color.z, color.w);
            bufferbuilder.addVertex(point2.x, point2.y, point2.z).setColor(color.x, color.y, color.z, color.w);
            bufferbuilder.addVertex(point3.x, point3.y, point3.z).setColor(color.x, color.y, color.z, color.w);
            bufferbuilder.addVertex(point4.x, point4.y, point4.z).setColor(color.x, color.y, color.z, color.w);
        }

        MeshData mesh = bufferbuilder.build();
        vertexBufferStarBackground.bind();
        vertexBufferStarBackground.upload(mesh);
        byteBuffer.close();
    }


    void createSquareBuffer() {
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
    private TextureTarget bloomBrightTarget;
    private TextureTarget bloomBlurTarget1;
    private TextureTarget bloomBlurTarget2;

    public void setupRenderTargets() {
        this.PlanetsTarget = new HDRTextureTarget(1000, 1000, true, false);
        this.AtmosphereTarget = new HDRTextureTarget(1000, 1000, false, false);
        this.bloomBrightTarget = new HDRTextureTarget(1000, 1000, false, false);
        this.bloomBlurTarget1 = new HDRTextureTarget(1000, 1000, false, false);
        this.bloomBlurTarget2 = new HDRTextureTarget(1000, 1000, false, false);
    }

    public void renderSkyBox(Matrix4f proj, Matrix4f view, Matrix4f worldMatrix, float partialTick) {
        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        Dimension myCurrentSpaceObject = DimensionManager.get(myId);
        Vec3 myCurrentPositionInSpace = myCurrentSpaceObject.getPosition(partialTick);


        Matrix4f atmMatrix = new Matrix4f();
        atmMatrix.scale(Minecraft.getInstance().gameRenderer.getRenderDistance()); // this prevents bobbing by zooming out

        RenderSystem.setShader(shaderUtils::getLocalAtmosphereShader);
        ShaderInstance shader = RenderSystem.getShader();


        shader.getUniform("WorldMat").set(worldMatrix);
        shader.getUniform("ProjMat").set(proj);
        shader.getUniform("ViewMat").set(view);
        shader.getUniform("ModelMat").set(atmMatrix);

        int totalLights = 0;
        for (ResourceLocation lightSourceId : myCurrentSpaceObject.getCurrentMainStars()) {
            Dimension star = DimensionManager.get(lightSourceId);
            Vec3 StarPos = star.getPosition(partialTick);
            Vec3 LightVector = myCurrentPositionInSpace.subtract(StarPos).scale(-1); //shader uses planet to star for dot product
            shader.getUniform("LightVectors[" + totalLights + "]").set((float) LightVector.x, (float) LightVector.y, (float) LightVector.z);
            shader.getUniform("LightColors[" + totalLights + "]").set(star.getEmissiveColor().x, star.getEmissiveColor().y, star.getEmissiveColor().z, star.getEmissiveColor().w);
            totalLights += 1;
        }
        shader.getUniform("LightCount").set(totalLights);

        shader.getUniform("SkyColor").set(myCurrentSpaceObject.getSkyColor().x, myCurrentSpaceObject.getSkyColor().y, myCurrentSpaceObject.getSkyColor().z);
        shader.getUniform("SunriseColor").set(myCurrentSpaceObject.getSunRiseColor().x, myCurrentSpaceObject.getSunRiseColor().y, myCurrentSpaceObject.getSunRiseColor().z);
        shader.getUniform("FogColor").set(myCurrentSpaceObject.getFogColor().x, myCurrentSpaceObject.getFogColor().y, myCurrentSpaceObject.getFogColor().z);

        shader.getUniform("playerHeight").set((float) Minecraft.getInstance().player.position().y - Minecraft.getInstance().level.getSeaLevel());

        shader.getUniform("AtmDensity").set(myCurrentSpaceObject.getAtmosphereDensity());

        //TODO maybe render the planet sphere below??
        //shader.getUniform("renderDistance").set((float) CelestialUtils.getRealRadiusFromValue(myPlanet.size));

        shader.apply();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.draw();
        shader.clear();
        VertexBuffer.unbind();
    }

    public void renderSpaceBodies(Matrix4f proj, Matrix4f viewMatrix, Matrix4f worldMatrix, float partialtick) {

        // for the proj matrix effects like bobbing, the planets have to be rendered FAR away or it will bounce around
        // we will scale translation and scale factor by this multiplier
        float distance_multiplier = 100000; // this should be 1AU but i am not sure how it would handle precision at such scale and no idea if something breaks so...

        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        Dimension myCurrentSpaceObject = DimensionManager.get(myId);
        Vec3 myCurrentPositionInSpace = myCurrentSpaceObject.getPosition(partialtick);

        // use custom near / far for rendering planets and stars, depth precision error should not be significant as space objects are sparsely distributed
        // note that the minecraft proj matrix has effects like bobbing that needs to be preserved
        Matrix4f newProj = new Matrix4f(proj);
        float n = 0.001f * distance_multiplier;
        float f = 100f * distance_multiplier;
        newProj.set(2, 2, -(f + n) / (f - n));
        newProj.set(3, 2, -(2f * f * n) / (f - n));

        Matrix4f newProj2 = new Matrix4f(proj);
        float n2 = 10f;
        float f2 = 10000f;
        newProj2.set(2, 2, -(f2 + n2) / (f2 - n2));
        newProj2.set(3, 2, -(2f * f2 * n2) / (f2 - n2));


        // render star background first
        NO_DEPTH_TEST.setupRenderState();
        GlStateManager._depthMask(false);

        RenderSystem.setShader(shaderUtils::getstarBackgroundShader);
        ShaderInstance shader = RenderSystem.getShader();
        shader.getUniform("ViewMat").set(viewMatrix);
        shader.getUniform("WorldMat").set(worldMatrix);
        shader.getUniform("ModelMat").set(new Matrix4f());
        shader.getUniform("ProjMat").set(newProj2);
        shader.apply();
        vertexBufferStarBackground.bind();
        vertexBufferStarBackground.draw();
        shader.clear();

        GlStateManager._depthMask(true);
        NO_DEPTH_TEST.clearRenderState();


        // Setup render states for planets
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();

        // Render planets / stars
        for (ResourceLocation otherDimensionId : myCurrentSpaceObject.getPlanetsToRenderInSky()) {
            // skip self TODO: this should be later calculated in myCurrentSpaceObject.getPlanetsToRenderInSky
            if (otherDimensionId.equals(myCurrentSpaceObject.getDimensionId())) continue;

            Dimension otherDimension = DimensionManager.get(otherDimensionId);

            // skip if it is a not visible dimension
            if (!otherDimension.shouldRenderInSky()) continue;

            Matrix4f planetMatrix = new Matrix4f();

            Vec3 otherPosition = otherDimension.getPosition(partialtick);

            Vec3 relativePos = otherPosition.subtract(myCurrentPositionInSpace); // in Astronomical units
            relativePos = relativePos.scale(distance_multiplier);
            planetMatrix.translate((float) relativePos.x, (float) relativePos.y, (float) relativePos.z);

            Vec3 modelUp = new Vec3(0, 1, 0);
            Vec3 targetNorth = otherDimension.getRotationAxis().normalize();
            Vec3 rotAxis = modelUp.cross(targetNorth);
            if (rotAxis.length() > 1e-9) {
                double rotAngleRad = Math.asin(rotAxis.length());
                planetMatrix.rotate(new Quaternionf().fromAxisAngleRad(rotAxis.toVector3f(), (float) rotAngleRad));
            } else if (modelUp.dot(targetNorth) < 0) {
                planetMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vec3(1, 0, 0).toVector3f(), 180f));
            }

            double planetRotationAngle = otherDimension.getRotationAngle(partialtick);
            planetMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 1, 0), (float) planetRotationAngle));

            // to scale correctly we need to convert the radius (in earth radius multiplier) to astronomical units
            double trueRadius = CelestialUtils.fromEarthRadius(otherDimension.getEarthRadiusMultiplier());
            double scaleAU = CelestialUtils.toAU(trueRadius);
            planetMatrix.scale((float) scaleAU);
            planetMatrix.scale(distance_multiplier);
            planetMatrix.scale(PLANET_RENDER_SCALE_MULTIPLIER); // true size is too small, so apply a fixed scale

            RenderSystem.setShader(shaderUtils::getPlanetShader);
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(otherDimension.getTexture()).setFilter(true, true);
            RenderSystem.setShaderTexture(0, otherDimension.getTexture());
            shader = RenderSystem.getShader();

            shader.getUniform("ProjMat").set(newProj);
            shader.getUniform("ViewMat").set(viewMatrix);
            shader.getUniform("WorldMat").set(worldMatrix); // so it can transform universe space to world space
            shader.getUniform("ModelMat").set(planetMatrix); // the planet transformation in universe space

            shader.getUniform("emissiveColor").set(otherDimension.getEmissiveColor());

            shader.getUniform("AtmDensity").set(myCurrentSpaceObject.getAtmosphereDensity());
            shader.getUniform("LocalSunriseColor").set(myCurrentSpaceObject.getSunRiseColor().x, myCurrentSpaceObject.getSunRiseColor().y, myCurrentSpaceObject.getSunRiseColor().z);
            shader.getUniform("TargetVector").set((float) relativePos.x, (float) relativePos.y, (float) relativePos.z);
            shader.getUniform("TargetAtmDensity").set(otherDimension.getAtmosphereDensity());
            shader.getUniform("TargetSkyColor").set(otherDimension.getSkyColor().x, otherDimension.getSkyColor().y, otherDimension.getSkyColor().z);
            shader.getUniform("playerHeight").set((float) Minecraft.getInstance().player.position().y - Minecraft.getInstance().level.getSeaLevel());

            int totalLights = 0;
            for (ResourceLocation lightSourceId : otherDimension.getCurrentMainStars()) {
                Dimension star = DimensionManager.get(lightSourceId);
                Vec3 StarPos = star.getPosition(partialtick);
                Vec3 LightVector = otherPosition.subtract(StarPos).scale(-1); //shader uses planet to star for dot product
                shader.getUniform("LightVectors[" + totalLights + "]").set((float) LightVector.x, (float) LightVector.y, (float) LightVector.z);
                shader.getUniform("LightColors[" + totalLights + "]").set(star.getEmissiveColor().x, star.getEmissiveColor().y, star.getEmissiveColor().z, star.getEmissiveColor().w);
                totalLights += 1;
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

    public void adjustRenderTargetSize(RenderTarget renderTarget, int w, int h, float multiplier) {
        int targetW = (int) (w * multiplier);
        int targetH = (int) (h * multiplier);
        if (renderTarget.width != targetW || renderTarget.height != targetH) {
            if (w * h > 20000) { // small screen / minimized could cause crashes otherwise
                renderTarget.resize(targetW, targetH, false);
            }
        }
    }

    public void renderSky(Matrix4f proj, Matrix4f view, float partialtick) {
        if (!finishedLoading) return;
        //if(true)return;

        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        Dimension myCurrentSpaceObject = DimensionManager.get(myId);
        if (myCurrentSpaceObject == null) return;
        if (!myCurrentSpaceObject.hasCustomSky()) return;

        AxisDirections myGlobalAxis = myCurrentSpaceObject.getGlobalAxisDirections(partialtick);

        // Create the base orientation for our skybox using the planet's axes.
        // This matrix transforms global space coordinates into world coordinates
        Matrix4f worldMatrix = new Matrix4f().lookAt(
                new Vector3f(0, 0, 0),
                myGlobalAxis.north.toVector3f(),
                myGlobalAxis.up.toVector3f()    // Up direction
        );
        // adjust frame buffer size for render
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        adjustRenderTargetSize(PlanetsTarget, windowWidth, windowHeight, 2f);
        adjustRenderTargetSize(AtmosphereTarget, windowWidth, windowHeight, 0.25f);

        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1f);

        this.PlanetsTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        renderSpaceBodies(proj, view, worldMatrix, partialtick);

        this.AtmosphereTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        renderSkyBox(proj, view, worldMatrix, partialtick);


        ShaderInstance shader;

        // post processing

        NO_DEPTH_TEST.setupRenderState();

        vertexBufferSquare.bind();

        float bloomWindowSizeMultiplier = 1f;
        adjustRenderTargetSize(bloomBlurTarget1, 480, 270, bloomWindowSizeMultiplier);
        adjustRenderTargetSize(bloomBlurTarget2, 480, 270, bloomWindowSizeMultiplier);
        adjustRenderTargetSize(bloomBrightTarget, 480, 270, bloomWindowSizeMultiplier);

        // blit extract bright regions
        bloomBrightTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        RenderSystem.setShader(shaderUtils::getBlitExtractBrightShader);
        shader = RenderSystem.getShader();
        shader.setSampler("frame", PlanetsTarget.getColorTextureId());
        shader.getUniform("threshold").set(1f);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();

        // blit blur
        RenderSystem.setShader(shaderUtils::getBlitBlurShader);
        shader = RenderSystem.getShader();

        bloomBlurTarget1.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        shader.setSampler("image", bloomBrightTarget.getColorTextureId());
        shader.getUniform("resolution").set(bloomBrightTarget.width);
        shader.getUniform("horizontal").set(1);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();

        bloomBlurTarget2.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        shader.setSampler("image", bloomBlurTarget1.getColorTextureId());
        shader.getUniform("resolution").set(bloomBlurTarget1.height);
        shader.getUniform("horizontal").set(0);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();


        // Switch back to main render target, combine framebuffers
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        RenderSystem.setShader(shaderUtils::getBlitAddTonemapShader);
        shader = RenderSystem.getShader();
        shader.setSampler("SpaceBackground", PlanetsTarget.getColorTextureId());
        shader.setSampler("SpaceBackgroundBloom", bloomBlurTarget2.getColorTextureId());
        shader.setSampler("Atmosphere", AtmosphereTarget.getColorTextureId());
        shader.getUniform("bloomIntensity").set(1f);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();


        VertexBuffer.unbind();
        NO_DEPTH_TEST.clearRenderState();

        // Clear depth buffer for subsequent rendering
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);
    }
}
