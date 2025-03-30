package ARMachines.rollingMachine;


import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static ARLib.ARLibRegistry.BLOCK_FLUID_INPUT_BLOCK;
import static ARLib.obj.GroupObject.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderRollingMachine implements BlockEntityRenderer<EntityRollingMachine> {

    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("armachines", "textures/block/rollingmachine.png");
    static WavefrontObject model;
    static ResourceLocation modelsrc = ResourceLocation.fromNamespaceAndPath("armachines", "multiblock/rollingmachine_new.obj");
    static{
        try {
            model = new WavefrontObject(modelsrc);
        } catch (ModelFormatException e) {
            throw new RuntimeException(e);
        }
    }

    static VertexFormat vertexFormat = POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
    static RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
            .setOverlayState(OVERLAY)
            .setLightmapState(LIGHTMAP)
            .setTransparencyState(NO_TRANSPARENCY)
            .setTextureState(new TextureStateShard(tex, false, false))
            .createCompositeState(false);


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

                model.resetTransformations("Roller2");
                model.translateWorldSpace("Roller2",new Vector3f(0.5f,0,0.5f));
                model.rotateWorldSpace("Roller2",Yaxis,angle);
                model.translateWorldSpace("Roller2",new Vector3f(-0.5f,0,-0.5f));
                model.translateWorldSpace("Roller2",new Vector3f(2.13552f,0.375729f-1,2.17779f));
                model.rotateWorldSpace("Roller2",a,(float) relativeProgress * 360 * 1f);
                model.applyTransformations("Roller2");
                model.renderPart("Roller2", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);

                model.resetTransformations("Roller1");
                model.translateWorldSpace("Roller1",new Vector3f(0.5f,0,0.5f));
                model.rotateWorldSpace("Roller1",Yaxis,angle);
                model.translateWorldSpace("Roller1",new Vector3f(-0.5f,0,-0.5f));
                model.translateWorldSpace("Roller1",new Vector3f(2.13208f,1.00678f-1,2.5557f));
                model.rotateWorldSpace("Roller1",a,-(float) relativeProgress * 360 * 1f);
                model.applyTransformations("Roller1");
                model.renderPart("Roller1", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);

                model.resetTransformations("Roller3");
                model.translateWorldSpace("Roller3",new Vector3f(0.5f,0,0.5f));
                model.rotateWorldSpace("Roller3",Yaxis,angle);
                model.translateWorldSpace("Roller3",new Vector3f(-0.5f,0,-0.5f));
                model.translateWorldSpace("Roller3",new Vector3f(2.13552f,0.375729f-1,2.93412f));
                model.rotateWorldSpace("Roller3",a,(float) relativeProgress * 360 * 1f);
                model.applyTransformations("Roller3");
                model.renderPart("Roller3", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);

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

