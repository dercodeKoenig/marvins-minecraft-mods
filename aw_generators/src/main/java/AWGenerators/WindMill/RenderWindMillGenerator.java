package AWGenerators.WindMill;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import AWGenerators.Main;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static AgeOfSteam.Static.*;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderWindMillGenerator implements BlockEntityRenderer<EntityWindMillGenerator> {

    static WavefrontObject model;
    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/windmill_generator.png");

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/windmill_generator.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void updateWindmillMesh(EntityWindMillGenerator tile, int size, int light) {
        if (size < 1) return;

        ByteBufferBuilder byteBuffer;
        BufferBuilder b;

        byteBuffer = new ByteBufferBuilder(4096);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);

        for (int p = 0; p < size; p++) {
            for (int n = 0; n < 4; n++) {
                for (Face i : model.groupObjects.get("blade").faces) {
                    PoseStack poseStack = new PoseStack();
                    // Apply the equivalent transformations
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90f * n));  // rotate around Z
                    poseStack.translate(0.0, 1.0f * p, 0.0);              // move upward by p

                    i.addFaceForRender(poseStack, b, light, 0, 0xffffffff);
                }
            }
        }

        tile.mesh = b.build();
        tile.vertexBuffer.bind();
        tile.vertexBuffer.upload(tile.mesh);
        byteBuffer.close();
    }


    public RenderWindMillGenerator(BlockEntityRendererProvider.Context c) {
        super();
    }

    public AABB getRenderBoundingBox(EntityWindMillGenerator tile) {
        return new AABB(tile.getBlockPos()).inflate(tile.size + 2);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }


    @Override
    public void render(EntityWindMillGenerator tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.lastLight != packedLight) {
tile.lastLight = packedLight;
            ByteBufferBuilder byteBuffer;
            BufferBuilder b;

            byteBuffer = new ByteBufferBuilder(4096);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("wheel").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_wheel = b.build();
            tile.vertexBuffer_wheel.bind();
            tile.vertexBuffer_wheel.upload(tile.mesh_wheel);
            byteBuffer.close();

            byteBuffer = new ByteBufferBuilder(4096);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("axle").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_axle = b.build();
            tile.vertexBuffer_axle.bind();
            tile.vertexBuffer_axle.upload(tile.mesh_axle);
            byteBuffer.close();

            updateWindmillMesh(tile, tile.size, packedLight);

        }

        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof BlockWindMillGenerator)) return;

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
        m2 = m2.translate(0, 0, -0.11f);
        m2 = m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, rotationMultiplier, (float) (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick)));
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.apply();
        tile.vertexBuffer_wheel.bind();
        tile.vertexBuffer_wheel.draw();


        if (tile.getBlockState().getValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED)) {
            tile.vertexBuffer_axle.bind();
            tile.vertexBuffer_axle.draw();

            if (tile.size != tile.last_size_for_meshUpdate) {
                updateWindmillMesh(tile, tile.size, packedLight);
                tile.last_size_for_meshUpdate = tile.size;
            }

            if (tile.size > 0) {
                tile.vertexBuffer.bind();
                tile.vertexBuffer.draw();
            }
        }

        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();
    }
}