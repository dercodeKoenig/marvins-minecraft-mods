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
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;

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


        DimensionProperties myPlanet = DimensionManager.INSTANCE.dimensions.get(myId);


        for (DimensionProperties planet : DimensionManager.INSTANCE.dimensions.values()) {

            if (planet.dimensionId.equals(myPlanet.dimensionId))continue;

            Matrix4f m1 = new Matrix4f(view);
            Vec3 direction = CelestialUtils.getBodyDirectionLocal(myPlanet.position, planet.position, myPlanet.rotationAxis,myPlanet.selfRotationDegrees,50).scale(50);
            m1.translate((float)direction.x,(float)direction.y,(float)direction.z);

            double scale = planet.size / myPlanet.position.distanceTo(planet.position);
            m1.scale((float) scale);

            RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);

            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(planet.texture).setFilter(true, true);
            RenderSystem.setShaderTexture(0, planet.texture);

            shader = RenderSystem.getShader();
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m1, proj, Minecraft.getInstance().getWindow());
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
