package advRocketry.Rocket;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RendererRocket extends EntityRenderer<EntityRocket> {

    public RendererRocket(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRocket entityRocket) {
        return null;
    }

    public void makeRenderBuffer(EntityRocket rocket, int packedLight) {

        for (RenderType type : rocket.renderDataMap.keySet()) {
            RenderData data = rocket.renderDataMap.get(type);
            data.byteBufferBuilder = new ByteBufferBuilder(4096);
            data.bufferBuilder = new BufferBuilder(data.byteBufferBuilder, type.mode, type.format);
        }

        for (BlockPos pos : rocket.blocks.keySet()) {
            BlockState state = rocket.blocks.get(pos);
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            RandomSource random = RandomSource.create(42L);

            for (RenderType type : model.getRenderTypes(state, random, ModelData.EMPTY)) {

                RenderType entityRenderType = RenderTypeHelper.getEntityRenderType(type, false);

                if (!rocket.renderDataMap.containsKey(entityRenderType)) {
                    System.out.println("rendertype not present: " + entityRenderType.name);
                    continue;
                }

                RenderData renderData = rocket.renderDataMap.get(entityRenderType);

                int i = Minecraft.getInstance().getBlockColors().getColor(state, rocket.level(), null, 0);
                float r = (float) (i >> 16 & 255) / 255.0F;
                float g = (float) (i >> 8 & 255) / 255.0F;
                float b = (float) (i & 255) / 255.0F;

                PoseStack poseStack = new PoseStack();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), renderData.bufferBuilder, state, model, r, g, b, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, entityRenderType);
            }
        }

        for (RenderType type : rocket.renderDataMap.keySet()) {
            RenderData renderData = rocket.renderDataMap.get(type);
            renderData.mesh = renderData.bufferBuilder.build();
            if (renderData.mesh != null) {
                renderData.vertexBuffer.bind();
                renderData.vertexBuffer.upload(renderData.mesh);
            }
            renderData.byteBufferBuilder.close();
        }
    }


    @Override
    public void render(EntityRocket p_entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        poseStack.rotateAround(RotationUtils.getCurrentRotation(p_entity),
                0, (float) p_entity.size.getY() / 2, 0);


        poseStack.translate(-(float) p_entity.size.getX() / 2, 0, -(float) p_entity.size.getZ() / 2);

        if (p_entity.requiresMeshUpdate || p_entity.lastLight != packedLight) {
            if(packedLight == 0 && p_entity.lastLight != 0){
                // assume error, happens sometimes
            }else {
                p_entity.requiresMeshUpdate = false;
                p_entity.lastLight = packedLight;
                makeRenderBuffer(p_entity, packedLight);
            }
        }

        for (RenderType type : p_entity.renderDataMap.keySet()) {

            RenderData renderData = p_entity.renderDataMap.get(type);

            if (renderData.mesh == null) continue;

            //System.out.println(type.name);

            type.setupRenderState();
            renderData.vertexBuffer.bind();

            Matrix4f modelMatrix = poseStack.last().pose();
            Matrix4f viewMatrix = RenderSystem.getModelViewMatrix();
            Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
            Matrix4f modelViewMatrix = new Matrix4f(viewMatrix).mul(modelMatrix);

            ShaderInstance shader = RenderSystem.getShader();
            shader.setDefaultUniforms(type.mode, modelViewMatrix, projectionMatrix, Minecraft.getInstance().getWindow());
            Uniform NormalMat = shader.getUniform("NormalMat");
            if(NormalMat != null) {
                Matrix3f normalMat = new Matrix3f(modelMatrix); // take upper-left 3x3
                normalMat.invert().transpose(); // compute normal matrix
                NormalMat.set(normalMat);
            }else{
                throw new RuntimeException(type.name + " has no normal matrix!! Report this issue to Marvin at github or discord");
            }
            shader.apply();
            renderData.vertexBuffer.draw();

            NormalMat.set(new Matrix3f()); // reset because for whatever reason minecraft doesnt reload the defaults in future shader draw calls

            shader.clear();
            type.clearRenderState();
        }
        VertexBuffer.unbind();


        for (BlockPos p : p_entity.blockEntities.keySet()) {
            BlockEntity be = p_entity.blockEntities.get(p);
            poseStack.pushPose();
            poseStack.translate(p.getX(), p.getY(), p.getZ());
            BlockEntityRenderer<BlockEntity> blockentityrenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
            if (blockentityrenderer != null) {
                blockentityrenderer.render(be, 0, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
