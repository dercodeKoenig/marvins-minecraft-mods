package BetterPipes;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
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
import org.joml.Vector3f;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderTank implements BlockEntityRenderer<EntityTank> {

    static float e = 0.001f;

    static VertexFormat POSITION_COLOR_TEXTURE_NORMAL_LIGHT =
            VertexFormat.builder()
                    .add("Position", VertexFormatElement.POSITION)
                    .add("Color", VertexFormatElement.COLOR)
                    .add("UV0", VertexFormatElement.UV0)
                    .add("UV1", VertexFormatElement.UV1)
                    .add("UV2", VertexFormatElement.UV2)
                    .add("Normal", VertexFormatElement.NORMAL)
                    .build();

    public RenderTank(BlockEntityRendererProvider.Context c) {
        super();
    }

    public static void renderFluidCubeStill(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            int color,
            VertexConsumer v, int light, int overlay
    ) {


        //render up face
        v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

        //render bottom face
        v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);

        // Render east face (x+ side)
        v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);

        // Render west face (x- side)
        v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        v.addVertex((float) x0, (float) y0, z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

        // Render south face (z+ side)
        v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

        // Render north face (z- side)
        v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);

    }


    @Override
    public void render(EntityTank tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        tile.vertexBuffer.bind();


        RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER.setupRenderState();
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        TRANSLUCENT_TRANSPARENCY.setupRenderState();

        RenderSystem.setShaderTexture(0, tile.spriteStill.atlasLocation());


        if (tile.requiresMeshUpdate || tile.lastLight != packedLight) {
            tile.requiresMeshUpdate = false;
            tile.lastLight = packedLight;
            if (!tile.myTank.isEmpty()) {
                float relativeFill = (float) tile.myTank.getFluidAmount() / tile.myTank.getCapacity();
                float y0 = e;
                float y1 = y0 + relativeFill * (1 - 2 * e);
                float x0 = e + 2f / 16;
                float x1 = 1 - (e + 2f / 16);
                float z0 = e + 2f / 16;
                float z1 = 1 - (e + 2f / 16);

                BufferBuilder bufferBuilder = new BufferBuilder(tile.myByteBuffer, VertexFormat.Mode.QUADS, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
                renderFluidCubeStill(x0, x1, z0, z1, y0, y1,
                        tile.spriteStill.getU0(), tile.spriteStill.getU1(), tile.spriteStill.getV0(), tile.spriteStill.getV1(),
                        tile.color, bufferBuilder, packedLight, packedOverlay);

                tile.mesh = bufferBuilder.build();
                if (tile.mesh != null) {
                    tile.vertexBuffer.upload(tile.mesh);
                }
            } else {
                tile.mesh = null;
            }
        }

        if (tile.mesh != null) {
            ShaderInstance shader = RenderSystem.getShader();
            Matrix4f m1 = new Matrix4f(RenderSystem.getModelViewMatrix());
            Matrix4f m2 = m1.mul(stack.last().pose());
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.apply();
            tile.vertexBuffer.draw();
            shader.clear();
        }

        VertexBuffer.unbind();

        RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER.clearRenderState();
        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        TRANSLUCENT_TRANSPARENCY.clearRenderState();
    }
}