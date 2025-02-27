package AgeOfSteam.Blocks.Mechanics.HandGenerator;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import AgeOfSteam.Main;
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
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderHandGenerator implements BlockEntityRenderer<EntityHandGenerator> {

    static WavefrontObject model;
    public ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/planks.png");

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/handcranked_generator.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }


    public RenderHandGenerator(BlockEntityRendererProvider.Context c) {
        super();
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

        Matrix4f m1 = new Matrix4f(RenderSystem.getModelViewMatrix());
        m1 = m1.mul(stack.last().pose());
        m1 = m1.translate(0.5f, 0.5f, 0.5f);

        double rotorRotationMultiplier = 1;
        if (facing == Direction.WEST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 90f));
        }
        if (facing == Direction.EAST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 270f));
            rotorRotationMultiplier = -1;
        }
        if (facing == Direction.SOUTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 180f));
            rotorRotationMultiplier = -1;
        }
        if (facing == Direction.NORTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 0f));
            //rotorRotationMultiplier = -1;
        }


        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();
        RenderSystem.setShaderTexture(0, tex);

        ShaderInstance shader = RenderSystem.getShader();

        Matrix4f m2 = new Matrix4f(m1);
        m2 = m2.translate(0.0f, 0.0f, -0.2f);
        m2 = m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1.0f, (float) (rotorRotationMultiplier * (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick))));
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.apply();

        tile.vertexBuffer2.bind();
        tile.vertexBuffer2.draw();

        m2 = new Matrix4f(m1);
        m2 = m2.translate(0.0f, 0.08f, 0.205f);
        m2 = m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1f, 0f, (float) (rotorRotationMultiplier * (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick))));
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
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