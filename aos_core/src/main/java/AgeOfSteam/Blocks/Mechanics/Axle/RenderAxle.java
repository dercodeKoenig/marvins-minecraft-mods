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

        if (tile.lastLight != packedLight) {
            tile.lastLight = packedLight;
            ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
            BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("Cube").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh = b.build();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.upload(tile.mesh);
            byteBuffer.close();
        }

        BlockState axleState = tile.getBlockState();
        if (!(axleState.getBlock() instanceof BlockAxleBase)) return;

        // hide the block when it is part of multi-block-structure
        if (axleState.getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) return;

        Direction.Axis facingAxis = axleState.getValue(BlockAxleBase.ROTATION_AXIS);

        Matrix4f modelMat = new Matrix4f();
        modelMat = modelMat.mul(stack.last().pose());

        modelMat = modelMat.translate(0.5f, 0.5f, 0.5f);

        if (facingAxis == Direction.Axis.Z) {
            // no rotation
        } else if (facingAxis == Direction.Axis.X) {
            modelMat = modelMat.rotate(new Quaternionf().fromAxisAngleDeg(0, 1f, 0, 90));
        } else if (facingAxis == Direction.Axis.Y) {
            modelMat = modelMat.rotate(new Quaternionf().fromAxisAngleDeg(1f, 0, 0, -90));
        }

        modelMat = modelMat.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f, (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick)));
        //System.out.println(tile.currentRotation);


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