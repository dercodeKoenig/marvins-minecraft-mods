package advRocketry.BlockEntityRenderers;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import advRocketry.BlockEntities.EntityObservatory;
import advRocketry.Blocks.Observatory;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Main;
import advRocketry.utils.AxisDirections;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

import static net.minecraft.client.renderer.RenderStateShard.*;
import static net.minecraft.client.renderer.RenderStateShard.NO_TRANSPARENCY;

public class RenderObservatory implements BlockEntityRenderer<EntityObservatory> {


    static WavefrontObject model;
    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/observatory.png");

    static VertexFormat POSITION_COLOR_TEXTURE_NORMAL_LIGHT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("Color", VertexFormatElement.COLOR).add("UV0", VertexFormatElement.UV0).add("UV1", VertexFormatElement.UV1).add("UV2", VertexFormatElement.UV2).add("Normal", VertexFormatElement.NORMAL).build();

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/block/obj/observatory.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    public RenderObservatory(BlockEntityRendererProvider.Context c) {
        super();
    }


    public void updateVertexBuffers(EntityObservatory.RenderData renderData, int light) {
        ByteBufferBuilder byteBuffer;
        BufferBuilder b;

        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("Axis").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        renderData.meshAxle = b.build();
        renderData.axle.bind();
        renderData.axle.upload(renderData.meshAxle);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("Scope").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        renderData.meshScope = b.build();
        renderData.scope.bind();
        renderData.scope.upload(renderData.meshScope);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("CasingXPlus").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        renderData.meshCasingXPlus = b.build();
        renderData.casingXPlus.bind();
        renderData.casingXPlus.upload(renderData.meshCasingXPlus);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("CasingXMinus").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        renderData.meshCasingXMinus = b.build();
        renderData.casingXMinus.bind();
        renderData.casingXMinus.upload(renderData.meshCasingXMinus);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("Base").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        renderData.meshBase = b.build();
        renderData.base.bind();
        renderData.base.upload(renderData.meshBase);
        byteBuffer.close();
    }


    @Override
    public void render(EntityObservatory observatory, float partialtick, PoseStack stack, MultiBufferSource multiBufferSource, int light, int overlay) {
        EntityObservatory.RenderData renderData = observatory.renderData;

        if (renderData.lastLight != light) {
            renderData.lastLight = light;
            updateVertexBuffers(renderData, light);
        }

        BlockState state = observatory.getBlockState();
        if (!(state.getBlock() instanceof Observatory)) return;

        if (!state.getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED))
            return;

        Direction back = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();
        RenderSystem.setShaderTexture(0, tex);

        Matrix4f modelMat = new Matrix4f();
        modelMat = modelMat.mul(stack.last().pose());

        // translate to structure center
        modelMat.translate(back.getStepX() * 2 + 0.5f, 0, back.getStepZ() * 2 + 0.5f);

        ShaderInstance shader;
        Uniform NormalMat;
        Matrix3f normalMat;

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMat), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMat); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        renderData.base.bind();
        renderData.base.draw();

        float yaw = renderData.yaw - (1-partialtick) * renderData.yawD;
        float pitch = renderData.pitch - (1-partialtick) * renderData.pitchD;
        yaw = yaw * (float)Math.PI / 180;
        pitch = pitch * (float)Math.PI / 180;

        Matrix4f modelMatScope = new Matrix4f(modelMat);
        modelMatScope.translate(0, 2, 0);
        modelMatScope.rotateY(yaw);       // Spin the axle around Y
        modelMatScope.rotateZ(-pitch);       // Spin the axle around Y

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMatScope), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMatScope); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        renderData.scope.bind();
        renderData.scope.draw();

        renderData.axle.bind();
        renderData.axle.draw();


        float openProgress = (float) renderData.openingTicks / renderData.openingTicksMax;
        if (renderData.should_open) openProgress += partialtick / renderData.openingTicksMax;
        if (!renderData.should_open) openProgress -= partialtick / renderData.openingTicksMax;
        openProgress = Math.clamp(openProgress, 0, 1);


        Matrix4f modelMatCaseXPlus = new Matrix4f(modelMat);
        modelMatCaseXPlus.rotateY(yaw);

        // open
        modelMatCaseXPlus.translate(0f, 0, openProgress);

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMatCaseXPlus), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMatCaseXPlus); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        renderData.casingXPlus.bind();
        renderData.casingXPlus.draw();


        Matrix4f modelMatCaseXMinus = new Matrix4f(modelMat);
        modelMatCaseXMinus.rotateY(yaw);

        // open
        modelMatCaseXMinus.translate(0f, 0, -openProgress);

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMatCaseXMinus), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMatCaseXMinus); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        renderData.casingXMinus.bind();
        renderData.casingXMinus.draw();

        // i think i need to reset it or there might be problems. dont know exactly why but minecraft doesnt know / care about normal mat
        NormalMat.set(new Matrix3f());
        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();

    }
}
