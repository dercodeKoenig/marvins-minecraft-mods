package ARLib.multiblockCore;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class RenderMultiblockPlaceholder implements BlockEntityRenderer<EntityMultiblockPlaceholder> {
    public RenderMultiblockPlaceholder(BlockEntityRendererProvider.Context context) {

    }
    @Override
    public void render(EntityMultiblockPlaceholder tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if(tile.renderBlock){
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    tile.replacedState,
                    stack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
                    null
            );
        }
    }
}

