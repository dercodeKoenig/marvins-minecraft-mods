package ARMachines.lathe;


import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.ModelFormatException;
import ARLib.obj.Static;
import ARLib.obj.WavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RenderLathe implements BlockEntityRenderer<EntityLathe> {

    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("armachines", "textures/block/lathe.png");
    static WavefrontObject model;

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath("armachines", "multiblock/lathe.obj"));
        } catch (ModelFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public int getViewDistance() {
        return 256;
    }

    @NotNull
    @Override
    public AABB getRenderBoundingBox(EntityLathe blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(100);
    }


    public RenderLathe(BlockEntityRendererProvider.Context context) {

    }

    @Override
    public void render(EntityLathe tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {

            int progress = tile.client_recipeProgress;
            int maxTime = tile.client_recipeMaxTime;
            double maxTime_I = (double) 1 / maxTime;
            double partial_add = 0;
            if (tile.isRunning)
                partial_add = partialTick * maxTime_I;
            double relativeProgress = progress * maxTime_I + partial_add;


            // Get the facing direction of the block
            Direction facing = tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            float angle = 0;
            switch (facing) {
                case NORTH:
                    angle = 270;
                    break;
                case EAST:
                    angle = 180;
                    break;
                case SOUTH:
                    angle = 90;
                    break;
                case WEST:
                    angle = 0;
                    break;
            }


            Vector3f Yaxis = new Vector3f(0, 1, 0);
/*
            // Hull is not rendered. Kept for reference using the modern PoseStack API:
            stack.pushPose();
            stack.translate(0.5f, 0, 0.5f);
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis, angle));
            stack.translate(-0.5f, 0, -0.5f);
            VertexConsumer vc = bufferSource.getBuffer(RenderType.entitySolid(tex));
            model.renderPart("Hull", stack, vc, packedLight, packedOverlay);
            stack.popPose();
*/

            double maxTranslate = -1.12;
            double translation = relativeProgress;
            if (translation > 0.5) translation = 1 - translation;


            VertexConsumer vc = bufferSource.getBuffer(Static.ENTITY_SOLID_TRIANGLES.apply(tex));

            stack.pushPose();
            stack.translate(0.5f, 0, 0.5f);
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis, angle));
            stack.translate(-0.5f, 0, -0.5f);
            stack.translate(0.935f, -0.319f, 1.51f - (float) maxTranslate * (float) translation * 2f);
            model.renderPart("Tool", stack, vc, packedLight, packedOverlay);
            stack.popPose();


            if (tile.client_hasRecipe) {
                stack.pushPose();
                stack.translate(0.5f, 0, 0.5f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis, angle));
                stack.translate(-0.5f, 0, -0.5f);
                stack.translate(0.62f, 0.18f, 1.50471f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(new Vector3f(0, 0, 1), (float) relativeProgress * 360f * 10));
                model.renderPart("Shaft", stack, vc, packedLight, packedOverlay);
                stack.popPose();
            }
        }
    }
}
