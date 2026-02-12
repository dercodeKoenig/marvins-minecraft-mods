package advRocketry.BlockEntityRenderers;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import advRocketry.BlockEntities.EntityObservatory;
import advRocketry.Blocks.Observatory;
import advRocketry.Main;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

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

    public RenderObservatory(BlockEntityRendererProvider.Context c) {super();}


    public void updateVertexBuffers(EntityObservatory tile, int light) {
        ByteBufferBuilder byteBuffer;
        BufferBuilder b;

        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("Axis").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.meshAxle = b.build();
        tile.axle.bind();
        tile.axle.upload(tile.meshAxle);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("Scope").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.meshScope = b.build();
        tile.scope.bind();
        tile.scope.upload(tile.meshScope);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("CasingXPlus").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.meshCasingXPlus = b.build();
        tile.casingXPlus.bind();
        tile.casingXPlus.upload(tile.meshCasingXPlus);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("CasingXMinus").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.meshCasingXMinus = b.build();
        tile.casingXMinus.bind();
        tile.casingXMinus.upload(tile.meshCasingXMinus);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("Base").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.meshBase = b.build();
        tile.base.bind();
        tile.base.upload(tile.meshBase);
        byteBuffer.close();
    }

    @Override
    public void render(EntityObservatory observatory, float partialtick, PoseStack stack, MultiBufferSource multiBufferSource, int light, int overlay) {

        if (observatory.lastLight != light) {
            observatory.lastLight = light;
            updateVertexBuffers(observatory,light);
        }

        BlockState state = observatory.getBlockState();
        if (!(state.getBlock() instanceof Observatory)) return;

        if(!state.getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED))
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
        modelMat.translate(back.getStepX()*2+0.5f,0,back.getStepZ()*2+0.5f);

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

        observatory.base.bind();
        observatory.base.draw();

        // rotate
        //modelMat.rotate(new Quaternionf().fromAxisAngleDeg(0,1,0,(float)(System.currentTimeMillis() % 360000) / 300f));

        Matrix4f modelMatScope = new Matrix4f(modelMat);
        modelMatScope.translate(0,2,0);

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMatScope), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMatScope); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        observatory.scope.bind();
        observatory.scope.draw();

        observatory.axle.bind();
        observatory.axle.draw();


        Matrix4f modelMatCaseXPlus = new Matrix4f(modelMat);
        // open
        modelMatCaseXPlus.translate(0f,0,1f);

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMatCaseXPlus), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMatCaseXPlus); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        observatory.casingXPlus.bind();
        observatory.casingXPlus.draw();


        Matrix4f modelMatCaseXMinus = new Matrix4f(modelMat);
        // open
        modelMatCaseXMinus.translate(0f,0,-1f);

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMatCaseXMinus), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMatCaseXMinus); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        observatory.casingXMinus.bind();
        observatory.casingXMinus.draw();

        // i think i need to reset it or there might be problems. dont know exactly why but minecraft doesnt know / care about normal mat
        NormalMat.set(new Matrix3f());
        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();

    }
}
