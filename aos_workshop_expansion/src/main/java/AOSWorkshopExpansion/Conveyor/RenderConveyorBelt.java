package AOSWorkshopExpansion.Conveyor;

import AOSWorkshopExpansion.Main;
import AgeOfSteam.Static;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static AgeOfSteam.Static.TPS;
import static AgeOfSteam.Static.rad_to_degree;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderConveyorBelt implements BlockEntityRenderer<EntityConveyorBelt> {

    public static ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/belt.png");

    public RenderConveyorBelt(BlockEntityRendererProvider.Context c) {
        super();
    }


    @Override
    public void render(EntityConveyorBelt tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {


        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof ConveyorBelt)) return;

        Direction facing = state.getValue(ConveyorBelt.FACING);
        Direction.Axis axis = facing.getAxis();
        boolean isDiagonal = state.getValue(ConveyorBelt.DIAGONAL);

        int y0 = 0;
        int y1 = 0;
        if (isDiagonal) {
            if (facing == Direction.SOUTH || facing == Direction.WEST)
                y0 = 1;
            else
                y1 = 1;
        }

        float partialRotation = (float) (rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick);
        float rotation = (float) (tile.myMechanicalBlock.currentRotation + partialRotation);
        float v1 = 1;
        if (y1 != 0 || y0 != 0)
            v1 = 1.41f; //diagonal, extended uv

        float vOffset = rotation / 360f;

        stack.translate(0.5f, 2f / 16f, 0.5f);
        if (axis == Direction.Axis.X) {
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, -90));
        }

        VertexConsumer v = bufferSource.getBuffer(RenderType.entitySolid(texture));

        v.addVertex(stack.last(), new Vector3f(-0.5f, y0, 0.5f)).setNormal(0, 1, 0).setColor(0xffffffff).setUv(0, v1 + vOffset).setOverlay(packedOverlay).setLight(packedLight);
        v.addVertex(stack.last(), new Vector3f(0.5f, y0, 0.5f)).setNormal(0, 1, 0).setColor(0xffffffff).setUv(1, v1 + vOffset).setOverlay(packedOverlay).setLight(packedLight);
        v.addVertex(stack.last(), new Vector3f(0.5f, y1, -0.5f)).setNormal(0, 1, 0).setColor(0xffffffff).setUv(1, 0 + vOffset).setOverlay(packedOverlay).setLight(packedLight);
        v.addVertex(stack.last(), new Vector3f(-0.5f, y1, -0.5f)).setNormal(0, 1, 0).setColor(0xffffffff).setUv(0, 0 + vOffset).setOverlay(packedOverlay).setLight(packedLight);


        for (ItemStack itemStack : tile.items_progress.keySet()) {
            float progress = tile.items_progress.get(itemStack) + partialRotation / 360f;
            float yOffset = progress * y1 + (1 - progress) * y0;
            stack.pushPose();
            stack.translate(0, 0.1 + yOffset, -progress + 0.5f);
            float scale = 0.6f;
            stack.scale(scale, scale, scale);
            Minecraft.getInstance().getItemRenderer().renderStatic(itemStack, ItemDisplayContext.FIXED, packedLight, packedOverlay, stack, bufferSource, null, 0);

            stack.popPose();
        }
    }
}
