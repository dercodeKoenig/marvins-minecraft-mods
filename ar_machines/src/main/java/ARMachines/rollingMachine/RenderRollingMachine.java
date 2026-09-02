package ARMachines.rollingMachine;


import ARLib.ARLib;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.ModelFormatException;
import ARLib.obj.Static;
import ARLib.obj.WavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RenderRollingMachine implements BlockEntityRenderer<EntityRollingMachine> {

    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("armachines", "textures/block/rollingmachine.png");
    static WavefrontObject model;
    static ResourceLocation modelsrc = ResourceLocation.fromNamespaceAndPath("armachines", "multiblock/rollingmachine.obj");
    static{
        try {
            model = new WavefrontObject(modelsrc);
        } catch (ModelFormatException e) {
            throw new RuntimeException(e);
        }
    }


    public int getViewDistance() {
        return 256;
    }

    @NotNull
    @Override
    public AABB getRenderBoundingBox(EntityRollingMachine blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(100);
    }


    public RenderRollingMachine(BlockEntityRendererProvider.Context context) {

    }


    @Override
    public void render(EntityRollingMachine tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
            {

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
                model.resetTransformations("Hull");
                model.translateWorldSpace("Hull",new Vector3f(0.5f,0,0.5f));
                model.rotateWorldSpace("Hull",Yaxis,angle);
                model.translateWorldSpace("Hull",new Vector3f(-0.5f,0,-0.5f));
                model.applyTransformations("Hull");
                model.renderPart("Hull", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);
*/


                Vector3f a = new Vector3f(1, 0, 0);

                VertexConsumer vc = bufferSource.getBuffer(Static.ENTITY_SOLID_TRIANGLES.apply(tex));

                stack.pushPose();
                stack.translate(0.5f,0,0.5f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis,angle));
                stack.translate(-0.5f,0,-0.5f);
                stack.translate(2.13552f,0.375729f-1,2.17779f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(a,(float) relativeProgress * 360 * 1f));
                model.renderPart("Roller2", stack, vc, packedLight, packedOverlay);
                stack.popPose();

                stack.pushPose();
                stack.translate(0.5f,0,0.5f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis,angle));
                stack.translate(-0.5f,0,-0.5f);
                stack.translate(2.13208f,1.00678f-1,2.5557f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(a,-(float) relativeProgress * 360 * 1f));
                model.renderPart("Roller1", stack, vc, packedLight, packedOverlay);
                stack.popPose();

                stack.pushPose();
                stack.translate(0.5f,0,0.5f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis,angle));
                stack.translate(-0.5f,0,-0.5f);
                stack.translate(2.13552f,0.375729f-1,2.93412f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(a,(float) relativeProgress * 360 * 1f));
                model.renderPart("Roller3", stack, vc, packedLight, packedOverlay);
                stack.popPose();

                if(tile.client_hasRecipe){
                    stack.translate(0.5f, 0, 0.5f);
                    stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis,angle));

                    double maxTranslate = 2.2;
                    double stackTranslate = relativeProgress * maxTranslate;
                    double offsetX = 1.6;
                    double offsetY = -0.2;
                    double offsetZ = 0.5;
                    if(stackTranslate < 1.5) {
                        for (int i = 0; i < tile.client_nextConsumedStacks.itemStacks.size(); i++) {
                            stack.pushPose();
                            stack.translate(offsetX, offsetY+i*0.001, offsetZ-i*0.05 + stackTranslate);
                            stack.mulPose(new Quaternionf().fromAxisAngleDeg(new Vector3f(1, 0, 0), 90));

                            ItemStack currStack = tile.client_nextConsumedStacks.itemStacks.get(i);
                            Minecraft.getInstance().getItemRenderer().renderStatic(currStack, ItemDisplayContext.GROUND, packedLight, packedOverlay, stack, bufferSource, tile.getLevel(), 0);
                            stack.popPose();
                        }
                    }else{
                        for (int i = 0; i < tile.client_nextProducedStacks.itemStacks.size(); i++) {
                            stack.pushPose();
                            stack.translate(offsetX, offsetY+i*0.001, offsetZ-i*0.05 + stackTranslate);
                            stack.mulPose(new Quaternionf().fromAxisAngleDeg(new Vector3f(1, 0, 0), 90));

                            ItemStack currStack = tile.client_nextProducedStacks.itemStacks.get(i);
                            Minecraft.getInstance().getItemRenderer().renderStatic(currStack, ItemDisplayContext.GROUND, packedLight, packedOverlay, stack, bufferSource, tile.getLevel(), 0);
                            stack.popPose();
                        }
                    }
                }
            }
        }
    }
}
