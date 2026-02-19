package AOSWorkshopExpansion.Conveyor;

import AOSWorkshopExpansion.Main;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

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

        if(tile.vertexBuffer.isInvalid())return;
        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof ConveyorBelt)) return;

        if (tile.lastLight != packedLight || true) {
            tile.lastLight = packedLight;
            ByteBufferBuilder byteBuffer = new ByteBufferBuilder(64);
            BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, Static.POSITION_COLOR_TEXTURE_NORMAL_LIGHT);

            b.addVertex(-0.5f, 0, 0.5f).setNormal(0, 1, 0).setColor(0xffffffff).setUv(0, 1).setOverlay(0).setLight(packedLight);
            b.addVertex(0.5f, 0, 0.5f).setNormal(0, 1, 0).setColor(0xffffffff).setUv(1, 1).setOverlay(0).setLight(packedLight);
            b.addVertex(0.5f, 0, -0.5f).setNormal(0, 1, 0).setColor(0xffffffff).setUv(1, 0).setOverlay(0).setLight(packedLight);
            b.addVertex(-0.5f, 0, -0.5f).setNormal(0, 1, 0).setColor(0xffffffff).setUv(0, 0).setOverlay(0).setLight(packedLight);

            tile.mesh = b.build();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.upload(tile.mesh);
            byteBuffer.close();
        }


        Direction.Axis axis = state.getValue(ConveyorBelt.AXIS);

        stack.translate(0.5f, 2f/16f, 0.5f);
        if (axis == Direction.Axis.X) {
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0,1,0,-90));
        }

        Matrix4f modelMat = new Matrix4f();
        modelMat = modelMat.mul(stack.last().pose());

        float rotation = (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick);

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();
        RenderSystem.setShaderTexture(0, texture);

        ShaderInstance shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMat), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        Uniform UVOffset = shader.getUniform("UVOffset");
        UVOffset.set(0f, rotation / 360f);
        shader.apply();

        tile.vertexBuffer.bind();
        tile.vertexBuffer.draw();

        shader.clear();
        VertexBuffer.unbind();


        for(ItemStack itemStack : tile.items_progress.keySet()){
            float progress = tile.items_progress.get(itemStack);

            stack.pushPose();
            stack.translate(0, 0.1, -progress+0.5f);
            float scale = 0.6f;
            stack.scale(scale, scale, scale);
            Minecraft.getInstance().getItemRenderer().renderStatic(itemStack, ItemDisplayContext.FIXED, packedLight, packedOverlay, stack, bufferSource, null, 0);

            stack.popPose();
        }

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();

    }
}
