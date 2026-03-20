package advRocketry.BlockEntityRenderers;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.ModelFormatException;
import ARLib.obj.Static;
import ARLib.obj.WavefrontObject;
import BetterPipes.Tank.BlockTank;
import BetterPipes.Tank.EntityTank;
import advRocketry.BlockEntities.EntityObservatory;
import advRocketry.BlockEntities.EntityPressureTank;
import advRocketry.Blocks.Observatory;
import advRocketry.Main;
import advRocketry.Utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Quaternionf;

public class RenderPressureTank implements BlockEntityRenderer<EntityPressureTank> {

    public RenderPressureTank(BlockEntityRendererProvider.Context c) {
        super();
    }


    @Override
    public void render(EntityPressureTank tile, float partialtick, PoseStack stack, MultiBufferSource multiBufferSource, int light, int overlay) {

        if (tile.tank.isEmpty())
            return;

        VertexConsumer v = multiBufferSource.getBuffer(RenderType.entityTranslucent(tile.spriteStill.atlasLocation()));

        float relativeFill = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();

        boolean renderTop = true;
        boolean renderBottom = true;

        // level is null on rockets, so we always render the faces
        if(tile.getLevel() != null) {
            if (tile.getLevel().getBlockEntity(tile.getBlockPos().below()) instanceof EntityPressureTank otherTank) {
                if (FluidStack.isSameFluidSameComponents(otherTank.tank.getFluid(), tile.tank.getFluid())) {
                    renderBottom = false;
                }
            }
            if (tile.getLevel().getBlockEntity(tile.getBlockPos().above()) instanceof EntityPressureTank otherTank) {
                if (FluidStack.isSameFluidSameComponents(otherTank.tank.getFluid(), tile.tank.getFluid())) {
                    renderTop = false;
                }
            }
        }

        float e = 0.001f;

        float y0 = renderBottom ? e : 0;
        float y1 = renderTop ? relativeFill - e : relativeFill;
        float x0 = e;
        float x1 = 1 - (e);
        float z0 = e;
        float z1 = 1 - (e);

        float u0 = tile.spriteStill.getU0();
        float u1 = tile.spriteStill.getU1();
        float v0 = tile.spriteStill.getV0();
        float v1 = tile.spriteStill.getV1();

        if (renderTop)
            RenderUtils.renderTopFace(v, stack.last(), x0, x1, z0, z1, y1, u0, u1, v0, v1, light, overlay, tile.color);
        if (renderBottom)
            RenderUtils.renderTopFace(v, stack.last(), x0, x1, z0, z1, y0, u0, u1, v0, v1, light, overlay, tile.color);

        RenderUtils.renderEastFace(v, stack.last(), y0, y1, z0, z1, x1, u0, u1, v0, v1, light, overlay, tile.color);
        RenderUtils.renderWestFace(v, stack.last(), y0, y1, z0, z1, x0, u0, u1, v0, v1, light, overlay, tile.color);

        RenderUtils.renderNorthFace(v, stack.last(), y0, y1, x0, x1, z0, u0, u1, v0, v1, light, overlay, tile.color);
        RenderUtils.renderSouthFace(v, stack.last(), y0, y1, x0, x1, z1, u0, u1, v0, v1, light, overlay, tile.color);

    }
}
