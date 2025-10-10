package advRocketry.Rocket;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
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

            if (state.getBlock() instanceof EntityBlock eb) {
                BlockEntity e = eb.newBlockEntity(p_entity.blockPosition(), state);
                e.setLevel(p_entity.level());
                Minecraft.getInstance().getBlockEntityRenderDispatcher().render(e, partialTick, poseStack, bufferSource);
            } else
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, packedLight, 0, ModelData.EMPTY, r);

            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
