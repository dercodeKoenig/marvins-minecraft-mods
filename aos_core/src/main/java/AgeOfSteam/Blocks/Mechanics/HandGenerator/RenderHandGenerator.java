package AgeOfSteam.Blocks.Mechanics.HandGenerator;

import ARLib.mixins.ShaderInstanceMixin;
import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import AgeOfSteam.Main;
import AgeOfSteam.Static;
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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderHandGenerator implements BlockEntityRenderer<EntityHandGenerator> {

    WavefrontObject model;
    public ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/planks.png");

    public RenderHandGenerator(BlockEntityRendererProvider.Context c) {
        super();
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/handcranked_generator.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void render(EntityHandGenerator tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.lastLight != packedLight) {
            tile.lastLight = packedLight;

            ByteBufferBuilder byteBuffer;
            BufferBuilder b;

            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("fly_wheel").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh2 = b.build();
            tile.vertexBuffer2.bind();
            tile.vertexBuffer2.upload(tile.mesh2);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("hand_wheel").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh = b.build();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.upload(tile.mesh);
            byteBuffer.close();
        }

        BlockState axleState = tile.getBlockState();
        if (!(axleState.getBlock() instanceof BlockHandGenerator)) return;
        Direction facing = axleState.getValue(BlockHandGenerator.FACING);

        Matrix4f modelMat = new Matrix4f();
        modelMat = modelMat.mul(stack.last().pose());
        modelMat = modelMat.translate(0.5f, 0.5f, 0.5f);

        double rotorRotationMultiplier = 1;
        if (facing == Direction.WEST) {
            modelMat = modelMat.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 90f));
        }
        if (facing == Direction.EAST) {
            modelMat = modelMat.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 270f));
            rotorRotationMultiplier = -1;
        }
        if (facing == Direction.SOUTH) {
            modelMat = modelMat.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 180f));
            rotorRotationMultiplier = -1;
        }
        if (facing == Direction.NORTH) {
            modelMat = modelMat.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 0f));
            //rotorRotationMultiplier = -1;
        }


        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();
        RenderSystem.setShaderTexture(0, tex);

        ShaderInstance shader = RenderSystem.getShader();

        Matrix4f modelMat2 = new Matrix4f(modelMat);
        modelMat2 = modelMat2.translate(0.0f, 0.0f, -0.2f);
        modelMat2 = modelMat2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1.0f, (float) (rotorRotationMultiplier * (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick))));
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul( modelMat2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        Uniform NormalMat = shader.getUniform("NormalMat");
        NormalMat.set(Static.getNormalMat(modelMat2));
        shader.apply();

        tile.vertexBuffer2.bind();
        tile.vertexBuffer2.draw();

        modelMat2 = new Matrix4f(modelMat);
        modelMat2 = modelMat2.translate(0.0f, 0.08f, 0.205f);
        modelMat2 = modelMat2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1f, 0f, (float) (rotorRotationMultiplier * (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick))));
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul( modelMat2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        NormalMat.set(Static.getNormalMat(modelMat2));
        shader.apply();

        tile.vertexBuffer.bind();
        tile.vertexBuffer.draw();


        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();
    }
}