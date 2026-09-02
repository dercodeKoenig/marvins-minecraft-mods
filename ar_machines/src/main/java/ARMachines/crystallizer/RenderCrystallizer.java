package ARMachines.crystallizer;


import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;


public class RenderCrystallizer implements BlockEntityRenderer<EntityCrystallizer> {

    //static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("armachines", "textures/block/crystallizer.png");
    static ResourceLocation modelsrc = ResourceLocation.fromNamespaceAndPath("armachines", "multiblock/crystallizer.obj");
    static WavefrontObject model;
    static {
        try {
            model = new WavefrontObject(modelsrc);
        } catch (
                ModelFormatException e) {
            throw new RuntimeException(e);
        }
    }


    public int getViewDistance() {
        return 256;
    }

    @NotNull
    @Override
    public AABB getRenderBoundingBox(EntityCrystallizer blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(100);
    }


    public RenderCrystallizer(BlockEntityRendererProvider.Context context) {

    }

    // This method is called every frame in order to render the block entity. Parameters are:
    // - blockEntity:   The block entity instance being rendered. Uses the generic type passed to the super interface.
    // - partialTick:   The amount of time, in fractions of a tick (0.0 to 1.0), that has passed since the last tick.
    // - poseStack:     The pose stack to render to.
    // - bufferSource:  The buffer source to get vertex buffers from.
    // - packedLight:   The light value of the block entity.
    // - packedOverlay: The current overlay value of the block entity, usually OverlayTexture.NO_OVERLAY.
    @Override
    public void render(EntityCrystallizer tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (tile.getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {

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

            double maxtranslate = 0.7;

            // A single translucent vertex buffer bound to the block atlas is used for all the fluid
            // parts of the crystallizer. The fluid sprite UVs are set on the model via scaleUV(...)
            // using the per-fluid TextureAtlasSprite coordinates.
            VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));


            if (tile.tank1.client_hasRecipe) {
                int progress = tile.tank1.client_recipeProgress;
                int maxTime = tile.tank1.client_recipeMaxTime;
                double maxTime_I = (double) 1 / maxTime;
                double partial_add = 0;
                if (tile.tank1.isRunning)
                    partial_add = partialTick * maxTime_I;
                double relativeProgress = progress * maxTime_I + partial_add;

                float fluidTranslation = 0;
                if (relativeProgress < 0.05) {
                    fluidTranslation = (float) (-1 + (relativeProgress * 20f));
                } else if (relativeProgress > 0.1) {
                    fluidTranslation = (float) (-(relativeProgress - 0.1) / 9 * 10);
                }


                if (tile.tank1.client_nextConsumedStacks.fluidStacks.size() == 1) {

                    IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(tile.tank1.client_nextConsumedStacks.fluidStacks.get(0).getFluid());
                    int color = extensions.getTintColor();
                    ResourceLocation fluidtexture = extensions.getStillTexture();
                    TextureAtlasSprite sprite = Minecraft.getInstance()
                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(fluidtexture);

                    model.scaleUV("Liquid",sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1());
                    stack.pushPose();
                    stack.translate(0.5f, 0, 0.5f);
                    stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis, angle));
                    stack.translate(-0.5f, 0, -0.5f);
                    stack.translate(0f, fluidTranslation * (float) maxtranslate, -1f);
                    model.renderPart("Liquid", stack, vc, packedLight, packedOverlay, color);
                    stack.popPose();
                }

                stack.pushPose();

                stack.translate(0.5f, 0, 0.5f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis, angle));
                stack.translate(-0.5f, 0, -0.5f);
                stack.translate(1f, 1.4, -0.4f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis, 90));
                stack.scale(0.6f, 0.6f, 0.6f);
                stack.scale((float) relativeProgress, (float) relativeProgress, (float) relativeProgress);

                int n = 0;
                for (ItemStack i : tile.tank1.client_nextProducedStacks.itemStacks) {
                    stack.translate(0.05 * n, 0.05 * n, 0.05 * n);
                    Minecraft.getInstance().getItemRenderer().render(i, ItemDisplayContext.NONE, false, stack, bufferSource, packedLight, packedOverlay, Minecraft.getInstance().getItemRenderer().getModel(i, null, null, 0));
                    n += 1;
                }

                stack.popPose();
            }




            if(tile.tank2.client_hasRecipe){
                int progress = tile.tank2.client_recipeProgress;
                int maxTime = tile.tank2.client_recipeMaxTime;
                double maxTime_I = (double) 1 / maxTime;
                double partial_add = 0;
                if (tile.tank2.isRunning)
                    partial_add = partialTick * maxTime_I;
                double relativeProgress = progress * maxTime_I + partial_add;

                float fluidTranslation = 0;
                if(relativeProgress<0.05){
                    fluidTranslation = (float) (-1+(relativeProgress*20f));
                }else if (relativeProgress > 0.1){
                    fluidTranslation = (float) (-(relativeProgress-0.1)/9*10);
                }

                if(tile.tank2.client_nextConsumedStacks.fluidStacks.size() == 1){

                    IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(tile.tank2.client_nextConsumedStacks.fluidStacks.get(0).getFluid());
                    int color = extensions.getTintColor();
                    ResourceLocation fluidtexture = extensions.getStillTexture();
                    TextureAtlasSprite sprite = Minecraft.getInstance()
                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(fluidtexture);

                    model.scaleUV("Liquid.002",sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1());
                    stack.pushPose();
                    stack.translate(0.5f, 0, 0.5f);
                    stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis, angle));
                    stack.translate(-0.5f, 0, -0.5f);
                    stack.translate(0f, fluidTranslation*(float)maxtranslate, -1f);
                    model.renderPart("Liquid.002", stack, vc, packedLight, packedOverlay,color);
                    stack.popPose();
                }

                stack.pushPose();

                stack.translate(0.5f, 0, 0.5f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis,angle));
                stack.translate(-0.5f, 0, -0.5f);
                stack.translate(1f, 1.4, 0.5f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis,90));
                stack.scale(0.6f,0.6f,0.6f);
                stack.scale((float) relativeProgress, (float) relativeProgress, (float) relativeProgress);

                int n = 0;
                for (ItemStack i : tile.tank2.client_nextProducedStacks.itemStacks){
                    stack.translate(0.05*n,0.05*n,0.05*n);
                    Minecraft.getInstance().getItemRenderer().render(i, ItemDisplayContext.NONE,false,stack,  bufferSource, packedLight, packedOverlay, Minecraft.getInstance().getItemRenderer().getModel(i, null, null, 0));
                    n+=1;
                }

                stack.popPose();
            }




            if(tile.tank3.client_hasRecipe){
                int progress = tile.tank3.client_recipeProgress;
                int maxTime = tile.tank3.client_recipeMaxTime;
                double maxTime_I = (double) 1 / maxTime;
                double partial_add = 0;
                if (tile.tank3.isRunning)
                    partial_add = partialTick * maxTime_I;
                double relativeProgress = progress * maxTime_I + partial_add;

                float fluidTranslation = 0;

                if(relativeProgress<0.05){
                    fluidTranslation = (float) (-1+(relativeProgress*20f));
                }else if (relativeProgress > 0.1){
                    fluidTranslation = (float) (-(relativeProgress-0.1)/9*10);
                }



                if(tile.tank3.client_nextConsumedStacks.fluidStacks.size() == 1){

                    IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(tile.tank3.client_nextConsumedStacks.fluidStacks.get(0).getFluid());
                    int color = extensions.getTintColor();
                    ResourceLocation fluidtexture = extensions.getStillTexture();
                    TextureAtlasSprite sprite = Minecraft.getInstance()
                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(fluidtexture);

                    model.scaleUV("Liquid.001",sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1());
                    stack.pushPose();
                    stack.translate(0.5f, 0, 0.5f);
                    stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis, angle));
                    stack.translate(-0.5f, 0, -0.5f);
                    stack.translate(0f, fluidTranslation*(float)maxtranslate, -1f);
                    model.renderPart("Liquid.001", stack, vc, packedLight, packedOverlay,color);
                    stack.popPose();
                }

                stack.pushPose();

                stack.translate(0.5f, 0, 0.5f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis,angle));
                stack.translate(-0.5f, 0, -0.5f);
                stack.translate(1f, 1.4, 1.4f);
                stack.mulPose(new Quaternionf().fromAxisAngleDeg(Yaxis,90));
                stack.scale(0.6f,0.6f,0.6f);
                stack.scale((float) relativeProgress, (float) relativeProgress, (float) relativeProgress);

                int n = 0;
                for (ItemStack i : tile.tank2.client_nextProducedStacks.itemStacks){
                    stack.translate(0.05*n,0.05*n,0.05*n);
                    Minecraft.getInstance().getItemRenderer().render(i, ItemDisplayContext.NONE,false,stack,  bufferSource, packedLight, packedOverlay, Minecraft.getInstance().getItemRenderer().getModel(i, null, null, 0));
                    n+=1;
                }

                stack.popPose();
            }
        }
    }
}


