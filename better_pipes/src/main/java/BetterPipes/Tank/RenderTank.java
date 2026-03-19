package BetterPipes.Tank;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

import static net.minecraft.client.renderer.RenderStateShard.*;
import static net.minecraft.client.renderer.RenderType.TRANSIENT_BUFFER_SIZE;

public class RenderTank implements BlockEntityRenderer<EntityTank> {

    static float e = 0.001f;

    public RenderTank(BlockEntityRendererProvider.Context c) {
        super();
    }

    public static void renderFluidCubeStill(
            PoseStack.Pose pose,
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            int color,
            VertexConsumer v, int light, int overlay,
            boolean renderTop, boolean renderBottom
    ) {


        //render up face
        if (renderTop) {
            v.addVertex(pose, x0, y1, z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex(pose, x0, y1, z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex(pose, x1, y1, z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex(pose, x1, y1, z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        }

        //render bottom face
        if (renderBottom) {
            v.addVertex(pose, x1, y0, z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex(pose, x1, y0, z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex(pose, x0, y0, z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex(pose, x0, y0, z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        }

        // Render east face (x+ side)
        v.addVertex(pose, x1, y0, z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        v.addVertex(pose, x1, y1, z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        v.addVertex(pose, x1, y1, z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        v.addVertex(pose, x1, y0, z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);

        // Render west face (x- side)
        v.addVertex(pose, x0, y0, z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(pose, x0, y1, z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(pose, x0, y1, z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(pose, x0, y0, z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

        // Render south face (z+ side)
        v.addVertex(pose, x1, y0, z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        v.addVertex(pose, x1, y1, z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        v.addVertex(pose, x0, y1, z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        v.addVertex(pose, x0, y0, z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

        // Render north face (z- side)
        v.addVertex(pose, x0, y0, z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        v.addVertex(pose, x0, y1, z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        v.addVertex(pose, x1, y1, z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        v.addVertex(pose, x1, y0, z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);

    }


    @Override
    public void render(EntityTank tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if(tile.myTank.isEmpty())
            return;

        VertexConsumer v = bufferSource.getBuffer(RenderType.entityTranslucent(tile.spriteStill.atlasLocation()));

        float relativeFill = (float) tile.myTank.getFluidAmount() / tile.myTank.getCapacity();

        boolean renderTop = true;
        boolean renderBottom = true;
        if (tile.getBlockState().getValue(BlockTank.connectedBelow)) {
            BlockEntity other = tile.getLevel().getBlockEntity(tile.getBlockPos().below());
            if (other instanceof EntityTank otherTank) {
                if (FluidStack.isSameFluidSameComponents(otherTank.myTank.getFluid(), tile.myTank.getFluid())) {
                    renderBottom = false;
                }
            }
        }
        if (tile.getBlockState().getValue(BlockTank.connectedAbove)) {
            BlockEntity other = tile.getLevel().getBlockEntity(tile.getBlockPos().above());
            if (other instanceof EntityTank otherTank) {
                if (FluidStack.isSameFluidSameComponents(otherTank.myTank.getFluid(), tile.myTank.getFluid())) {
                    renderTop = false;
                }
            }
        }

        float y0 = renderBottom ? e : 0;
        float y1 = renderTop ? relativeFill - e : relativeFill;
        float x0 = e + 2f / 16;
        float x1 = 1 - (e + 2f / 16);
        float z0 = e + 2f / 16;
        float z1 = 1 - (e + 2f / 16);
        renderFluidCubeStill(stack.last(), x0, x1, z0, z1, y0, y1,
                tile.spriteStill.getU0(), tile.spriteStill.getU1(), tile.spriteStill.getV0(), tile.spriteStill.getV1(),
                tile.color, v, packedLight, packedOverlay, renderTop, renderBottom);
    }
}