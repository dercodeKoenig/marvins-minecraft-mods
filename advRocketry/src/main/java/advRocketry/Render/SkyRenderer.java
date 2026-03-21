package advRocketry.Render;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.PlanetRenderCache;
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
import net.minecraft.client.renderer.texture.TextureManager;
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

    public static TextureTarget PlanetsTarget;
    public static TextureTarget AtmosphereTarget;
    public static TextureTarget bloomBrightTarget;
    public static TextureTarget bloomBlurTarget1;
    public static TextureTarget bloomBlurTarget2;

    boolean finishedLoading = false;
    public static long startTime;


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
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        texturemanager.getTexture(planetDimension.getTexture()).setFilter(true, true);
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

        Vector3f TargetSkyColor = RenderUtils.gamma_reverse(planetDimension.getSkyColor());
        shader.getUniform("TargetSkyColor").set(TargetSkyColor);

        Vector3f TargetCloudColor = RenderUtils.gamma_reverse(planetDimension.computeRawCloudColor());
        shader.getUniform("TargetCloudColor").set(TargetCloudColor);

        float TargetCloudValue = planetDimension.computeCloudValue();
        shader.getUniform("TargetCloudValue").set(TargetCloudValue);

        Vector3f TargetReflectiveTextureTintColor = RenderUtils.gamma_reverse(planetDimension.getReflectiveTextureTintColor());
        shader.getUniform("TargetReflectiveTextureTintColor").set(TargetReflectiveTextureTintColor);

        shader.getUniform("BrightnessMultiplier").set(brightnessModifider);

        shader.getUniform("playerHeight").set(playerHeightAboveSea);

        shader.getUniform("playerEye").set(eyePos);

        shader.getUniform("planetSkyHeight").set((float) Config.INSTANCE.planet_Sky_Height);

        Vector3f localTerrainFogColor = RenderUtils.gamma_reverse(myCurrentFogColor);
        shader.getUniform("localTerrainFogColor").set(localTerrainFogColor);

        float time = (float)(System.currentTimeMillis() - startTime) / 1000f;
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
            float planetGeometryScale,
            float brightnessModifider,
            float partialtick
    ) {
        // nice thing, the planet matrix is already transformed
        RenderSystem.setShader(shaderUtils::getRingSystemShader);
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/planet/8k_saturn_ring_alpha.png");
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        texturemanager.getTexture(tex).setFilter(true, true);
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

    void createStarBackgroundBuffer() {
        int starCount = 1000;
        vertexBufferStarBackground = new VertexBuffer(VertexBuffer.Usage.STATIC);
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(starCount * 4 * 16);
        BufferBuilder bufferbuilder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, POSITION_COLOR);
        float radius = 1000; // about 1 - 2 px on a full hd screen?
        float scale = 1f;
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

            Vector3f point1 = position.add(normal1.scale(-scale)).add(normal2.scale(-scale)).toVector3f();
            Vector3f point2 = position.add(normal1.scale(scale)).add(normal2.scale(-scale)).toVector3f();
            Vector3f point3 = position.add(normal1.scale(scale)).add(normal2.scale(scale)).toVector3f();
            Vector3f point4 = position.add(normal1.scale(-scale)).add(normal2.scale(scale)).toVector3f();

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

    void createRingSystemBuffer() {
        WavefrontObject ringModel;

        vertexBufferRingSystem = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            ringModel = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/environment/ring.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
        BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_NORMAL);
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
        BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_NORMAL);
        for (Face i : SkyBoxSphere.groupObjects.get("Icosphere").faces) {
            i.addFaceForRender(new PoseStack(), b);
        }
        MeshData mesh = b.build();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.upload(mesh);
        byteBuffer.close();
    }

    private void setupRenderTargets() {
        PlanetsTarget = new HDRTextureTarget(1000, 1000, true, false);
        AtmosphereTarget = new HDRTextureTarget(1000, 1000, false, false);
        bloomBrightTarget = new HDRTextureTarget(1000, 1000, false, false);
        bloomBlurTarget1 = new HDRTextureTarget(1000, 1000, false, false);
        bloomBlurTarget2 = new HDRTextureTarget(1000, 1000, false, false);
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

        // for star background
        Matrix4f newProj2 = new Matrix4f(proj);
        float n2 = 10f;
        float f2 = 10000f;
        newProj2.set(2, 2, -(f2 + n2) / (f2 - n2));
        newProj2.set(3, 2, -(2f * f2 * n2) / (f2 - n2));

        // render star background first
        // no depth write required
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

        // enable depth test for planet rendering so the rings render correctly only in front of the planet
        LEQUAL_DEPTH_TEST.setupRenderState();

        // for the proj matrix effects like bobbing, the planets have to be rendered FAR away or it will bounce around
        // we will scale translation and scale factor by this multiplier
        float distance_multiplier = 1000000; // this should be 1AU but i am not sure how it would handle precision at such scale and no idea if something breaks so...

        float playerHeightAboveSea = (float) Minecraft.getInstance().player.position().y - Minecraft.getInstance().level.getSeaLevel();

        Vec3 myDimensionPositionInSpace = myCurrentSpaceObject.getPosition(partialtick);

        // Render planets / stars
        for (PlanetDimension otherDimension : PlanetRenderCache.INSTANCE.getPlanetsToRenderInSky()) {

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
                float playerHeightAboveMyPlanetCenterAU =
                        (float) (CelestialUtils.toAU(
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
            double scaleAU = CelestialUtils.toAU(trueRadius); // bc we do not render at true distance multiplier ( 1AU is too large to handle ) we need to also scale the size correctly
            double planetGeometryScale = (scaleAU * distance_multiplier * Config.INSTANCE.planet_Render_Scale_Multiplier);
            planetMatrix.scale((float) planetGeometryScale);

            // custom proj matrix for every draw because of high potential distance range
            Matrix4f newProj = new Matrix4f(proj);
            float n = (float) (relativePos.length() / 10000);
            float f = (float) (relativePos.length() * 100);
            newProj.set(2, 2, -(f + n) / (f - n));
            newProj.set(3, 2, -(2f * f * n) / (f - n));

            if (!skipPlanetRender) {

                renderPlanet(
                        otherDimension,
                        newProj,
                        viewMatrix,
                        worldMatrix,
                        planetMatrix,
                        new Vector3f(0,0,0),
                        myCurrentSpaceObject.getAtmosphereDensity(),
                        myCurrentSpaceObject.getSunRiseColor(),
                        myCurrentSpaceObject.computeTerrainFogColor(partialtick),
                        playerHeightAboveSea,
                        isMyDimension,
                        1,
                        partialtick
                );

            }

            if (otherDimension.hasRings()) {
                renderRingSystem(
                        otherDimension,
                        newProj,
                        viewMatrix,
                        worldMatrix,
                        planetMatrix,
                        new Vector3f(0,0,0),
                        (float) planetGeometryScale,
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
        // adjust frame buffer size for render
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        adjustRenderTargetSize(PlanetsTarget, windowWidth, windowHeight, 1f);
        adjustRenderTargetSize(AtmosphereTarget, windowWidth, windowHeight, 0.25f);

        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1f);

        PlanetsTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        renderSpaceBodies(proj, view, worldMatrix, partialtick);

        AtmosphereTarget.bindWrite(true);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        renderSkyBox(proj, view, worldMatrix, partialtick);


        ShaderInstance shader;

        // post processing

        GlStateManager._depthMask(false);

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

        GlStateManager._depthMask(true);

        // Clear depth buffer for subsequent rendering
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);
    }
}
