package advRocketry.BlockEntityRenderers;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.Static;
import ARLib.obj.WavefrontObject;
import advRocketry.BlockEntities.EntityObservatory;
import advRocketry.Blocks.Observatory;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Main;
import advRocketry.utils.AxisDirections;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

import static net.minecraft.client.renderer.RenderStateShard.*;
import static net.minecraft.client.renderer.RenderStateShard.NO_TRANSPARENCY;

public class RenderObservatory implements BlockEntityRenderer<EntityObservatory> {


    static WavefrontObject model;
    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/observatory.png");

    static VertexFormat POSITION_COLOR_TEXTURE_NORMAL_LIGHT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("Color", VertexFormatElement.COLOR).add("UV0", VertexFormatElement.UV0).add("UV1", VertexFormatElement.UV1).add("UV2", VertexFormatElement.UV2).add("Normal", VertexFormatElement.NORMAL).build();

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/block/obj/observatory.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    public RenderObservatory(BlockEntityRendererProvider.Context c) {
        super();
    }

    @Override
    public AABB getRenderBoundingBox(EntityObservatory blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(5);
    }

    @Override
    public void render(EntityObservatory observatory, float partialtick, PoseStack stack, MultiBufferSource multiBufferSource, int light, int overlay) {
        EntityObservatory.RenderData renderData = observatory.renderData;

        BlockState state = observatory.getBlockState();
        if (!(state.getBlock() instanceof Observatory)) return;

        if (!state.getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED))
            return;

        Direction back = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();

        RenderType renderType = Static.ENTITY_SOLID_TRIANGLES.apply(tex);
        VertexConsumer v = multiBufferSource.getBuffer(renderType);

        stack.translate(back.getStepX() * 2.001 + 0.5f, 0, back.getStepZ() * 2.001 + 0.5f);

        // render base block
        model.renderPart("Base", stack, v, light, overlay);

        float yaw = renderData.yaw - (1 - partialtick) * renderData.yawD;
        float pitch = renderData.pitch - (1 - partialtick) * renderData.pitchD;
        //yaw = yaw * (float)Math.PI / 180;
        //pitch = pitch * (float)Math.PI / 180;

        // translate & yaw
        stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, yaw));

        stack.pushPose();
        stack.rotateAround(new Quaternionf().fromAxisAngleDeg(0, 0, 1, -pitch),0,2.5f,0);
        model.renderPart("Scope", stack, v, light, overlay);
        model.renderPart("Axis", stack, v, light, overlay);
        stack.popPose();

        float openProgress = (float) renderData.openingTicks / renderData.openingTicksMax;
        if (renderData.should_open) openProgress += partialtick / renderData.openingTicksMax;
        if (!renderData.should_open) openProgress -= partialtick / renderData.openingTicksMax;
        openProgress = Math.clamp(openProgress, 0, 1);

        stack.pushPose();
        stack.translate(0f, 0, openProgress * 0.9f);
        model.renderPart("CasingXPlus", stack, v, light, overlay);
        stack.popPose();

        stack.pushPose();
        stack.translate(0f, 0, -openProgress * 0.9f);
        model.renderPart("CasingXMinus", stack, v, light, overlay);
        stack.popPose();
    }
}
