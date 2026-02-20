package AOSWorkshopExpansion.Piston;

import AOSWorkshopExpansion.Main;
import AOSWorkshopExpansion.Registry;
import AgeOfSteam.Static;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;

import static AgeOfSteam.Static.TPS;
import static AgeOfSteam.Static.rad_to_degree;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderPiston implements BlockEntityRenderer<EntityPiston> {

    public RenderPiston(BlockEntityRendererProvider.Context c) {
        super();
    }

    @Override
    // to avoid messing around with trying to calculate render box, just render always. we skip render anyway if no blocks
    public boolean shouldRenderOffScreen(EntityPiston piston) {
        return true;
    }

    @Override
    public void render(EntityPiston tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (tile.currentAction == 0)
            return;

        // i use .tocenter to get the vector3f quickly so offset
        stack.translate(-0.5f, -0.5f, -0.5f);

        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();

        Direction travelDirection = tile.getBlockState().getValue(Piston.SPECIALFACING).direction;
        if (tile.currentAction == -1)
            travelDirection = travelDirection.getOpposite();

        // tile.movingBlocks contains the target position and the blockState
        // there is no entry with target position = piston position because it would replace the piston
        // because it skips the piston, we need to add a custom entry that would move to the piston position

        HashMap<BlockPos, BlockState> movingBlocks = new HashMap<>();
        // copy existing entries to not modify the original map with relative position
        for (BlockPos p : tile.movingBlocks.keySet()) {
            movingBlocks.put(p.subtract(tile.getBlockPos()), tile.movingBlocks.get(p));
        }
        // add the pistons extension to render (this block is the pistons position but there is no extension)
        movingBlocks.put(
                new BlockPos(0,0,0),
                Registry.PISTON_EXTENSION.get().defaultBlockState().setValue(PistonExtension.AXIS, tile.getBlockState().getValue(Piston.SPECIALFACING).direction.getAxis())
        );
        for (BlockPos p : movingBlocks.keySet()) {
            Vector3f start = p.relative(travelDirection.getOpposite()).getCenter().toVector3f();
            Vector3f end = p.getCenter().toVector3f();
            Vector3f direction = end.sub(start);
            float progress = (float) tile.movingTicks / tile.moveTicksMax + partialTick / tile.moveTicksMax;
            Vector3f position = start.add(direction.mul(progress));
            BlockState state = movingBlocks.get(p);
            stack.pushPose();
            stack.translate(position.x, position.y, position.z);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, stack, bufferSource, packedLight, packedOverlay, ModelData.EMPTY, null);
            stack.popPose();
        }


        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();

    }
}
