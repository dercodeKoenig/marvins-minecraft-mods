package advRocketry.Rocket;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class RendererRocket extends EntityRenderer<EntityRocket> {

    RenderType r = RenderType.create(
            "rocket",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            4194304,
            true,
            false,
            RenderType.CompositeState.builder()
                    .setLightmapState(LIGHTMAP)
                    .setShaderState(RENDERTYPE_SOLID_SHADER)
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
        for (BlockPos p : p_entity.blocks.keySet()) {
            BlockState state = p_entity.blocks.get(p);
            poseStack.pushPose();
            poseStack.translate(p.getX(), p.getY(), p.getZ());
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, packedLight, 0, ModelData.EMPTY, r);
            poseStack.popPose();
        }
    }
}
