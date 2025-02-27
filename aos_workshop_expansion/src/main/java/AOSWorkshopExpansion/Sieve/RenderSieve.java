package AOSWorkshopExpansion.Sieve;

import AOSWorkshopExpansion.Main;
import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase;
import AgeOfSteam.Static;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.POSITION_COLOR_TEXTURE_NORMAL_LIGHT;
import static AgeOfSteam.Static.TPS;
import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderSieve implements BlockEntityRenderer<EntitySieve> {

    static WavefrontObject model;
    static ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/block/planks.png");

    static {
        try {
            model = new WavefrontObject(ResourceLocation.fromNamespaceAndPath(Main.MODID, "objmodels/mechanical_sieve.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    void updateRenderData(EntitySieve tile, int light) {
        if (tile.myInputs.getItem() instanceof BlockItem bi) {
            Block block = bi.getBlock();
            BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
            TextureAtlasSprite blockTexture = blockRenderer.getBlockModel(block.defaultBlockState()).getParticleIcon(ModelData.EMPTY);
            tile.inputStackTexture = blockTexture.atlasLocation();
            model.scaleUV("sieve.001", blockTexture.getU0(), blockTexture.getV0(), blockTexture.getU1(), blockTexture.getV1());
            ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
            BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("sieve.001").faces) {
                i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
            }
            model.scaleUV("sieve.001", 0, 0, 1, 1);
            MeshData mesh = b.build();
            tile.myInputRendererBuffer.bind();
            tile.myInputRendererBuffer.upload(mesh);
            byteBuffer.close();
        }

        if (tile.myHopperInputs.getItem() instanceof BlockItem bi) {
            Block block = bi.getBlock();
            BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
            TextureAtlasSprite blockTexture = blockRenderer.getBlockModel(block.defaultBlockState()).getParticleIcon(ModelData.EMPTY);
            tile.hopperStackTexture = blockTexture.atlasLocation();
            model.scaleUV("hopper_plane", blockTexture.getU0(), blockTexture.getV0(), blockTexture.getU1(), blockTexture.getV1());
            ByteBufferBuilder byteBuffer = new ByteBufferBuilder(1024);
            BufferBuilder b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("hopper_plane").faces) {
                i.addFaceForRender(new PoseStack(), b, light, 0, 0xffffffff);
            }
            model.scaleUV("hopper_plane", 0, 0, 1, 1);
            MeshData mesh = b.build();
            tile.myHopperInputRendererBuffer.bind();
            tile.myHopperInputRendererBuffer.upload(mesh);
            byteBuffer.close();
        }
    }

    public RenderSieve(BlockEntityRendererProvider.Context c) {
        super();
    }

    @Override
    public void render(EntitySieve tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.lastLight != packedLight) {
            tile.lastLight = packedLight;


            ByteBufferBuilder byteBuffer;
            BufferBuilder b;


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("sieve.001").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh2 = b.build();
            tile.vertexBuffer2.bind();
            tile.vertexBuffer2.upload(tile.mesh2);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("sieve").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh = b.build();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.upload(tile.mesh);
            byteBuffer.close();


            byteBuffer = new ByteBufferBuilder(1024);
            b = new BufferBuilder(byteBuffer, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            for (Face i : model.groupObjects.get("arm").faces) {
                i.addFaceForRender(new PoseStack(), b, packedLight, 0, 0xffffffff);
            }
            tile.mesh3 = b.build();
            tile.vertexBuffer3.bind();
            tile.vertexBuffer3.upload(tile.mesh3);
            byteBuffer.close();
        }


        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof BlockSieve)) return;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        Matrix4f m1 = new Matrix4f(RenderSystem.getModelViewMatrix());
        m1 = m1.mul(stack.last().pose());
        m1 = m1.translate(0.5f, 0.5f, 0.5f);

        if (facing == Direction.WEST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 0f));
        }
        if (facing == Direction.EAST) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 180f));

        }
        if (facing == Direction.SOUTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 90f));
        }
        if (facing == Direction.NORTH) {
            m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 270f));
        }


        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        NO_TRANSPARENCY.setupRenderState();

        RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        ShaderInstance shader = RenderSystem.getShader();
        RenderSystem.setShaderTexture(0, tex);

        Matrix4f m2 = new Matrix4f(m1);
        float crankshaftR = 0.07f;
        double targetHeight = 0.03;
        double armLength = 0.62;
        float XRotationMultiplier =
                (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1)
                        * (facing.getAxis() == Direction.Axis.X ? 1 : -1);

        double a = tile.myMechanicalBlock.currentRotation / 180 * Math.PI + tile.myMechanicalBlock.internalVelocity / TPS * partialTick;
        float translationX = -1f + (float) Math.sin(a) * crankshaftR * XRotationMultiplier;
        float translationY = (float) Math.cos(a) * crankshaftR;
        double b = Math.asin((translationY - targetHeight) / armLength);
        m2.translate(translationX, translationY, -0.04f);
        m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, -(float) b * 180f / (float) Math.PI));
        m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, 180f));

        BlockEntity t = tile.getLevel().getBlockEntity(tile.getBlockPos().relative(facing));
        if (t instanceof EntityCrankShaftBase cs) {
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.apply();
            tile.vertexBuffer3.bind();
            tile.vertexBuffer3.draw();
        }

        m2 = new Matrix4f(m1);
        float sieveTargetX = 0.4f + (float) (translationX + Math.cos(b) * armLength);
        m2.translate(sieveTargetX, 0, 0);

        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.apply();
        tile.vertexBuffer.bind();
        tile.vertexBuffer.draw();


        if (tile.myMesh.getItem() instanceof IMesh mesh) {

            Matrix4f m4 = new Matrix4f(m2);

            Matrix4f m3 = new Matrix4f(m2);
            m3.translate(0, -0.01f, 0);
            m3.rotate(new Quaternionf().fromAxisAngleDeg(1f, 0f, 0f, 180f));

            Matrix4f m5 = new Matrix4f(m3);


            if (tile.myInputs.getItem() instanceof BlockItem bi) {
                if (!tile.lastInputStackForRender.getItem().equals(tile.myInputs.getItem())) {
                    updateRenderData(tile, packedLight);
                    tile.lastInputStackForRender = tile.myInputs.copy();
                }
                RenderSystem.setShaderTexture(0, tile.inputStackTexture);
                float maxTranslateUp = 0.065f;
                float translateUp = (float) (((float) tile.myInputs.getCount() - tile.currentProgress / tile.client_syncedCurrentRecipeTime) / tile.maxStackSizeForSieve * maxTranslateUp + 0.001f);
                m2.translate(0, translateUp, 0);
                shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
                shader.apply();
                tile.myInputRendererBuffer.bind();
                tile.myInputRendererBuffer.draw();

                // i know in this one the normal is the wrong direction but nobody will look from below so i really dont care
                m3.translate(0, -0.01f, 0);
                shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m3, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
                shader.apply();
                tile.myInputRendererBuffer.bind();
                tile.myInputRendererBuffer.draw();
            }

            if (tile.myHopperInputs.getItem() instanceof BlockItem bi) {

                if (!tile.lastHopperInputStackForRender.getItem().equals(tile.myHopperInputs.getItem())) {
                    updateRenderData(tile, packedLight);
                    tile.lastHopperInputStackForRender = tile.myHopperInputs.copy();
                }
                RenderSystem.setShaderTexture(0, tile.hopperStackTexture);
                float baseOffset = 0.37f;
                float maxTranslateUp = 0.49f - baseOffset;
                float translateUp = (float) (((float) tile.myHopperInputs.getCount()) / tile.maxStackSizeForSieveHopper * maxTranslateUp + baseOffset);
                m2 = new Matrix4f(m1);
                m2.translate(0, translateUp, 0);
                shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
                shader.apply();
                tile.myHopperInputRendererBuffer.bind();
                tile.myHopperInputRendererBuffer.draw();
            }

            RenderSystem.setShader(GameRenderer::getRendertypeEntityCutoutShader);
            shader = RenderSystem.getShader();
            RenderSystem.setShaderTexture(0, mesh.getTexture());
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, m4, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.apply();

            tile.vertexBuffer2.bind();
            tile.vertexBuffer2.draw();
        }


        shader.clear();
        VertexBuffer.unbind();

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        NO_TRANSPARENCY.clearRenderState();
    }
}