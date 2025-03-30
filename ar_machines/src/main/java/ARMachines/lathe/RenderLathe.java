package ARMachines.lathe;


import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.GroupObject;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
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

import static ARLib.obj.GroupObject.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
import static net.minecraft.client.renderer.RenderStateShard.*;

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
            model.resetTransformations("Hull");
            model.translateWorldSpace("Hull", new Vector3f(0.5f, 0, 0.5f));
            model.rotateWorldSpace("Hull", Yaxis, angle);
            model.translateWorldSpace("Hull", new Vector3f(-0.5f, 0, -0.5f));
            model.applyTransformations("Hull");
            model.renderPart("Hull", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);
*/

            double maxTranslate = -1.12;
            double translation = relativeProgress;
            if (translation > 0.5) translation = 1 - translation;


            model.resetTransformations("Tool");
            model.translateWorldSpace("Tool", new Vector3f(0.5f, 0, 0.5f));
            model.rotateWorldSpace("Tool", Yaxis, angle);
            model.translateWorldSpace("Tool", new Vector3f(-0.5f, 0, -0.5f));
            model.translateWorldSpace("Tool", new Vector3f(0.935f, -0.319f, 1.51f - (float) maxTranslate * (float) translation * 2f));
            model.applyTransformations("Tool");
            model.renderPart("Tool", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);


            if (tile.client_hasRecipe) {
                model.resetTransformations("Shaft");
                model.translateWorldSpace("Shaft", new Vector3f(0.5f, 0, 0.5f));
                model.rotateWorldSpace("Shaft", Yaxis, angle);
                model.translateWorldSpace("Shaft", new Vector3f(-0.5f, 0, -0.5f));
                model.translateWorldSpace("Shaft", new Vector3f(0.62f, 0.18f, 1.50471f));
                model.rotateWorldSpace("Shaft", new Vector3f(0, 0, 1), (float) relativeProgress * 360f * 10);
                model.applyTransformations("Shaft");
                model.renderPart("Shaft", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);

            }
        }
    }
}

