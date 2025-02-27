package AgeOfSteam.Blocks.Mechanics.CrankShaft;

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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderSmallCrankShaftBase implements BlockEntityRenderer<EntityCrankShaftBase> {

    static WavefrontObject model;
    public ResourceLocation tex;

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/small_crankshaft.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    public RenderSmallCrankShaftBase(BlockEntityRendererProvider.Context c, ResourceLocation texture) {
        super();
        this.tex = texture;
    }


    @Override
    public void render(EntityCrankShaftBase tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.lastLight != packedLight) {
            tile.lastLight = packedLight;
            ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
            BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("axle").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh = b.build();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.upload(tile.mesh);
            byteBuffer.close();
        }

        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof BlockCrankShaftBase)) return;
        Direction.Axis facingAxis = state.getValue(BlockCrankShaftBase.ROTATION_AXIS);

        Matrix4f m1 = new Matrix4f(RenderSystem.getModelViewMatrix());
        m1 = m1.mul(stack.last().pose());

        m1 = m1.translate(0.5f, 0.5f, 0.5f);

        if (facingAxis == Direction.Axis.Z) {
            // no rotation
        } else if (facingAxis == Direction.Axis.X) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0, 1f, 0, 90));
        } else if (facingAxis == Direction.Axis.Y) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(1f, 0, 0, -90));
        }

        m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f, (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick)));
        //System.out.println(tile.currentRotation);

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();
        RenderSystem.setShaderTexture(0, tex);

        ShaderInstance shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m1, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
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