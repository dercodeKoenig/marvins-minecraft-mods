package AgeOfSteam.Blocks.Mechanics.DistributorGearbox;

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

public abstract class RenderDistributorGearboxBase implements BlockEntityRenderer<EntityDistributorGearboxBase> {

    static WavefrontObject model;
    static ResourceLocation tex;

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/distributor_gearbox.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }

    }


    public RenderDistributorGearboxBase(BlockEntityRendererProvider.Context c, ResourceLocation texture) {
        super();
        this.tex = texture;
    }

    @Override
    public void render(EntityDistributorGearboxBase tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

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

        BlockState myState = tile.getBlockState();
        if (!(myState.getBlock() instanceof BlockDistributorGearboxbase)) return;
        Direction.Axis normalAxis = myState.getValue(BlockDistributorGearboxbase.ROTATION_AXIS);

        Matrix4f m1 = new Matrix4f(RenderSystem.getModelViewMatrix());
        m1 = m1.mul(stack.last().pose());

        m1 = m1.translate(0.5f, 0.5f, 0.5f);
        if (normalAxis == Direction.Axis.Y) {
            // no rotation
        } else if (normalAxis == Direction.Axis.X) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0, 0, 1f, 90));
        } else if (normalAxis == Direction.Axis.Z) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0, 0, 1f, 90));
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(1f, 0, 0, 90));
        }

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();
        RenderSystem.setShaderTexture(0, tex);

        ShaderInstance shader = RenderSystem.getShader();
        tile.vertexBuffer.bind();

        for (int i = 0; i < 4; i++) {
            Matrix4f m2 = new Matrix4f(m1);
            m2 = m2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 1, 0f, (float) 90 * i));

            if (i == 0)
                m2 = m2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f, (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick)));
            if (i == 1)
                m2 = m2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f, 14.7f - (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick)));
            if (i == 2)
                m2 = m2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f, (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick)));
            if (i == 3)
                m2 = m2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f, 14.7f - (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick)));

            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.apply();
            tile.vertexBuffer.draw();
        }
        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();
    }
}