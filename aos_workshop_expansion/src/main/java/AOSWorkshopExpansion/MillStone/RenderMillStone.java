package AOSWorkshopExpansion.MillStone;

import AOSWorkshopExpansion.Main;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.POSITION_COLOR_TEXTURE_NORMAL_LIGHT;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderMillStone implements BlockEntityRenderer<EntityMillStone> {

    static WavefrontObject model;
    static ResourceLocation texAxle = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/planks.png");
    static ResourceLocation texStone = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/stone.png");
    static ResourceLocation texStoneLarge = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/stone_large.png");


    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/millstone.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }


    public RenderMillStone(BlockEntityRendererProvider.Context c) {
        super();
    }

    public AABB getRenderBoundingBox(EntityMillStone tile) {
        return new AABB(tile.getBlockPos()).inflate(1);
    }

    @Override
    public void render(EntityMillStone tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.lastLight != packedLight) {

            ByteBufferBuilder byteBuffer;
            BufferBuilder b;


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("axle").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.meshAxle = b.build();
            tile.vertexBufferAxle.bind();
            tile.vertexBufferAxle.upload(tile.meshAxle);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("stone").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.meshStone = b.build();
            tile.vertexBufferStone.bind();
            tile.vertexBufferStone.upload(tile.meshStone);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("plate").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.meshPlate = b.build();
            tile.vertexBufferPlate.bind();
            tile.vertexBufferPlate.upload(tile.meshPlate);
            byteBuffer.close();
        }

        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof BlockMillStone)) return;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        stack.translate(0.5f, 0.5f, 0.5f);
        Matrix4f m1 = new Matrix4f(RenderSystem.getModelViewMatrix());
        m1 = m1.mul(stack.last().pose());
        double directionMultiplier = 1;
        if (facing == Direction.WEST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 90f));
        }
        if (facing == Direction.EAST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 270f));
            directionMultiplier = -1;
        }
        if (facing == Direction.SOUTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 0f));
        }
        if (facing == Direction.NORTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 180f));
            directionMultiplier = -1;
        }

        double rotation = (tile.myMechanicalBlock.currentRotation + Static.rad_to_degree(tile.myMechanicalBlock.internalVelocity) / Static.TPS * partialTick) * directionMultiplier;

        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        ShaderInstance shader = RenderSystem.getShader();
        RenderSystem.setShaderTexture(0, texAxle);

        Matrix4f m2 = new Matrix4f(m1);

        m2 = m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, (float) rotation));
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.apply();
        tile.vertexBufferAxle.bind();
        tile.vertexBufferAxle.draw();

        if (!state.getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
            shader.clear();
            VertexBuffer.unbind();
            LIGHTMAP.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            NO_TRANSPARENCY.clearRenderState();
            return;
        }
        ;

        RenderSystem.setShaderTexture(0, texStone);
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.apply();
        tile.vertexBufferStone.bind();
        tile.vertexBufferStone.draw();

        Matrix4f m3 = new Matrix4f(m1);

        RenderSystem.setShaderTexture(0, texStoneLarge);
        m3 = m3.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1f, 0f, (float) -rotation * 0.25f));
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m3, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.apply();
        tile.vertexBufferPlate.bind();
        tile.vertexBufferPlate.draw();

        long t0 = System.nanoTime();
        for (int i = 0; i < tile.inventory.getSlots(); i++) {
            ItemStack s = tile.inventory.getStackInSlot(i);
            stack.pushPose();

            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, 20 * i - (float) (rotation * 0.25)));
            stack.translate(0.85, -0.3, 0);
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(1, 0, 0, 90f));
            stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, 10f));
            float scale = 0.4f;
            stack.scale(scale, scale, scale);
            Minecraft.getInstance().getItemRenderer().renderStatic(s, ItemDisplayContext.FIXED, packedLight, packedOverlay, stack, bufferSource, null, 0);


            stack.popPose();
        }
        long t1 = System.nanoTime();
        // TODO the item rendering is a bit slow....
        //System.out.println((double)(t1-t0) / 1000 / 1000);

        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();
    }
}