package AgeOfSteam.Blocks.Mechanics.FlyWheel;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import AgeOfSteam.Main;
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

public class RenderFlyWheelBase implements BlockEntityRenderer<EntityFlyWheelBase> {

    WavefrontObject axle;
    WavefrontObject flywheel;

    ResourceLocation tex;

    public RenderFlyWheelBase(BlockEntityRendererProvider.Context c, ResourceLocation texture) {
        super();
        this.tex = texture;
        try {
            axle = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/rod_new.obj"));
            flywheel = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/flywheel.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }


    @Override
    public void render(EntityFlyWheelBase tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.lastLight != packedLight) {
            tile.lastLight = packedLight;

            ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
            BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : axle.groupObjects.get("Cube").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            for (Face i : flywheel.groupObjects.get("fly_wheel").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh = b.build();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.upload(tile.mesh);
            byteBuffer.close();
        }

        BlockState axleState = tile.getBlockState();
        if (!(axleState.getBlock() instanceof BlockFlyWheelBase)) return;
        Direction.Axis facingAxis = axleState.getValue(BlockFlyWheelBase.ROTATION_AXIS);

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
        RenderSystem.setShaderTexture(0, tex);

        ShaderInstance shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul( modelMat), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        Uniform NormalMat = shader.getUniform("NormalMat");
        Matrix3f normalMat = new Matrix3f(modelMat); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        tile.vertexBuffer.bind();
        tile.vertexBuffer.draw();

        NormalMat.set(new Matrix3f());
        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();
    }
}