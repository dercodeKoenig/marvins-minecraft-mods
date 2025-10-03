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

import static advRocketry.shaderUtils.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class skyrenderer {

    // can modify fog distance
    public static void renderFog(ViewportEvent.RenderFog event) {
        ResourceLocation dimension = Minecraft.getInstance().level.dimension().location();
        event.setNearPlaneDistance(event.getNearPlaneDistance() * (float) (1d / (DimensionManager.INSTANCE.dimensions.get(dimension).atmosphereDensity + 0.0001)));
        event.setFarPlaneDistance( event.getFarPlaneDistance() * (float) (1d / (DimensionManager.INSTANCE.dimensions.get(dimension).atmosphereDensity + 0.0001)));
        event.setCanceled(true);
    }
    // can modify fog color
    public static void computeFogColor( ViewportEvent.ComputeFogColor event) {
        Vector3f color =  DimensionManager.INSTANCE.dimensions.get(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld")).getFogColor();
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

    public void renderSkyBox(Matrix4f proj, Matrix4f view){
        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);



        Matrix4f atmMatrix = new Matrix4f(view);
        atmMatrix.scale(Minecraft.getInstance().gameRenderer.getRenderDistance()); // this prevents bobbing by zooming out

        // TODO when i increase y it should slowly go out of atmosphere, task for shader...
        Vector3f atmColor = myPlanet.getAtmosphereColor();
        RenderSystem.setShader(shaderUtils::getAtmosphereShader);
        ShaderInstance shader = RenderSystem.getShader();
        shader.setSampler("planetTexture", planetRenderTarget.getColorTextureId());
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, atmMatrix, proj, Minecraft.getInstance().getWindow());
        Uniform color = shader.getUniform("Color");
        color.set(atmColor.x, atmColor.y, atmColor.z, 1f);
        shader.getUniform("screenWidth").set(atmosphereRenderTarget.width);
        shader.getUniform("screenHeight").set(atmosphereRenderTarget.height);
        shader.getUniform("playerHeight").set((float) Minecraft.getInstance().player.position().y);
        shader.getUniform("renderDistance").set(Minecraft.getInstance().gameRenderer.getRenderDistance());
        // using the real planet radius looks bad because the terrain is not rendered.
        //TODO maybe render the planet sphere below??
        //shader.getUniform("renderDistance").set((float) CelestialUtils.getRealRadiusFromValue(myPlanet.size));

        shader.apply();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.draw();
        shader.clear();
        VertexBuffer.unbind();
    }

    public void renderSpaceBodies(PoseStack poseStack, Matrix4f proj, Matrix4f skyViewMatrix, double partialtick){


        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);
        Vec3 myPlanetPosition = myPlanet.getPosition((float)partialtick);

        // Setup render states for planets
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();


        // Render planets to FBO
        for (DimensionProperties planet : DimensionManager.INSTANCE.dimensions.values()) {
            if (planet.dimensionId.equals(myPlanet.dimensionId)) continue;

            Matrix4f planetModelMatrix = new Matrix4f();

            Vec3 planetPosition = planet.getPosition((float)partialtick);

            // TODO: make this in real coordinates and setup custom new/far plane to support depth between planets
            Vec3 relativePos = planetPosition.subtract(myPlanetPosition).normalize().scale(200.0f);
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

            double planetRotationAngle = planet.getRotationAngle((float)partialtick);
            planetModelMatrix.rotate(new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 1, 0), (float) planetRotationAngle));

            double distance = myPlanetPosition.distanceTo(planetPosition);
            double scale = planet.size / distance * 10;
            planetModelMatrix.scale((float) scale);

            RenderSystem.setShader(shaderUtils::getPlanetShader);
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(planet.texture).setFilter(true, true);
            RenderSystem.setShaderTexture(0, planet.texture);
            ShaderInstance shader = RenderSystem.getShader();
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, planetModelMatrix, proj, Minecraft.getInstance().getWindow());

            // for now use the main star, later use the 3 or 4 brightest stars here
            DimensionProperties star = DimensionManager.INSTANCE.dimensions.get(planet.lightSourceDimensionId);
            if (star != null) {
                Vec3 Star0_Pos = star.getPosition((float)partialtick);
                Vec3 Light0_Vector = planetPosition.subtract(Star0_Pos).scale(-1); //shader uses planet to star for dot product

                shader.getUniform("Light0_Vector").set((float) Light0_Vector.x, (float) Light0_Vector.y, (float) Light0_Vector.z);
                Uniform light0Color = shader.getUniform("Light0_Color");
                if(light0Color != null)
                    light0Color.set(star.emissiveColor.x,star.emissiveColor.y,star.emissiveColor.z,star.emissiveColor.w);
            }

            Uniform reflectivity = shader.getUniform("reflectivity");
            if(reflectivity != null)
            reflectivity.set(planet.reflectivity);

            Uniform skyView = shader.getUniform("skyViewMat");
            if(skyView!=null)
            skyView.set(skyViewMatrix);

            Uniform emissiveColor = shader.getUniform("emissiveColor");
            if(emissiveColor != null)
            emissiveColor.set(planet.emissiveColor);

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

    public void renderSky(PoseStack poseStack, Matrix4f proj, Matrix4f view, double partialtick) {
        if (!finishedLoading) return;


        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);

        CelestialUtils.AxisDirections myGlobalAxis = CelestialUtils.getGlobalAxisDirections(myPlanet, (float) partialtick);

        // Create the base orientation for our skybox using the planet's axes.
        // This matrix transforms global space coordinates into our local tilted planet's reference frame.
        Matrix4f planetOrientationMatrix = new Matrix4f().lookAt(
                new Vector3f(0, 0, 0),
                myGlobalAxis.north.toVector3f(),
                myGlobalAxis.up.toVector3f()    // Up direction
        );

        Matrix4f skyViewMatrix =  new Matrix4f(view).mul(planetOrientationMatrix);


        // TODO maybe use this for planet rendering?
        float fovy = 2f * (float)Math.atan(1.0f / proj.get(1,1));
        float aspect = proj.get(1,1) / proj.get(0,0);
        Matrix4f newProj = new Matrix4f().perspective(fovy, aspect, 1, 100000);

        // adjust frame buffer size for render
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        if(planetRenderTarget.width != windowWidth * 2 ||planetRenderTarget.height != windowHeight * 2 ){
            if(windowWidth * windowHeight > 20000){ // small screen / minimized could cause crashes otherwise
                planetRenderTarget.resize(windowWidth*2, windowHeight*2, false);
            }
        }
        // Bind FBO for planet rendering
        this.planetRenderTarget.bindWrite(true);
        // Clear with transparent black
        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 0.0f);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        renderSpaceBodies(poseStack, proj, skyViewMatrix, partialtick); // use skyView because it is relative to universe 0,0,0

        if(atmosphereRenderTarget.width != windowWidth || atmosphereRenderTarget.height != windowHeight * 2 ){
            if(windowWidth * windowHeight > 20000){ // small screen / minimized could cause crashes otherwise
                atmosphereRenderTarget.resize(windowWidth, windowHeight, false);
            }
        }
        // Bind FBO for planet rendering
        this.atmosphereRenderTarget.bindWrite(true);
        // Clear with transparent black
        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 0.0f);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);
        renderSkyBox(proj, view); // use normal view in skybox because it is relative to player


        // Switch back to main render target
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);

        // Blit the planet texture onto the screen, no blending, overwrite whatever exists (nothing exists)
        this.atmosphereRenderTarget.blitToScreen(windowWidth, windowHeight, true);

        // Clear depth buffer for subsequent rendering
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);
    }
}
