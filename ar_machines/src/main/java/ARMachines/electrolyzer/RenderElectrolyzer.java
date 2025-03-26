package ARMachines.electrolyzer;


import ARLib.obj.WavefrontObject;
import ARMachines.lathe.EntityLathe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static ARLib.obj.GroupObject.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
import static net.minecraft.client.renderer.RenderStateShard.*;
/*
public class RenderElectrolyzer implements BlockEntityRenderer<EntityElectrolyzer> {

    ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("armachines", "multiblock/electrolyzer.png");

    public int getViewDistance() {
        return 256;
    }

    @NotNull
    @Override
    public AABB getRenderBoundingBox(EntityElectrolyzer blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(100);
    }


    public RenderElectrolyzer(BlockEntityRendererProvider.Context context) {

    }

    // This method is called every frame in order to render the block entity. Parameters are:
    // - blockEntity:   The block entity instance being rendered. Uses the generic type passed to the super interface.
    // - partialTick:   The amount of time, in fractions of a tick (0.0 to 1.0), that has passed since the last tick.
    // - poseStack:     The pose stack to render to.
    // - bufferSource:  The buffer source to get vertex buffers from.
    // - packedLight:   The light value of the block entity.
    // - packedOverlay: The current overlay value of the block entity, usually OverlayTexture.NO_OVERLAY.
    @Override
    public void render(EntityElectrolyzer tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

WavefrontObject model = tile.model;
        if (tile.isMultiblockFormed()) {

            VertexFormat vertexFormat = POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
            RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
                    .setOverlayState(OVERLAY)
                    .setLightmapState(LIGHTMAP)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .createCompositeState(false);


            // Get the facing direction of the block
            Direction facing = tile.getFacing();
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

            model.resetTransformations("Hull");
            model.translateWorldSpace("Hull", new Vector3f(0.5f, 0, 0.5f));
            model.rotateWorldSpace("Hull", Yaxis, angle);
            model.translateWorldSpace("Hull", new Vector3f(-0.5f, 0, -0.5f));
            model.applyTransformations("Hull");
            model.renderPart("Hull", stack, bufferSource, vertexFormat, compositeState, packedLight, packedOverlay);

        }
    }
}

 */

