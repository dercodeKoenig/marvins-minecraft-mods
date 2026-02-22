package AOSWorkshopExpansion.Drill;

import AOSWorkshopExpansion.Main;
import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
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

import static AOSWorkshopExpansion.Drill.Drill.FACING;
import static AgeOfSteam.Static.TPS;
import static AgeOfSteam.Static.rad_to_degree;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderDrill implements BlockEntityRenderer<EntityDrill> {

    public static ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/drill.png");
    static WavefrontObject model;

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/drill.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    public RenderDrill(BlockEntityRendererProvider.Context c) {
        super();
    }

    @Override
    public void render(EntityDrill tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.vertexBuffer.isInvalid()) return;
        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof Drill)) return;

        Direction facing = state.getValue(FACING);

        if (tile.lastLight != packedLight) {
            tile.lastLight = packedLight;

            ByteBufferBuilder byteBuffer = new ByteBufferBuilder(64);
            BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, Static.POSITION_COLOR_TEXTURE_NORMAL_LIGHT);

            for (Face i : model.groupObjects.get("head").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }

            tile.mesh = b.build();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.upload(tile.mesh);
            byteBuffer.close();
        }


        stack.translate(0.5f, 0.5f, 0.5f);
        if (facing == Direction.NORTH) {
            //stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, -90));
        }
        if (facing == Direction.WEST) {
            //stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, -90));
        }
        if (facing == Direction.EAST) {
            //stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, -90));
        }
        if (facing == Direction.SOUTH) {
            //stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, -90));
        }
        if (facing == Direction.UP) {
            //stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, -90));
        }
        if (facing == Direction.DOWN) {
            //stack.mulPose(new Quaternionf().fromAxisAngleDeg(0, 1, 0, -90));
        }

        Matrix4f modelMat = new Matrix4f();
        modelMat = modelMat.mul(stack.last().pose());

        float partialRotation = (float) (rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick);
        float rotation = (float) (tile.myMechanicalBlock.currentRotation + partialRotation);

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();
        RenderSystem.setShaderTexture(0, texture);

        ShaderInstance shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMat), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        Uniform NormalMat = shader.getUniform("NormalMat");
        NormalMat.set(Static.getNormalMat(modelMat));
        shader.apply();

        tile.vertexBuffer.bind();
        tile.vertexBuffer.draw();

        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();

    }
}
