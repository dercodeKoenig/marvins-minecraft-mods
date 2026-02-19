package AgeOfSteam.Blocks.Mechanics.FlyWheel;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import AgeOfSteam.Main;
import AgeOfSteam.Static;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderLargeFlyWheelBase extends RenderFlyWheelBase {

    public RenderLargeFlyWheelBase(BlockEntityRendererProvider.Context c, ResourceLocation texture) {
        super(c, texture);
        try {
            axle = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/flywheel_large_axle.obj"));
            flywheel = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/flywheel_large.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public AABB getRenderBoundingBox(EntityFlyWheelBase tile) {
        return new AABB(tile.getBlockPos()).inflate(1);
    }
}