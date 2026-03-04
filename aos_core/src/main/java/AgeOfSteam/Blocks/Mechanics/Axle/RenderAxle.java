package AgeOfSteam.Blocks.Mechanics.Axle;

import ARLib.mixins.ShaderInstanceMixin;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import AgeOfSteam.Main;
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
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderAxle implements BlockEntityRenderer<EntityAxleBase> {

    WavefrontObject model;
    ResourceLocation texture;

    public RenderAxle(BlockEntityRendererProvider.Context c, ResourceLocation tex) {
        super();
        this.texture = tex;
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/rod_new.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }


    @Override
    public void render(EntityAxleBase tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState axleState = tile.getBlockState();
        if (!(axleState.getBlock() instanceof BlockAxleBase)) return;

        // hide the block when it is part of multi-block-structure
        if (axleState.getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) return;

        Direction.Axis facingAxis = axleState.getValue(BlockAxleBase.ROTATION_AXIS);

        stack.translate(0.5f, 0.5f, 0.5f);

        if (facingAxis == Direction.Axis.Z) {
            // no rotation
        } else if (facingAxis == Direction.Axis.X) {
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1f, 0, 90));
        } else if (facingAxis == Direction.Axis.Y) {
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(1f, 0, 0, -90));
        }
        stack.mulPose(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f, (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick)));

        VertexConsumer v = bufferSource.getBuffer(Static.ENTITY_SOLID_TRIANGLES.apply(texture));

        for (Face i : model.groupObjects.get("Cube").faces) {
            i.addFaceForRender(stack, v, packedLight, packedOverlay, 0xffffffff);
        }
    }
}