package AWGenerators.StirlingGenerator;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import AWGenerators.Main;
import AgeOfSteam.Static;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderStirlingGenerator implements BlockEntityRenderer<EntityStirlingGenerator> {

    static WavefrontObject model;
    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/stirling_generator.png");


    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/stirling_generator.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    public RenderStirlingGenerator(BlockEntityRendererProvider.Context c) {
        super();
    }

    @Override
    public void render(EntityStirlingGenerator tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.lastLight != packedLight) {
tile.lastLight = packedLight;

            ByteBufferBuilder byteBuffer;
            BufferBuilder b;


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("flywheel").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_flywheel = b.build();
            tile.vertexBuffer_flywheel.bind();
            tile.vertexBuffer_flywheel.upload(tile.mesh_flywheel);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("flywheel_arm").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_flywheel_arm = b.build();
            tile.vertexBuffer_flywheel_arm.bind();
            tile.vertexBuffer_flywheel_arm.upload(tile.mesh_flywheel_arm);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("piston_arm1").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_piston_arm1 = b.build();
            tile.vertexBuffer_piston_arm1.bind();
            tile.vertexBuffer_piston_arm1.upload(tile.mesh_piston_arm1);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("piston_arm2").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_piston_arm2 = b.build();
            tile.vertexBuffer_piston_arm2.bind();
            tile.vertexBuffer_piston_arm2.upload(tile.mesh_piston_arm2);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("piston_crank1").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_piston_crank1 = b.build();
            tile.vertexBuffer_piston_crank1.bind();
            tile.vertexBuffer_piston_crank1.upload(tile.mesh_piston_crank1);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("piston_crank2").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_piston_crank2 = b.build();
            tile.vertexBuffer_piston_crank2.bind();
            tile.vertexBuffer_piston_crank2.upload(tile.mesh_piston_crank2);
            byteBuffer.close();
        }

        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof BlockStirlingGenerator)) return;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        stack.translate(0.5f, 0.5f, 0.5f);
        float rotationMultiplier = 0;

        if (facing == Direction.WEST) {
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 90f));
            rotationMultiplier = 1;
        }
        if (facing == Direction.EAST) {
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 270f));
            rotationMultiplier = -1;
        }
        if (facing == Direction.SOUTH) {
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 180f));
            rotationMultiplier = -1;
        }
        if (facing == Direction.NORTH) {
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 0f));
            rotationMultiplier = 1;
        }
        stack.translate(-0.5f, -0.5f, -0.5f);

        Matrix4f m1 = new Matrix4f();
        m1 = m1.mul(stack.last().pose());

        float rotation = (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick);

        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        ShaderInstance shader = RenderSystem.getShader();
        RenderSystem.setShaderTexture(0, tex);

        Matrix4f m2 = new Matrix4f(m1);
        m2 = m2.rotateAround(new Quaternionf().fromAxisAngleDeg(0f, 0f, rotationMultiplier, rotation), 0.5f, 0.5f, 0f);
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES,new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.getUniform("NormalMat").set(Static.getNormalMat(m2));
        shader.apply();
        tile.vertexBuffer_flywheel.bind();
        tile.vertexBuffer_flywheel.draw();

        m2 = new Matrix4f(m1);
        m2 = m2.rotateAround(new Quaternionf().fromAxisAngleDeg(0f, 0f, rotationMultiplier, -rotation), 0.75f, 0.75f, 0f);
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.getUniform("NormalMat").set(Static.getNormalMat(m2));
        shader.apply();
        tile.vertexBuffer_piston_crank1.bind();
        tile.vertexBuffer_piston_crank1.draw();

        m2 = new Matrix4f(m1);
        m2 = m2.rotateAround(new Quaternionf().fromAxisAngleDeg(0f, 0f, rotationMultiplier, rotation), 0.75f, 0.6875f, 0f);
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.getUniform("NormalMat").set(Static.getNormalMat(m2));
        shader.apply();
        tile.vertexBuffer_piston_crank2.bind();
        tile.vertexBuffer_piston_crank2.draw();


        m2 = new Matrix4f(m1);
        float r_arm = 1f / 16f;
        float tx = (float) (Math.cos((rotation * rotationMultiplier) / 180f * Math.PI) * r_arm);
        float ty = (float) (Math.sin((rotation * rotationMultiplier) / 180f * Math.PI) * r_arm);
        m2 = m2.translate(tx, ty, 0);
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.getUniform("NormalMat").set(Static.getNormalMat(m2));
        shader.apply();
        tile.vertexBuffer_flywheel_arm.bind();
        tile.vertexBuffer_flywheel_arm.draw();


        m2 = new Matrix4f(m1);
        tx = (float) (Math.cos((-rotation * rotationMultiplier) / 180f * Math.PI) * r_arm);
        ty = (float) (Math.sin((-rotation * rotationMultiplier) / 180f * Math.PI) * r_arm);
        m2 = m2.translate(tx, ty, 0);
        float c1ArmLen = 0.7f;
        float c1Rotation = (float) (Math.asin(ty / c1ArmLen) * 180 / Math.PI);
        m2 = m2.rotateAround(new Quaternionf().fromAxisAngleDeg(0, 0, 1f, c1Rotation), 0.75f, 0.75f + ty, 0f);
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.getUniform("NormalMat").set(Static.getNormalMat(m2));
        shader.apply();
        tile.vertexBuffer_piston_arm1.bind();
        tile.vertexBuffer_piston_arm1.draw();


        m2 = new Matrix4f(m1);
        m2 = m2.translate(tx, ty, 0);
        float c2ArmLen = 0.7f;
        float c2Rotation = (float) -(Math.asin(tx / c2ArmLen) * 180 / Math.PI);
        m2 = m2.rotateAround(new Quaternionf().fromAxisAngleDeg(0, 0, 1f, c2Rotation), 0.75f, 0.75f + ty, 0f);
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.getUniform("NormalMat").set(Static.getNormalMat(m2));
        shader.apply();
        tile.vertexBuffer_piston_arm2.bind();
        tile.vertexBuffer_piston_arm2.draw();

        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();

        if (tile.currentBurnTime > 0) {
            stack.scale(3f / 16, 3f / 16, 3f / 16);
            stack.translate(2f / 3, 1f, 10.5 / 3f);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.FIRE.defaultBlockState(), stack, bufferSource, packedLight, packedOverlay, ModelData.EMPTY, null);
        }
    }
}