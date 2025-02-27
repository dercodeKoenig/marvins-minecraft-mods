package AOSBasicFluid.Pump;

import AOSBasicFluid.Main;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static AgeOfSteam.Static.POSITION_COLOR_TEXTURE_NORMAL_LIGHT;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderPump implements BlockEntityRenderer<EntityPump> {

    static WavefrontObject model;
    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/pump.png");

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/pump.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }


    public RenderPump(BlockEntityRendererProvider.Context c) {
        super();
    }

    public AABB getRenderBoundingBox(EntityPump tile) {
        return new AABB(tile.getBlockPos()).inflate(1);
    }

    @Override
    public void render(EntityPump tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {


        if (packedLight != tile.lastLight) {
            tile.lastLight = packedLight;

            ByteBufferBuilder byteBuffer;
            BufferBuilder b;

            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("arm1").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.meshArm1 = b.build();
            tile.vertexBufferArm1.bind();
            tile.vertexBufferArm1.upload(tile.meshArm1);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("arm2").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.meshArm2 = b.build();
            tile.vertexBufferArm2.bind();
            tile.vertexBufferArm2.upload(tile.meshArm2);
            byteBuffer.close();

            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("arm3").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.meshArm3 = b.build();
            tile.vertexBufferArm3.bind();
            tile.vertexBufferArm3.upload(tile.meshArm3);
            byteBuffer.close();


        }

        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof BlockPump)) return;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        stack.translate(0.5f, 0.5f, 0.5f);
        Matrix4f m1 = new Matrix4f(RenderSystem.getModelViewMatrix());
        m1 = m1.mul(stack.last().pose());

        // this is rotations mess up the calculations for the angles later. no idea exactly why but i tried around with this values and it seems to work now
        double directionMultiplier = 1;
        double axisMultiplier = 1;
        if (facing == Direction.WEST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 0f));
            directionMultiplier = -1;
            axisMultiplier = -1;
        }
        if (facing == Direction.EAST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 180f));
            axisMultiplier = -1;
        }
        if (facing == Direction.SOUTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 270f));
        }
        if (facing == Direction.NORTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 90f));
            directionMultiplier = -1;
        }

        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        ShaderInstance shader = RenderSystem.getShader();
        RenderSystem.setShaderTexture(0, tex);

        double rotation = (tile.myMechanicalBlock.currentRotation + Static.rad_to_degree(tile.myMechanicalBlock.internalVelocity) / Static.TPS * partialTick) * directionMultiplier;

        double crankLen = 0.112;
        double crankAngle = rotation / 180 * Math.PI;
        double l1 = 1.22;
        double l2 = 1.0;

        // crankshaft point
        double A_x = crankLen * Math.sin(crankAngle);
        double A_y = crankLen * Math.cos(crankAngle);

        // point b relative to crankshaft center
        double B_x = -1 * axisMultiplier;
        double B_y = 1.2;

        double dx = B_x - A_x;
        double dy = B_y - A_y;
        double d = Math.hypot(dx, dy);

        double a_val = (l1 * l1 - l2 * l2 + d * d) / (2 * d);
        double h = Math.sqrt(Math.max(l1 * l1 - a_val * a_val, 0));

        double base_x = A_x + a_val * dx / d;
        double base_y = A_y + a_val * dy / d;

        double P1_x = base_x + h * (-dy / d);
        double P1_y = base_y + h * (dx / d);
        double P2_x = base_x - h * (-dy / d);
        double P2_y = base_y - h * (dx / d);
        double P_x = 0;
        double P_y = 0;

        if (P1_y >= P2_y) {
            P_x = P1_x;
            P_y = P1_y;
        } else {
            P_x = P2_x;
            P_y = P2_y;
        }

        double a_angle = Math.atan2(P_x - A_x, P_y - A_y);
        double b_angle = Math.atan2(P_y - B_y, P_x - B_x);


        Matrix4f m2 = new Matrix4f(m1);
        m2.translate(new Vector3f((float) (P_x + (axisMultiplier)), (float) (P_y), 0f));
        // the difference in P_x is almost not visible because the angle is just too small but i still keep it now
        // maybe it can be used in other machines...
        // it looks almost like the following:
        //m2.translate(new Vector3f((float)  (0+ (axisMultiplier)), (float) (P_y), 0f));

        m2 = m2.rotate(new Quaternionf().fromAxisAngleRad(0f, 0f, -1f, (float) a_angle));

        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        //shader.getUniform("NormalMatrix").set((new Matrix3f(m2)).invert().transpose());
        //shader.getUniform("UV2").set(packedLight & '\uffff', packedLight >> 16 & '\uffff');
        shader.apply();
        tile.vertexBufferArm3.bind();
        tile.vertexBufferArm3.draw();

        m2 = new Matrix4f(m1);
        m2.translate(new Vector3f((float) 0, (float) (B_y), 0f));
        m2 = m2.rotate(new Quaternionf().fromAxisAngleRad(0f, 0f, (float) 1, (float) b_angle));

        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        //shader.getUniform("NormalMatrix").set((new Matrix3f(m2)).invert().transpose());
        //shader.getUniform("UV2").set(packedLight & '\uffff', packedLight >> 16 & '\uffff');
        shader.apply();
        tile.vertexBufferArm1.bind();
        tile.vertexBufferArm1.draw();


        m2 = new Matrix4f(m1);
        // I choose to not rotate the second arm
        double C_y = B_y + Math.sin(-b_angle) * l2;
        m2.translate(new Vector3f((float) (-0.95 * axisMultiplier), (float) (C_y), 0f));

        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        //shader.getUniform("NormalMatrix").set((new Matrix3f(m2)).invert().transpose());
        //shader.getUniform("UV2").set(packedLight & '\uffff', packedLight >> 16 & '\uffff');
        shader.apply();
        tile.vertexBufferArm2.bind();
        tile.vertexBufferArm2.draw();


        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();
    }
}