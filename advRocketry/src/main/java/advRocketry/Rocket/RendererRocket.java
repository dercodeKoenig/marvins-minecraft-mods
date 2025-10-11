package advRocketry.Rocket;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class RendererRocket extends EntityRenderer<EntityRocket> {

    RenderType r = RenderType.create("rocket", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 4096, RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .createCompositeState(true));

    public RendererRocket(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRocket entityRocket) {
        return null;
    }

    @Override
    public void render(EntityRocket p_entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(-(float) p_entity.size.getX() / 2, 0, -(float) p_entity.size.getZ() / 2);
        for (BlockPos p : p_entity.blocks.keySet()) {
            BlockState state = p_entity.blocks.get(p);
            poseStack.pushPose();
            poseStack.translate(p.getX(), p.getY(), p.getZ());

            RenderShape rendershape = state.getRenderShape();
            if (rendershape == RenderShape.MODEL) {
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, r);
            }
            poseStack.popPose();
        }
        for (BlockPos p : p_entity.blockEntities.keySet()) {
            BlockEntity be = p_entity.blockEntities.get(p);
            poseStack.pushPose();
            poseStack.translate(p.getX(), p.getY(), p.getZ());
            BlockEntityRenderer<BlockEntity> blockentityrenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
            if (blockentityrenderer != null) {
                blockentityrenderer.render(be,0,poseStack,bufferSource,packedLight, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
