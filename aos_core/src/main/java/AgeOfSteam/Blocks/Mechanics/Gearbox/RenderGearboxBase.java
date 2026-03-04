package AgeOfSteam.Blocks.Mechanics.Gearbox;

import ARLib.mixins.ShaderInstanceMixin;
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

public abstract class RenderGearboxBase implements BlockEntityRenderer<EntityGearboxBase> {

    WavefrontObject model;
    ResourceLocation tex;


    public RenderGearboxBase(BlockEntityRendererProvider.Context c, ResourceLocation texture) {
        super();
        this.tex = texture;
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/gearbox.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }


    @Override
    public void render(EntityGearboxBase tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.lastLight != packedLight) {
            tile.lastLight = packedLight;
            ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
            BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("small_output").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_in = b.build();
            tile.vertexBuffer_in.bind();
            tile.vertexBuffer_in.upload(tile.mesh_in);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("big_output").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.vertexBuffer_out.bind();
            tile.mesh_out = b.build();
            tile.vertexBuffer_out.upload(tile.mesh_out);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(2048);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("connection").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh_mid = b.build();
            tile.vertexBuffer_mid.bind();
            tile.vertexBuffer_mid.upload(tile.mesh_mid);
            byteBuffer.close();
        }

        BlockState myState = tile.getBlockState();
        if (!(myState.getBlock() instanceof BlockGearboxBase)) return;

        Direction facing = myState.getValue(BlockGearboxBase.FACING);

        Matrix4f modelMat = new Matrix4f();
        modelMat = modelMat.mul(stack.last().pose());
        modelMat = modelMat.translate(0.5f, 0.5f, 0.5f);

        double facingBasedRotationMultiplier = 1;
        if (facing == Direction.NORTH) {
            // all good
        }
        if (facing == Direction.SOUTH) {
            facingBasedRotationMultiplier = -1;
            modelMat = modelMat.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 1, 0f, (float) 180));
        }
        if (facing == Direction.EAST) {
            facingBasedRotationMultiplier = -1;
            modelMat = modelMat.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 1, 0f, (float) 270));
        }
        if (facing == Direction.WEST) {
            modelMat = modelMat.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 1, 0f, (float) 90));
        }

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();
        RenderSystem.setShaderTexture(0, tex);

        ShaderInstance shader = RenderSystem.getShader();

        Matrix4f modelMat2 = new Matrix4f(modelMat);
        modelMat2 = modelMat2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 1, 0f, (float) 0));

        modelMat2 = modelMat2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f,
                (float) (facingBasedRotationMultiplier * tile.myMechanicalBlock.getRotationMultiplierToOutside(facing) * (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick))));


        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMat2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        Uniform NormalMat = shader.getUniform("NormalMat");
        if(NormalMat != null)
            NormalMat.set(Static.getNormalMat(modelMat2));
        shader.apply();

        tile.vertexBuffer_in.bind();
        tile.vertexBuffer_in.draw();


        modelMat2 = new Matrix4f(modelMat);
        modelMat2 = modelMat2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 1, 0f, (float) 0));

        modelMat2 = modelMat2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f,
                (float) (facingBasedRotationMultiplier * tile.myMechanicalBlock.getRotationMultiplierToOutside(facing.getOpposite()) * (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick))));

        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMat2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        if(NormalMat != null)
            NormalMat.set(Static.getNormalMat(modelMat2));
        shader.apply();

        tile.vertexBuffer_out.bind();
        tile.vertexBuffer_out.draw();
        //shader.clear();


        modelMat2 = new Matrix4f(modelMat);
        modelMat2 = modelMat2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 1, 0f, (float) 0));
        modelMat2 = modelMat2.translate(0.3f, 0, 0);

        modelMat2 = modelMat2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f,
                (float) (facingBasedRotationMultiplier * (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick))));

        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMat2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        if(NormalMat != null)
            NormalMat.set(Static.getNormalMat(modelMat2));
        shader.apply();

        tile.vertexBuffer_mid.bind();
        tile.vertexBuffer_mid.draw();
        //shader.clear();

        modelMat2 = new Matrix4f(modelMat);
        modelMat2 = modelMat2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 1, 0f, (float) 0));
        modelMat2 = modelMat2.translate(-0.3f, 0, 0);

        modelMat2 = modelMat2.rotate(new Quaternionf().fromAxisAngleDeg((float) 0, (float) 0, 1.0f,
                (float) (facingBasedRotationMultiplier * (tile.myMechanicalBlock.currentRotation + rad_to_degree(tile.myMechanicalBlock.internalVelocity) / TPS * partialTick))));

        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelMat2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        NormalMat = shader.getUniform("NormalMat");
        if(NormalMat != null)
            NormalMat.set(Static.getNormalMat(modelMat2));
        shader.apply();

        tile.vertexBuffer_mid.bind();
        tile.vertexBuffer_mid.draw();


        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();
    }
}