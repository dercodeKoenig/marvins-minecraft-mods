package AOSWorkshopExpansion.WoodMill;

import AOSWorkshopExpansion.Main;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderWoodMill implements BlockEntityRenderer<EntityWoodMill> {

    static WavefrontObject model;
    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/saw_cutting_unit.png");


    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/woodmill.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    public RenderWoodMill(BlockEntityRendererProvider.Context c) {
        super();
    }

    public AABB getRenderBoundingBox(EntityWoodMill tile) {
        return new AABB(tile.getBlockPos()).inflate(1);
    }

    @Override
    public void render(EntityWoodMill tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.lastLight != packedLight) {
            tile.lastLight = packedLight;


            ByteBufferBuilder byteBuffer;
            BufferBuilder b;


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("saw").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_saw = b.build();
            tile.vertexBuffer_saw.bind();
            tile.vertexBuffer_saw.upload(tile.mesh_saw);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("arm").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_arm = b.build();
            tile.vertexBuffer_arm.bind();
            tile.vertexBuffer_arm.upload(tile.mesh_arm);
            byteBuffer.close();
        }

        if (!tile.getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) return;

        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof BlockWoodMill)) return;
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        Matrix4f m1 = new Matrix4f();
        m1 = m1.mul(stack.last().pose());
        m1 = m1.translate(0.5f, 0.5f, 0.5f);

        stack.translate(0.5f, 0.5f, 0.5f);
        if (facing == Direction.WEST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 0f));
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 0f));
        }
        if (facing == Direction.EAST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 180f));
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 180f));
        }
        if (facing == Direction.SOUTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 90f));
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 90f));
        }
        if (facing == Direction.NORTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 270f));
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 270f));
        }
        stack.translate(-0.5f, -0.5f, -0.5f);


        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        ShaderInstance shader = RenderSystem.getShader();
        RenderSystem.setShaderTexture(0, tex);

        Matrix4f m2 = new Matrix4f(m1);
        float crankshaftR = 0.112f;
        double targetX = 0;
        double armLength = 0.58;
        float XRotationMultiplier =
                (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1)
                        * (facing.getAxis() == Direction.Axis.X ? 1 : -1);


        double a = tile.myMechanicalBlock.currentRotation / 180 * Math.PI + tile.myMechanicalBlock.internalVelocity / TPS * partialTick;
        float translationX = (float) Math.sin(a) * crankshaftR * XRotationMultiplier;
        float translationY = -1f + (float) Math.cos(a) * crankshaftR;
        double b = Math.asin((translationX - targetX) / armLength);
        m2.translate(translationX, translationY, 0);
        m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, (float) b * 180f / (float) Math.PI));
        m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, 180f));


        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.getUniform("NormalMat").set(Static.getNormalMat(m2));
        shader.apply();
        tile.vertexBuffer_arm.bind();
        tile.vertexBuffer_arm.draw();

        m2 = new Matrix4f(m1);
        float sawTargetY = 0.4f + (float) (translationY + Math.cos(b) * armLength);
        m2.translate(0, sawTargetY, 0);

        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.getUniform("NormalMat").set(Static.getNormalMat(m2));
        shader.apply();
        tile.vertexBuffer_saw.bind();
        tile.vertexBuffer_saw.draw();


        for (EntityWoodMill.workingRecipe i : tile.currentWorkingRecipes) {
            if (i.currentInput.getItem() instanceof BlockItem bi) {
                BlockState s = bi.getBlock().defaultBlockState();
                stack.pushPose();

                stack.scale(0.3f, 0.3f, 0.3f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 0, 1f, 90f));
                stack.translate(0.9f, 1.4f, 1.16f);

                float partialOffset = Math.abs((float) (rad_to_degree(tile.myMechanicalBlock.internalVelocity) / 360f / TPS)) * WoodMillConfig.INSTANCE.speedMultiplier * partialTick;
                stack.translate(0f, -(i.progress + partialOffset) / tile.timeRequired * 1.6f / 0.3f, 0f);

                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(s, stack, bufferSource, packedLight, packedOverlay, ModelData.EMPTY, null);
                stack.translate(0, -1f, 0f);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(s, stack, bufferSource, packedLight, packedOverlay, ModelData.EMPTY, null);
                stack.translate(0, -1f, 0f);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(s, stack, bufferSource, packedLight, packedOverlay, ModelData.EMPTY, null);
                stack.popPose();
            }
        }


        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();
    }
}