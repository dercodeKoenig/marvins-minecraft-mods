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
    public static void computeFogColor( ViewportEvent.ComputeFogColor event) {
        Vector3f color =  DimensionManager.INSTANCE.dimensions.get(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld")).getFogColor();

        //System.out.println(event.getRed()+"+"+event.getGreen()+":"+event.getBlue());

        event.setBlue(color.x);
        event.setGreen(color.y);
        event.setRed(color.z);

        //System.out.println("post:"+event.getRed()+"+"+event.getGreen()+":"+event.getBlue());
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

    public void setupRenderTarget() {
        this.planetRenderTarget = new TextureTarget(1000, 1000, true, Minecraft.ON_OSX);
    }

    public void renderSkyBox(Matrix4f proj, Matrix4f view){
        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);

        NO_CULL.setupRenderState();

        // Render skybox first
        Vector3f atmColor = myPlanet.getAtmosphereColor();
        RenderSystem.setShader(shaderUtils::getAtmosphereShader);
        ShaderInstance shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, view, proj, Minecraft.getInstance().getWindow());
        Uniform color = shader.getUniform("Color");
        color.set(atmColor.x, atmColor.y, atmColor.z, 1f);

        shader.apply();
        vertexBufferSkyBox.bind();
        vertexBufferSkyBox.draw();
        shader.clear();
        VertexBuffer.unbind();

        NO_CULL.clearRenderState();
    }

    public void renderSpaceBodies(PoseStack poseStack, Matrix4f proj, Matrix4f view, double partialtick){

        // adjust frame buffer size for render
        int windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

        if(planetRenderTarget.width != windowWidth * 2 ||planetRenderTarget.height != windowHeight * 2 ){
            if(windowWidth * windowHeight > 20000){ // small screen / minimized could cause crashes otherwise
                planetRenderTarget.resize(windowWidth*2, windowHeight*2, true);
                System.out.println("planet render framebuffer resized to " + planetRenderTarget.width+":"+planetRenderTarget.height);
            }
        }
        // adjust frame buffer size for render end



        ResourceLocation myId = Minecraft.getInstance().level.dimension().location();
        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);
        Vec3 myPlanetPosition = myPlanet.getPosition((float)partialtick);

        CelestialUtils.AxisDirections myGlobalAxis = CelestialUtils.getGlobalAxisDirections(myPlanet, (float) partialtick);

        // Create the base orientation for our skybox using the planet's axes.
        // This matrix transforms world coordinates into our tilted planet's reference frame.
        Matrix4f planetOrientationMatrix = new Matrix4f().lookAt(
                new Vector3f(0, 0, 0),
                myGlobalAxis.east.toVector3f(), // Look direction
                myGlobalAxis.up.toVector3f()    // Up direction
        );
        // You might also want to try looking at myGlobalAxis.north instead of east, depending on your desired base orientation.

        Matrix4f finalSkyViewMatrix =  new Matrix4f(view).mul(planetOrientationMatrix);
        //System.out.println("planetOrientation: "+planetOrientationMatrix);
        //System.out.println("view: "+view);
        //System.out.println("final: "+finalSkyViewMatrix);


        //System.out.println(myGlobalAxis.up);

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

            Vec3 planetPosition = planet.getPosition((float)partialtick);

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

            Matrix4f modelViewMatrix = new Matrix4f(finalSkyViewMatrix).mul(planetModelMatrix);


            RenderSystem.setShader(shaderUtils::getPlanetShader);
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(planet.texture).setFilter(true, true);
            RenderSystem.setShaderTexture(0, planet.texture);
            ShaderInstance shader = RenderSystem.getShader();
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, modelViewMatrix, proj, Minecraft.getInstance().getWindow());

            DimensionProperties star = DimensionManager.INSTANCE.dimensions.get(planet.lightSourceDimensionId);
            if (star != null) {
                Vec3 lightWorld = star.getPosition((float)partialtick).subtract(planetPosition);
                Matrix4f finalViewMatrix = new Matrix4f(finalSkyViewMatrix);
                Vector4f lightView4 = new Vector4f((float) lightWorld.x, (float) lightWorld.y, (float) lightWorld.z, 0.0f);
                finalViewMatrix.transform(lightView4);
                Vec3 lightView = new Vec3(lightView4.x(), lightView4.y(), lightView4.z()).normalize();
                if (shader.LIGHT0_DIRECTION != null) {
                    shader.LIGHT0_DIRECTION.set((float) lightView.x, (float) lightView.y, (float) lightView.z);
                }
            }

            Uniform reflectivity = shader.getUniform("reflectivity");
            reflectivity.set(planet.reflectivity);

            Uniform emissiveColor = shader.getUniform("emissiveColor");
            emissiveColor.set(planet.emissiveColor);

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
    }

    public void renderSky(PoseStack poseStack, Matrix4f proj, Matrix4f view, double partialtick) {
        if (!finishedLoading) return;

        renderSkyBox(proj, view);

        renderSpaceBodies(poseStack, proj, view, partialtick);


        // Clear depth buffer for subsequent rendering
        RenderSystem.clear(GL30.GL_DEPTH_BUFFER_BIT, false);
    }
}
