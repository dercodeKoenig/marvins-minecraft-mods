package advRocketry.BlockEntityRenderers;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import advRocketry.BlockEntities.EntityObservatory;
import advRocketry.Blocks.Observatory;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Main;
import advRocketry.utils.AxisDirections;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

import static net.minecraft.client.renderer.RenderStateShard.*;
import static net.minecraft.client.renderer.RenderStateShard.NO_TRANSPARENCY;

public class RenderObservatory implements BlockEntityRenderer<EntityObservatory> {


    static WavefrontObject model;
    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/observatory.png");

    static VertexFormat POSITION_COLOR_TEXTURE_NORMAL_LIGHT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("Color", VertexFormatElement.COLOR).add("UV0", VertexFormatElement.UV0).add("UV1", VertexFormatElement.UV1).add("UV2", VertexFormatElement.UV2).add("Normal", VertexFormatElement.NORMAL).build();

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "models/block/obj/observatory.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    public RenderObservatory(BlockEntityRendererProvider.Context c) {
        super();
    }


    public void updateVertexBuffers(EntityObservatory tile, int light) {
        ByteBufferBuilder byteBuffer;
        BufferBuilder b;

        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("Axis").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.meshAxle = b.build();
        tile.axle.bind();
        tile.axle.upload(tile.meshAxle);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("Scope").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.meshScope = b.build();
        tile.scope.bind();
        tile.scope.upload(tile.meshScope);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("CasingXPlus").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.meshCasingXPlus = b.build();
        tile.casingXPlus.bind();
        tile.casingXPlus.upload(tile.meshCasingXPlus);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("CasingXMinus").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.meshCasingXMinus = b.build();
        tile.casingXMinus.bind();
        tile.casingXMinus.upload(tile.meshCasingXMinus);
        byteBuffer.close();


        byteBuffer = new ByteBufferBuilder(1024);
        b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face i : model.groupObjects.get("Base").faces) {
            i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
        }
        tile.meshBase = b.build();
        tile.base.bind();
        tile.base.upload(tile.meshBase);
        byteBuffer.close();
    }

    public Pair<Float, Float> getYawAndPitch(EntityObservatory observatory, float partialTick) {
        float yaw = 0f;
        float pitch = 0f;

        Dimension targetDim = DimensionManager.INSTANCE_CLIENT.get(observatory.taskTarget);
        Dimension myDim = DimensionManager.INSTANCE_CLIENT.get(observatory.getLevel().dimension().location());

        if (targetDim != null && myDim != null && myDim != targetDim) {
            // try to look to target space object

            Vec3 targetPos = targetDim.getPosition(partialTick);
            Vec3 myPos = myDim.getPosition(partialTick);

            Vector3f relative = targetPos.subtract(myPos).toVector3f();

            AxisDirections myGlobalAxis = myDim.getGlobalAxisDirections(partialTick);

            Matrix4f worldMatrix = new Matrix4f().lookAt(
                    new Vector3f(0, 0, 0),
                    myGlobalAxis.front.toVector3f(),
                    myGlobalAxis.up.toVector3f()
            );
            Vector3f relativeWorldSpace = worldMatrix.transformDirection(relative);

            // Horizon check: If Y is positive, it's above the horizon
            if (relativeWorldSpace.y > 0) {
                // Since the model faces West (-X) by default:
                // We use Z for the first parameter (the "y" in standard atan2)
                // We use -X for the second parameter (the "x" in standard atan2)
                yaw = (float) Math.atan2(relativeWorldSpace.z, -relativeWorldSpace.x);

                // Math.asin(y) gives the elevation angle above the XZ plane
                pitch = (float) Math.asin(relativeWorldSpace.y);

                return Pair.of(yaw, pitch);
            }
        }

        return Pair.of(yaw, pitch);
    }


    @Override
    public void render(EntityObservatory observatory, float partialtick, PoseStack stack, MultiBufferSource multiBufferSource, int light, int overlay) {

        if (observatory.lastLight != light) {
            observatory.lastLight = light;
            updateVertexBuffers(observatory, light);
        }

        BlockState state = observatory.getBlockState();
        if (!(state.getBlock() instanceof Observatory)) return;

        if (!state.getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED))
            return;

        Direction back = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();
        RenderSystem.setShaderTexture(0, tex);

        Matrix4f modelMat = new Matrix4f();
        modelMat = modelMat.mul(stack.last().pose());

        // translate to structure center
        modelMat.translate(back.getStepX() * 2 + 0.5f, 0, back.getStepZ() * 2 + 0.5f);

        ShaderInstance shader;
        Uniform NormalMat;
        Matrix3f normalMat;

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMat), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMat); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        observatory.base.bind();
        observatory.base.draw();

        Pair<Float, Float> yaw_pitch = getYawAndPitch(observatory, partialtick);
        float yaw = yaw_pitch.getFirst();
        float pitch = yaw_pitch.getSecond();

        Matrix4f modelMatScope = new Matrix4f(modelMat);
        modelMatScope.translate(0, 2, 0);
        modelMatScope.rotateY(yaw);       // Spin the axle around Y
        modelMatScope.rotateZ(-pitch);       // Spin the axle around Y

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMatScope), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMatScope); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        observatory.scope.bind();
        observatory.scope.draw();

        observatory.axle.bind();
        observatory.axle.draw();


        float openProgress = (float) observatory.openingTicks / observatory.openingTicksMax;
        if (observatory.should_open) openProgress += partialtick / observatory.openingTicksMax;
        if (!observatory.should_open) openProgress -= partialtick / observatory.openingTicksMax;
        openProgress = Math.clamp(openProgress, 0, 1);


        Matrix4f modelMatCaseXPlus = new Matrix4f(modelMat);
        modelMatCaseXPlus.rotateY(yaw);

        // open
        modelMatCaseXPlus.translate(0f, 0, openProgress);

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMatCaseXPlus), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMatCaseXPlus); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        observatory.casingXPlus.bind();
        observatory.casingXPlus.draw();


        Matrix4f modelMatCaseXMinus = new Matrix4f(modelMat);
        modelMatCaseXMinus.rotateY(yaw);

        // open
        modelMatCaseXMinus.translate(0f, 0, -openProgress);

        shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMatCaseXMinus), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        normalMat = new Matrix3f(modelMatCaseXMinus); // take upper-left 3x3
        normalMat.invert().transpose(); // compute normal matrix
        NormalMat.set(normalMat);
        shader.apply();

        observatory.casingXMinus.bind();
        observatory.casingXMinus.draw();

        // i think i need to reset it or there might be problems. dont know exactly why but minecraft doesnt know / care about normal mat
        NormalMat.set(new Matrix3f());
        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();

    }
}
