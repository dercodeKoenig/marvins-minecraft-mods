package advRocketry.Render;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.Main;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Utils.AxisDirections;
import advRocketry.Utils.CelestialUtils;
import advRocketry.Utils.ClientUtils;
import advRocketry.Utils.RenderUtils;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.opengl.GL30;

import java.lang.Math;

import static advRocketry.Render.shaderUtils.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class SkyRenderer {

    public static SkyRenderer INSTANCE = new SkyRenderer();

    public static VertexBuffer vertexBufferSkyBox;
    public static VertexBuffer vertexBufferPlanet;
    public static VertexBuffer vertexBufferRingSystem;
    public static VertexBuffer vertexBufferSquare;
    public static VertexBuffer vertexBufferStarBackground;

    public static TextureTarget AtmosphereTarget; // render atm first
    public static TextureTarget PlanetsAndStarsTarget;  // can use atm depth from alpha channel of AtmosphereTarget
    public static TextureTarget PlanetsStarsAndAtmosphereTarget; // add atmosphere and planets together
    public static TextureTarget bloomExtractBrightTarget; // extract bright regions for bloom shader
    public static TextureTarget bloomBlurHorizontal;   // blur horizontal
    public static TextureTarget bloomBlurVertical;  // blur vertical
    // final addition of bloomBlurVertical and PlanetsStarsAndAtmosphereTarget and post processing into main render target

    public static long startTime;
    boolean finishedLoading = false;


    public SkyRenderer() {
        RenderSystem.recordRenderCall(() -> {
            createSkyBoxBuffer();
            createPlanetBuffer();
            createRingSystemBuffer();
            createSquareBuffer();
            createStarBackgroundBuffer();
            setupRenderTargets();
            finishedLoading = true;
        });
        startTime = System.currentTimeMillis();
    }

    public static void renderPlanet(
            PlanetDimension planetDimension,
            Matrix4f proj,
            Matrix4f viewMatrix,
            Matrix4f worldMatrix,
            Matrix4f planetMatrix,
            Vector3f eyePos,
            float myAtmDensity,
            Vector3f mySunriseColor,
            Vector3f myCurrentFogColor,
            float playerHeightAboveSea,
            boolean isMyDimension,
            float brightnessModifider,
            float partialtick
    ) {
        RenderSystem.setShader(shaderUtils::getPlanetShader);

        RenderSystem.setShaderTexture(0, planetDimension.getTexture());
        ShaderInstance shader = RenderSystem.getShader();

        shader.getUniform("ProjMat").set(proj);
        shader.getUniform("ViewMat").set(viewMatrix);
        shader.getUniform("WorldMat").set(worldMatrix); // so it can transform universe space to world space
        shader.getUniform("ModelMat").set(planetMatrix); // the planet transformation in universe space

        Vector3f emissiveColor = RenderUtils.gamma_reverse(planetDimension.getEmissiveColor());
        shader.getUniform("emissiveColor").set(new Vector4f(emissiveColor.x, emissiveColor.y, emissiveColor.z, planetDimension.getRadiationIntensity()));

        shader.getUniform("LocalAtmDensity").set(myAtmDensity);

        Vector3f LocalSunriseColor = RenderUtils.gamma_reverse(mySunriseColor);
        shader.getUniform("LocalSunriseColor").set(LocalSunriseColor);

        shader.getUniform("TargetAtmDensity").set(planetDimension.getAtmosphereDensity());

        Vector3f TargetSunriseColor = RenderUtils.gamma_reverse(planetDimension.getSunRiseColor());
        shader.getUniform("TargetSunriseColor").set(TargetSunriseColor);

        Vector3f TargetSkyColor = RenderUtils.gamma_reverse(planetDimension.getSkyColor());
        shader.getUniform("TargetSkyColor").set(TargetSkyColor);

        Vector3f TargetCloudColor = RenderUtils.gamma_reverse(planetDimension.computeRawCloudColor());
        shader.getUniform("TargetCloudColor").set(TargetCloudColor);

        float TargetCloudValue = planetDimension.computeCloudValue();
        shader.getUniform("TargetCloudValue").set(TargetCloudValue);

        shader.getUniform("CloudWarp").set(Config.INSTANCE.planet_Cloud_Noise_Warp ? 1 : 0);
        shader.getUniform("CloudSampleSteps").set(Config.INSTANCE.planet_Cloud_Noise_Samples);

        shader.getUniform("TargetTextureTintColor").set(planetDimension.getTextureTintColor());

        shader.getUniform("BrightnessMultiplier").set(brightnessModifider);

        shader.getUniform("playerHeight").set(playerHeightAboveSea);

        shader.getUniform("playerEye").set(eyePos);

        shader.getUniform("planetSkyHeight").set((float) Config.INSTANCE.planet_Sky_Height);

        Vector3f localTerrainFogColor = RenderUtils.gamma_reverse(myCurrentFogColor);
        shader.getUniform("localTerrainFogColor").set(localTerrainFogColor);

        float time = (float) (System.currentTimeMillis() - startTime + planetDimension.getDimensionId().hashCode()) / 1000f;
        shader.getUniform("time").set(time);

        int totalLights = 0;
        Vec3 myPosition = planetDimension.getPosition(partialtick);
        for (ResourceLocation lightSourceId : planetDimension.getCurrentMainStars()) {
            Dimension star = DimensionManager.INSTANCE_CLIENT.get(lightSourceId);
            if (star == null) continue;
            Vec3 StarPos = star.getPosition(partialtick);
            Vec3 LightVector = myPosition.subtract(StarPos).scale(-1); //shader uses planet to star for dot product
            shader.getUniform("LightVectors[" + totalLights + "]").set((float) LightVector.x, (float) LightVector.y, (float) LightVector.z);
            Vector3f lightColor = RenderUtils.gamma_reverse(star.getEmissiveColor());
            shader.getUniform("LightColors[" + totalLights + "]").set(lightColor.x, lightColor.y, lightColor.z, star.getRadiationIntensity());
            totalLights += 1;
        }
        shader.getUniform("LightCount").set(totalLights);


        if (isMyDimension) {
            shader.getUniform("isLocalPlanet").set(1);
        } else {
            shader.getUniform("isLocalPlanet").set(0);
        }

        shader.apply();
        vertexBufferPlanet.bind();
        vertexBufferPlanet.draw();
        shader.clear();
    }

    public static void renderRingSystem(
            PlanetDimension planetDimension,
            Matrix4f proj,
            Matrix4f viewMatrix,
            Matrix4f worldMatrix,
            Matrix4f planetMatrix,
            Vector3f eyePos,
            float myAtmDensity,
            Vector3f mySunriseColor,
            float playerHeightAboveSea,
            float planetGeometryScale,
            float brightnessModifider,
            float partialtick
    ) {
        // nice thing, the planet matrix is already transformed
        RenderSystem.setShader(shaderUtils::getRingSystemShader);
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/planet/8k_saturn_ring_alpha.png");
        RenderSystem.setShaderTexture(0, tex);
        ShaderInstance shader = RenderSystem.getShader();
        shader.getUniform("ProjMat").set(proj);
        shader.getUniform("ViewMat").set(viewMatrix);
        shader.getUniform("WorldMat").set(worldMatrix);
        shader.getUniform("ModelMat").set(planetMatrix);

        shader.getUniform("playerEye").set(eyePos);

        shader.getUniform("scale").set(4f);
        shader.getUniform("planetGeometryScale").set(planetGeometryScale);

        shader.getUniform("BrightnessMultiplier").set(brightnessModifider);

        // for atm filter
        shader.getUniform("LocalAtmDensity").set(myAtmDensity);
        Vector3f LocalSunriseColor = RenderUtils.gamma_reverse(mySunriseColor);
        shader.getUniform("LocalSunriseColor").set(LocalSunriseColor);
        shader.getUniform("playerHeight").set(playerHeightAboveSea);
        shader.getUniform("playerEye").set(eyePos);
        shader.getUniform("planetSkyHeight").set((float) Config.INSTANCE.planet_Sky_Height);

        int totalLights = 0;
        Vec3 myPosition = planetDimension.getPosition(partialtick);
        for (ResourceLocation lightSourceId : planetDimension.getCurrentMainStars()) {
            Dimension star = DimensionManager.INSTANCE_CLIENT.get(lightSourceId);
            if (star == null) continue;
            Vec3 StarPos = star.getPosition(partialtick);
            Vec3 LightVector = myPosition.subtract(StarPos).scale(-1); //shader uses planet to star for dot product
            shader.getUniform("LightVectors[" + totalLights + "]").set((float) LightVector.x, (float) LightVector.y, (float) LightVector.z);
            Vector3f lightColor = RenderUtils.gamma_reverse(star.getEmissiveColor());
            shader.getUniform("LightColors[" + totalLights + "]").set(lightColor.x, lightColor.y, lightColor.z, star.getRadiationIntensity());
            totalLights += 1;
        }
        shader.getUniform("LightCount").set(totalLights);

        TRANSLUCENT_TRANSPARENCY.setupRenderState();
        NO_CULL.setupRenderState();

        shader.apply();
        vertexBufferRingSystem.bind();
        vertexBufferRingSystem.draw();
        shader.clear();

        TRANSLUCENT_TRANSPARENCY.clearRenderState();
        NO_CULL.clearRenderState();
    }

    public static void adjustRenderTargetSize(RenderTarget renderTarget, int w, int h, float multiplier) {
        int targetW = (int) (w * multiplier);
        int targetH = (int) (h * multiplier);
        if (renderTarget.width != targetW || renderTarget.height != targetH) {
            if (w * h > 20000) { // small screen / minimized could cause crashes otherwise
                renderTarget.resize(targetW, targetH, false);
            }
        }
    }

    public static void debugCommandRender() {
        //INSTANCE.createStarBackgroundBuffer();
    }

    void createStarBackgroundBuffer() {
        // making it too small makes it not work with the minecraft bobbing effect and stars will jump around
        int starCount = 8000;
        // need them far away or view bobbing will break shit
        float BoxSize = 50000;
        float scale = 5f;

        WavefrontObject cube;
        try {
            cube = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/environment/smooth_cube.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }

        vertexBufferStarBackground = new VertexBuffer(VertexBuffer.Usage.STATIC);
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(starCount * 8 * 32);
        BufferBuilder bufferbuilder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, STAR_BACKGROUND);


        for (int i = 0; i < starCount; i++) {
            // 1. Generate a random center
            float cx = (float) ((Math.random() - 0.5) * BoxSize);
            float cy = (float) ((Math.random() - 0.5) * BoxSize);
            float cz = (float) ((Math.random() - 0.5) * BoxSize);

            int color = RenderUtils.packColor(1, 1, 1, 1);

            for (Face face : cube.groupObjects.get("Cube").faces) {
                for (int j = 0; j < face.vertices.length; ++j) {
                    // Get the raw local vertex from the OBJ (e.g., -1.0 or 1.0)
                    float vx = face.vertices[j].x * scale;
                    float vy = face.vertices[j].y * scale;
                    float vz = face.vertices[j].z * scale;

                    // ADD THE CENTER TO THE POSITION
                    // STORE THE LOCAL OFFSET IN THE NORMAL
                    bufferbuilder
                            .addVertex(cx + vx, cy + vy, cz + vz)
                            .setColor(color)
                            .setNormal(vx / scale, vy / scale, vz / scale);
                    // We divide by scale so the normal is exactly -1.0 or 1.0
                }
            }
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

    void createRingSystemBuffer() {
        WavefrontObject ringModel;

        vertexBufferRingSystem = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            ringModel = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/environment/ring.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
        BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, POSITION_NORMAL);
        for (Face i : ringModel.groupObjects.get("Circle").faces) {
            i.addFaceForRender(new PoseStack(), b);
        }
        MeshData meshPlanet = b.build();
        vertexBufferRingSystem.bind();
        vertexBufferRingSystem.upload(meshPlanet);
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
        BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION);
        for (Face i : SkyBoxSphere.groupObjects.get("Icosphere").faces) {
            i.addFaceForRender(new PoseStack(), b);
        }
        MeshData mesh = b.build();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.upload(mesh);
        byteBuffer.close();
    }

    private void setupRenderTargets() {
        PlanetsAndStarsTarget = new HDRTextureTarget(1000, 1000, true, false);
        AtmosphereTarget = new HDRTextureTarget(1000, 1000, false, false);
        PlanetsStarsAndAtmosphereTarget = new HDRTextureTarget(1000, 1000, true, false);
        bloomExtractBrightTarget = new HDRTextureTarget(1000, 1000, false, false);
        bloomBlurHorizontal = new HDRTextureTarget(1000, 1000, false, false);
        bloomBlurVertical = new HDRTextureTarget(1000, 1000, false, false);
    }

    private void renderWarpTravelBox(Matrix4f proj, Matrix4f view, Matrix4f worldMatrix, float partialTick) {
        Dimension myCurrentSpaceObject = ClientUtils.getPlayerDimension();

        // render only on space station and only when warp travel to save gpu load
        if (myCurrentSpaceObject instanceof SpaceStationDimension spaceStation && spaceStation.getMovement().length() > 0.0001) {

            Matrix4f modelMat = new Matrix4f();
            modelMat.scale(Minecraft.getInstance().gameRenderer.getRenderDistance()); // this prevents bobbing by zooming out

            RenderSystem.setShader(shaderUtils::getWarpTravelShader);
            ShaderInstance shader = RenderSystem.getShader();

            shader.getUniform("WorldMat").set(worldMatrix);
            shader.getUniform("ProjMat").set(proj);
            shader.getUniform("ViewMat").set(view);
            shader.getUniform("ModelMat").set(modelMat);
            shader.getUniform("time").set((float) (System.currentTimeMillis() - startTime) / 1000);
            shader.getUniform("intensity").set((float) Math.pow((spaceStation.getMovement().length() - 0.0001) / Config.INSTANCE.station_SpaceTravel_AU_Per_Second * 20 * 5, 0.5));

            shader.apply();
            vertexBufferSkyBox.bind();
            vertexBufferSkyBox.draw();
            shader.clear();
            VertexBuffer.unbind();
        }
    }

    private void renderSkyBox(Matrix4f proj, Matrix4f view, Matrix4f worldMatrix, float partialTick) {
        Dimension myCurrentSpaceObject = ClientUtils.getPlayerDimension();

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
            Dimension star = DimensionManager.INSTANCE_CLIENT.get(lightSourceId);
            if (star == null) continue;
            Vec3 StarPos = star.getPosition(partialTick);
            Vec3 LightVector = myCurrentPositionInSpace.subtract(StarPos).scale(-1); //shader uses planet to star for dot product
            shader.getUniform("LightVectors[" + totalLights + "]").set((float) LightVector.x, (float) LightVector.y, (float) LightVector.z);
            Vector3f starColorLin = RenderUtils.gamma_reverse(star.getEmissiveColor());
            shader.getUniform("LightColors[" + totalLights + "]").set(starColorLin.x, starColorLin.y, starColorLin.z, star.getRadiationIntensity());
            totalLights += 1;
        }
        shader.getUniform("LightCount").set(totalLights);

        Vector3f SkyColorLin = RenderUtils.gamma_reverse(myCurrentSpaceObject.getSkyColor());
        shader.getUniform("SkyColor").set(SkyColorLin.x, SkyColorLin.y, SkyColorLin.z);

        Vector3f SunriseColorLin = RenderUtils.gamma_reverse(myCurrentSpaceObject.getSunRiseColor());
        shader.getUniform("SunriseColor").set(SunriseColorLin.x, SunriseColorLin.y, SunriseColorLin.z);

        Vector3f FogColorLin = RenderUtils.gamma_reverse(myCurrentSpaceObject.getFogColor());
        shader.getUniform("FogColor").set(FogColorLin.x, FogColorLin.y, FogColorLin.z);

        shader.getUniform("playerHeight").set((float) Minecraft.getInstance().player.position().y - Minecraft.getInstance().level.getSeaLevel());

        shader.getUniform("planetSkyHeight").set((float) Config.INSTANCE.planet_Sky_Height);

        shader.getUniform("AtmDensity").set(myCurrentSpaceObject.getAtmosphereDensity());

        shader.apply();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.draw();
        shader.clear();
        VertexBuffer.unbind();
    }

    private void renderSpaceBodies(Matrix4f proj, Matrix4f viewMatrix, Matrix4f worldMatrix, float partialtick) {

        Dimension myCurrentSpaceObject = ClientUtils.getPlayerDimension();
        Vec3 myDimensionPositionInSpace = myCurrentSpaceObject.getPosition(partialtick);

        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        // for star background
        // it is important that we not use set setPerspective because it kills the bobbing effect already inside proj and makes things look very strange
        Matrix4f newProj2 = new Matrix4f(proj);
        float n2 = 100f;
        float f2 = 1000000;
        newProj2.set(2, 2, -(f2 + n2) / (f2 - n2));
        newProj2.set(3, 2, -(2f * f2 * n2) / (f2 - n2));

        // render star background first
        // no depth write required
        GlStateManager._depthMask(false);
        Matrix4f starBackgroundModelMat = new Matrix4f();
        starBackgroundModelMat.translate(myDimensionPositionInSpace.toVector3f().mul(-1));

        RenderSystem.setShader(shaderUtils::getstarBackgroundShader);
        ShaderInstance shader = RenderSystem.getShader();
        shader.getUniform("ViewMat").set(viewMatrix);
        shader.getUniform("WorldMat").set(worldMatrix);
        shader.getUniform("ModelMat").set(starBackgroundModelMat);
        shader.getUniform("ProjMat").set(newProj2);
        // lots of atmosphere makes it dark, TODO: maybe also darken based on atmosphere & terrain brightness so they are only visible at night even on dark sky planets?
        float BrightnessModifier = (float) Math.exp(-myCurrentSpaceObject.getAtmosphereDensity() * 4);
        shader.getUniform("BrightnessModifier").set(BrightnessModifier);
        Vector3f movement = myCurrentSpaceObject.getMovement().toVector3f();
        shader.getUniform("WarpMovement").set(movement);
        shader.getUniform("ScreenSize").set(windowWidth, windowHeight);
        shader.apply();
        vertexBufferStarBackground.bind();
        vertexBufferStarBackground.draw();
        shader.clear();
        GlStateManager._depthMask(true);

        // enable depth test for planet rendering so the rings render correctly only in front of the planet
        LEQUAL_DEPTH_TEST.setupRenderState();

        float playerHeightAboveSea = (float) Minecraft.getInstance().player.position().y - Minecraft.getInstance().level.getSeaLevel();

        // Render planets / stars
        for (PlanetDimension otherDimension : PlanetRenderCache.INSTANCE.getPlanetsToRenderInSky()) {

            // current position could be slightly modified when this is my planet, thats why i make a copy
            Vec3 myCurrentPositionInSpace = myDimensionPositionInSpace;

            boolean isMyDimension = otherDimension.equals(myCurrentSpaceObject);

            boolean skipPlanetRender = false;

            // only render when we sit in rocket to reduce gpu load when it it not required
            if (isMyDimension) {
                skipPlanetRender = true;
                if ((Minecraft.getInstance().player.getVehicle() instanceof EntityRocket))
                    skipPlanetRender = false;
                if (playerHeightAboveSea > 300)
                    skipPlanetRender = false;
            }

            if (isMyDimension) {
                // special case: to correctly render the planet below, we need to add the up vector * radius * render multiplier to get the players location and not the planet center
                double playerHeightAboveMyPlanetCenterAU =
                        (CelestialUtils.toAU(
                                ((PlanetDimension) myCurrentSpaceObject).getEarthRadiusMultiplier()
                                        * CelestialUtils.EARTH_RADIUS
                                        * Config.INSTANCE.planet_Render_Scale_Multiplier
                                        * 1.0
                                        + Minecraft.getInstance().player.position().y * 1
                        ));
                Vec3 localUp = myCurrentSpaceObject.getGlobalAxisDirections(0).up;
                myCurrentPositionInSpace = myCurrentPositionInSpace.add(localUp.scale(playerHeightAboveMyPlanetCenterAU));
            }

            Matrix4f planetMatrix = new Matrix4f();

            Vec3 otherPosition = otherDimension.getPosition(partialtick);

            Vec3 relativePos = otherPosition.subtract(myCurrentPositionInSpace); // in Astronomical units
            relativePos = relativePos.scale(CelestialUtils.ASTRONOMICAL_UNIT); // scale in m. float precision is relative so this should work
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

            float brightness = 1.0f;

            // translation is in M, so scale has to be in M too.
            double trueRadius = CelestialUtils.fromEarthRadius(otherDimension.getEarthRadiusMultiplier());
            double geometryScale = trueRadius * Config.INSTANCE.planet_Render_Scale_Multiplier;

            // to avoid star not being rendered because to small, it should be scaled to cover 1 or 2 px minimum
            // but the brightness has to be scaled too or it wil look strange

            // A threshold representing roughly 1-2 pixels on screen.
            // Note: You may need to tweak this specific value slightly depending on your camera FOV!
            double minApparentSize = 0.001;

            double distance = relativePos.length();
            double apparentSizeRatio = geometryScale / distance;
            if (apparentSizeRatio < minApparentSize) {
                // 1. Inflate the star so it hits the minimum pixel size
                double scaleCorrection = minApparentSize / apparentSizeRatio;
                geometryScale *= scaleCorrection;

                // 2. Dim the star to conserve energy (inverse square law)
                // If we make it twice as big, it should be 4 times dimmer.
                brightness = (float) (1.0 / (scaleCorrection * scaleCorrection));

                // clamp brightness so it doesn't drop completely to 0 and disappear
                brightness = Math.max(brightness, 0.01f);
            }
            if (apparentSizeRatio < minApparentSize / 5) {
                // skip the render entirely
                continue;
            }

            planetMatrix.scale((float) geometryScale);


            // custom proj matrix for every draw because of high potential distance range
            n2 = (float) (relativePos.length() / 10000);
            f2 = (float) (relativePos.length() * 100);
            newProj2.set(2, 2, -(f2 + n2) / (f2 - n2));
            newProj2.set(3, 2, -(2f * f2 * n2) / (f2 - n2));

            float myAtmDensity = myCurrentSpaceObject.getAtmosphereDensity();

            if (!skipPlanetRender) {

                renderPlanet(
                        otherDimension,
                        newProj2,
                        viewMatrix,
                        worldMatrix,
                        planetMatrix,
                        new Vector3f(0, 0, 0),
                        myAtmDensity,
                        myCurrentSpaceObject.getSunRiseColor(),
                        myCurrentSpaceObject.computeTerrainFogColor(partialtick),
                        playerHeightAboveSea,
                        isMyDimension,
                        brightness,
                        partialtick
                );

            }

            if (otherDimension.hasRings()) {
                renderRingSystem(
                        otherDimension,
                        newProj2,
                        viewMatrix,
                        worldMatrix,
                        planetMatrix,
                        new Vector3f(0, 0, 0),
                        myAtmDensity,
                        myCurrentSpaceObject.getSunRiseColor(),
                        playerHeightAboveSea,
                        (float) geometryScale,
                        1,
                        partialtick
                );
            }

            // we do manual depth sorting, always render on top
            RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);
        }

        LEQUAL_DEPTH_TEST.clearRenderState();

        VertexBuffer.unbind();
    }

    public void renderSky(Matrix4f proj, Matrix4f view, float partialtick) {
        if (!finishedLoading) return;

        Dimension myCurrentSpaceObject = ClientUtils.getPlayerDimension();
        if (myCurrentSpaceObject == null) return;
        if (!myCurrentSpaceObject.hasCustomSky()) return;

        AxisDirections myGlobalAxis = myCurrentSpaceObject.getGlobalAxisDirections(partialtick);

        // Create the base orientation for our skybox using the planet's axes.
        // This matrix transforms global space coordinates into world coordinates
        Matrix4f worldMatrix = new Matrix4f().lookAt(
                new Vector3f(0, 0, 0),
                myGlobalAxis.front.toVector3f(),
                myGlobalAxis.up.toVector3f()    // Up direction
        );

        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        adjustRenderTargetSize(PlanetsAndStarsTarget, windowWidth, windowHeight, 1f);
        adjustRenderTargetSize(AtmosphereTarget, windowWidth, windowHeight, 0.25f);
        adjustRenderTargetSize(PlanetsStarsAndAtmosphereTarget, windowWidth, windowHeight, 1f);

        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1f);

        // render atmosphere first
        AtmosphereTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        if (myCurrentSpaceObject instanceof PlanetDimension)
            // only planets need atm shader
            renderSkyBox(proj, view, worldMatrix, partialtick);
        if (myCurrentSpaceObject instanceof SpaceStationDimension)
            // space station has now atm, but maybe warp travel effects
            renderWarpTravelBox(proj, view, worldMatrix, partialtick);

        // now render the planets and stars
        PlanetsAndStarsTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        renderSpaceBodies(proj, view, worldMatrix, partialtick);


        ShaderInstance shader;

        // post processing

        GlStateManager._depthMask(false);

        vertexBufferSquare.bind();

        float bloomWindowSizeMultiplier = 1f;
        adjustRenderTargetSize(bloomBlurHorizontal, 480, 270, bloomWindowSizeMultiplier);
        adjustRenderTargetSize(bloomBlurVertical, 480, 270, bloomWindowSizeMultiplier);
        adjustRenderTargetSize(bloomExtractBrightTarget, 480, 270, bloomWindowSizeMultiplier);

        // add atmosphere and stars/planets together
        PlanetsStarsAndAtmosphereTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        RenderSystem.setShader(shaderUtils::getBlitAddShader);
        shader = RenderSystem.getShader();
        shader.setSampler("Frame1", PlanetsAndStarsTarget.getColorTextureId());
        shader.setSampler("Frame2", AtmosphereTarget.getColorTextureId());
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();

        // blit extract bright regions
        bloomExtractBrightTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        RenderSystem.setShader(shaderUtils::getBlitExtractBrightShader);
        shader = RenderSystem.getShader();
        shader.setSampler("frame", PlanetsStarsAndAtmosphereTarget.getColorTextureId());
        shader.getUniform("threshold").set(1f);
        shader.getUniform("resolution").set(PlanetsStarsAndAtmosphereTarget.width, PlanetsStarsAndAtmosphereTarget.height);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();

        // blit blur
        RenderSystem.setShader(shaderUtils::getBlitBlurShader);
        shader = RenderSystem.getShader();

        bloomBlurHorizontal.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        shader.setSampler("image", bloomExtractBrightTarget.getColorTextureId());
        shader.getUniform("resolution").set(bloomExtractBrightTarget.width);
        shader.getUniform("horizontal").set(1);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();

        bloomBlurVertical.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        shader.setSampler("image", bloomBlurHorizontal.getColorTextureId());
        shader.getUniform("resolution").set(bloomBlurHorizontal.height);
        shader.getUniform("horizontal").set(0);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();


        // Switch back to main render target, combine framebuffers
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        RenderSystem.setShader(shaderUtils::getBlitPostProcessingShader);
        shader = RenderSystem.getShader();
        shader.setSampler("Frame", PlanetsStarsAndAtmosphereTarget.getColorTextureId());
        shader.setSampler("Bloom", bloomBlurVertical.getColorTextureId());
        shader.getUniform("bloomIntensity").set(1f);
        shader.apply();
        vertexBufferSquare.draw();
        shader.clear();

        VertexBuffer.unbind();

        GlStateManager._depthMask(true);

        // Clear depth buffer for subsequent rendering
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);
    }
}
