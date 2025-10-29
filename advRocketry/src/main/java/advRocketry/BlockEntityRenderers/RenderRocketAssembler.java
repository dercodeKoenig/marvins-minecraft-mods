package advRocketry.BlockEntityRenderers;

import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.Main;
import advRocketry.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

// renders the construction scan tower
// i choose to use the simple approach without vertex buffers because there are probably only a few rocket assemblers visible at once on screen

public class RenderRocketAssembler implements BlockEntityRenderer<EntityRocketAssembler> {

    private ResourceLocation grid = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/grid.png");
    private ResourceLocation girder = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/girder.png");
    private ResourceLocation round_h = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/round_h.png");

    public RenderRocketAssembler(BlockEntityRendererProvider.Context c) {
        super();
    }

    public AABB getRenderBoundingBox(EntityRocketAssembler blockEntity) {
        if (blockEntity.areaMax == null || blockEntity.areaMin == null)
            return new AABB(blockEntity.getBlockPos());
        else {
            return new AABB(
                    new Vec3(blockEntity.areaMin.getX(), blockEntity.areaMin.getY(), blockEntity.areaMin.getZ()),
                    new Vec3(blockEntity.areaMax.getX() + 1, blockEntity.areaMax.getY() + 1, blockEntity.areaMax.getZ() + 1)
            ).inflate(2);
        }
    }

    public void renderScanTowerLeg(VertexConsumer vertexConsumer, PoseStack stack, float x, float z, float y0, float y1, int packedLight, int towerColor) {
        RenderUtils.renderTopFace(vertexConsumer, stack.last(), x - 0.25f, x + 0.25f, z - 0.25f, z + 0.25f, y1 + 0.25f, 0, 1, 0, 1, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
        RenderUtils.renderBottomFace(vertexConsumer, stack.last(), x - 0.25f, x + 0.25f, z - 0.25f, z + 0.25f, y0 - 0.25f, 0, 1, 0, 1, packedLight, OverlayTexture.NO_OVERLAY, towerColor);

        RenderUtils.renderEastFace(vertexConsumer, stack.last(), y0 - 0.25f, y1 + 0.25f, z - 0.25f, z + 0.25f, x + 0.25f, 0, 1, 2 * (y1 - y0) + 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
        RenderUtils.renderWestFace(vertexConsumer, stack.last(), y0 - 0.25f, y1 + 0.25f, z - 0.25f, z + 0.25f, x - 0.25f, 0, 1, 2 * (y1 - y0) + 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);

        RenderUtils.renderSouthFace(vertexConsumer, stack.last(), y0 - 0.25f, y1 + 0.25f, x - 0.25f, x + 0.25f, z + 0.25f, 0, 1, 2 * (y1 - y0) + 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
        RenderUtils.renderNorthFace(vertexConsumer, stack.last(), y0 - 0.25f, y1 + 0.25f, x - 0.25f, x + 0.25f, z - 0.25f, 0, 1, 2 * (y1 - y0) + 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
    }

    @Override
    public void render(EntityRocketAssembler entity, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (entity.areaMin == null || entity.areaMax == null) return;

        int blockLight = entity.getLevel().getBrightness(LightLayer.BLOCK, entity.getBlockPos().above());
        int skyLight = entity.getLevel().getBrightness(LightLayer.SKY, entity.getBlockPos().above());
        packedLight = LightTexture.pack(blockLight, skyLight);

        int towerColor = 0xffa09080;

        BlockPos relativeMin = entity.areaMin.subtract(entity.getBlockPos());
        Vector3f relativeMinCenter = new Vector3f(relativeMin.getX() + 0.5f, relativeMin.getY() + 0.5f, relativeMin.getZ() + 0.5f);
        BlockPos relativeMax = entity.areaMax.subtract(entity.getBlockPos());
        Vector3f relativeMaxCenter = new Vector3f(relativeMax.getX() + 0.5f, relativeMax.getY() + 0.5f, relativeMax.getZ() + 0.5f);

        float e = 0.001f;
        float x0 = relativeMinCenter.x - 1.25f + e;
        float x1 = relativeMaxCenter.x + 1.25f - e;
        float z0 = relativeMinCenter.z - 1.25f + e;
        float z1 = relativeMaxCenter.z + 1.25f - e;
        float y0 = relativeMinCenter.y - 1.25f + e;


        float partialTickOffset = entity.clientBuildDiffPerTick*partialTick;
        float h = (float) (entity.clientBuildProgress+partialTickOffset) / EntityRocketAssembler.buildTimeBase;

        int scanHeightMax = entity.areaMax.getY()-entity.areaMin.getY()+1;

        float y1 = relativeMinCenter.y - 0.25f + e + Math.clamp(h,0,scanHeightMax);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(girder));
        // render the 4 legs
        renderScanTowerLeg(vertexConsumer, stack, x0, z0, y0, y1, packedLight, towerColor);
        renderScanTowerLeg(vertexConsumer, stack, x0, z1, y0, y1, packedLight, towerColor);
        renderScanTowerLeg(vertexConsumer, stack, x1, z0, y0, y1, packedLight, towerColor);
        renderScanTowerLeg(vertexConsumer, stack, x1, z1, y0, y1, packedLight, towerColor);

        Direction.Axis myFacingAxis = entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis();

        // render the top/bottom faces of the top connections between the 4 legs
        if (myFacingAxis == Direction.Axis.X) {
            RenderUtils.renderTopFace(vertexConsumer, stack.last(), x0 + 0.25f, x1 - 0.25f, z0 - 0.25f, z0 + 0.25f, y1 + 0.25f, 2 * (x1 - x0) + 1, 1, 0, 1, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
            RenderUtils.renderBottomFace(vertexConsumer, stack.last(), x0 + 0.25f, x1 - 0.25f, z0 - 0.25f, z0 + 0.25f, y1 - 0.125f, 2 * (x1 - x0) + 1, 1, 0, 1, packedLight, OverlayTexture.NO_OVERLAY, towerColor);

            RenderUtils.renderTopFace(vertexConsumer, stack.last(), x0 + 0.25f, x1 - 0.25f, z1 - 0.25f, z1 + 0.25f, y1 + 0.25f, 2 * (x1 - x0) + 1, 1, 0, 1, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
            RenderUtils.renderBottomFace(vertexConsumer, stack.last(), x0 + 0.25f, x1 - 0.25f, z1 - 0.25f, z1 + 0.25f, y1 - 0.125f, 2 * (x1 - x0) + 1, 1, 0, 1, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
        }
        if (myFacingAxis == Direction.Axis.Z) {
            RenderUtils.renderTopFace(vertexConsumer, stack.last(), x0 - 0.25f, x0 + 0.25f, z0 + 0.25f, z1 - 0.25f, y1 + 0.25f, 0, 1, 2 * (x1 - x0) + 1, 1, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
            RenderUtils.renderBottomFace(vertexConsumer, stack.last(), x0 - 0.25f, x0 + 0.25f, z0 + 0.25f, z1 - 0.25f, y1 - 0.125f, 0, 1, 2 * (x1 - x0) + 1, 1, packedLight, OverlayTexture.NO_OVERLAY, towerColor);

            RenderUtils.renderTopFace(vertexConsumer, stack.last(), x1 - 0.25f, x1 + 0.25f, z0 + 0.25f, z1 - 0.25f, y1 + 0.25f, 0, 1, 2 * (x1 - x0) + 1, 1, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
            RenderUtils.renderBottomFace(vertexConsumer, stack.last(), x1 - 0.25f, x1 + 0.25f, z0 + 0.25f, z1 - 0.25f, y1 - 0.125f, 0, 1, 2 * (x1 - x0) + 1, 1, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
        }

        vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(grid));
        // render the long side faces of the top connections between the 4 legs
        if (myFacingAxis == Direction.Axis.X) {
            RenderUtils.renderSouthFace(vertexConsumer, stack.last(), y1 - 0.25f, y1 + 0.25f, x0 + 0.25f, x1 - 0.25f, z0 + 0.25f, 0, 1, 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
            RenderUtils.renderNorthFace(vertexConsumer, stack.last(), y1 - 0.25f, y1 + 0.25f, x0 + 0.25f, x1 - 0.25f, z0 - 0.25f, 0, 1, 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);

            RenderUtils.renderSouthFace(vertexConsumer, stack.last(), y1 - 0.25f, y1 + 0.25f, x0 + 0.25f, x1 - 0.25f, z1 + 0.25f, 0, 1, 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
            RenderUtils.renderNorthFace(vertexConsumer, stack.last(), y1 - 0.25f, y1 + 0.25f, x0 + 0.25f, x1 - 0.25f, z1 - 0.25f, 0, 1, 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
        }
        if (myFacingAxis == Direction.Axis.Z) {
            RenderUtils.renderEastFace(vertexConsumer, stack.last(), y1 - 0.25f, y1 + 0.25f, z0 + 0.25f, z1 - 0.25f, x0 + 0.25f, 0, 1, 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
            RenderUtils.renderWestFace(vertexConsumer, stack.last(), y1 - 0.25f, y1 + 0.25f, z0 + 0.25f, z1 - 0.25f, x0 - 0.25f, 0, 1, 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);

            RenderUtils.renderEastFace(vertexConsumer, stack.last(), y1 - 0.25f, y1 + 0.25f, z0 + 0.25f, z1 - 0.25f, x1 + 0.25f, 0, 1, 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
            RenderUtils.renderWestFace(vertexConsumer, stack.last(), y1 - 0.25f, y1 + 0.25f, z0 + 0.25f, z1 - 0.25f, x1 - 0.25f, 0, 1, 1, 0, packedLight, OverlayTexture.NO_OVERLAY, towerColor);
        }


        // render the scanning glowing thing stuff whatever
        vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(round_h));

        if (entity.buildProgress > -1 && (float)entity.buildProgress / EntityRocketAssembler.buildTimeBase < scanHeightMax+0.5f) {
            if (myFacingAxis == Direction.Axis.X) {
                // normal uv
                RenderUtils.renderTopFace(vertexConsumer, stack.last(), x0, x1, z0, z1, y1, 0, 1 + (x1 - x0), 0, 1 + (z1 - z0), packedLight, OverlayTexture.NO_OVERLAY, 0xffff0000);
            } else {
                // rotate uv by 90°
                RenderUtils.renderTopFace2(vertexConsumer, stack.last(), x0, x1, z0, z1, y1, 0, 1 + (x1 - x0), 0, 1 + (z1 - z0), packedLight, OverlayTexture.NO_OVERLAY, 0xffff0000);
            }
        }
    }
}