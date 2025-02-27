package AWGenerators.WaterWheel;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import AWGenerators.Main;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderWaterWheelGenerator implements BlockEntityRenderer<EntityWaterWheelGenerator> {

    static WavefrontObject model;
    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/planks.png");

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/waterwheel_generator.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }

    }

    public RenderWaterWheelGenerator(BlockEntityRendererProvider.Context c) {
        super();
    }

    public AABB getRenderBoundingBox(EntityWaterWheelGenerator tile) {
        return new AABB(tile.getBlockPos()).inflate(1);
    }


    @Override
    public void render(EntityWaterWheelGenerator tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.lastLight != packedLight) {
            tile.lastLight = packedLight;
            ByteBufferBuilder byteBuffer;
            BufferBuilder b;
            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("wheel").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh = b.build();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.upload(tile.mesh);
            byteBuffer.close();
        }

        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof BlockWaterWheelGenerator)) return;
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        Matrix4f m1 = new Matrix4f(RenderSystem.getModelViewMatrix());
        m1 = m1.mul(stack.last().pose());
        m1 = m1.translate(0.5f, 0.5f, 0.5f);
        float rotationMultiplier = 0;

        if (facing == Direction.WEST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 90f));
            rotationMultiplier = 1;
        }
        if (facing == Direction.EAST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 270f));
            rotationMultiplier = -1;
        }
        if (facing == Direction.SOUTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 180f));
            rotationMultiplier = -1;
        }
        if (facing == Direction.NORTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 0f));
            rotationMultiplier = 1;
        }


        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        ShaderInstance shader = RenderSystem.getShader();
        RenderSystem.setShaderTexture(0, tex);

        Matrix4f m2 = new Matrix4f(m1);
        m2 = m2.translate(0, 0, -0.2f);
        m2 = m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, rotationMultiplier, (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick)));
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
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