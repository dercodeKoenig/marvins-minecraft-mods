package BetterPipes.Pipe;

import ARLib.obj.Face;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.BlockCrankShaftBase;
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
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static AgeOfSteam.Static.TPS;
import static net.minecraft.client.renderer.RenderStateShard.*;
import static net.minecraft.client.renderer.RenderType.TRANSIENT_BUFFER_SIZE;

public class RenderPipe implements BlockEntityRenderer<EntityPipe> {

    // the order in that you define it is very important!
    // unlike older versions, the order is not linked to .addvertex(...) but has to be like this
    // I dont know why but this is the only way it works
    private static VertexFormat POSITION_COLOR_TEXTURE_NORMAL_LIGHT =
            VertexFormat.builder()
                    .add("Position", VertexFormatElement.POSITION)
                    .add("Color", VertexFormatElement.COLOR)
                    .add("UV0", VertexFormatElement.UV0)
                    .add("UV1", VertexFormatElement.UV1)
                    .add("UV2", VertexFormatElement.UV2)
                    .add("Normal", VertexFormatElement.NORMAL)
                    .build();


    static float e = 0.001f;
    static float wMin = 0.02f;
    static float wMax = 0.25f - e;

    public RenderPipe(BlockEntityRendererProvider.Context c) {
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

    public static void renderFluidCubeFacebyDirection(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            Direction d,
            int color,
            VertexConsumer v, int light, int overlay
    ) {

        if (d == Direction.UP) {
            //render up face
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        }
        if (d == Direction.DOWN) {
            //render bottom face
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        }

        if (d == Direction.EAST) {
            // Render east face (x+ side)
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        }
        if (d == Direction.WEST) {
            // Render west face (x- side)
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        }
        if (d == Direction.SOUTH) {
            // Render south face (z+ side)
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        }
        if (d == Direction.NORTH) {
            // Render north face (z- side)
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        }
    }

    public static void renderVerticalFluidStill(
            float x0f, float x1f, float z0f, float z1f, float y0f, float y1f,
            float u0, float u1, float v0, float v1,
            int color,
            VertexConsumer v, int light, int overlay
    ) {

        // Render east face (x+ side)
        v.addVertex(x1f, y0f, z0f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1f, y1f, z1f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1f, y0f, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);

        // Render west face (x- side)
        v.addVertex(x0f, y0f, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0f, y1f, z1f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0f, y0f, z0f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

        // Render south face (z+ side)#
        v.addVertex(x1f, y0f, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x1f, y1f, z1f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x0f, y1f, z1f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x0f, y0f, z1f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

        // Render north face (z- side)
        v.addVertex(x0f, y0f, z0f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x0f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x1f, y1f, z0f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x1f, y0f, z0f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
    }

    public static void renderHorizontalFluidStill(
            float x0f, float x1f, float z0f, float z1f, float y0f, float y1f,
            float u0, float u1, float v0, float v1,
            int color,
            VertexConsumer v, int light, int overlay,
            float y0BottomOffsetNorth,
            float y0BottomOffsetSouth,
            float y0BottomOffsetEast,
            float y0BottomOffsetWest
    ) {


        y1f = Math.max(y1f, y0f + 5 * e);


        //render top face
        v.addVertex(x0f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x0f, y1f, z1f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x1f, y1f, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x1f, y1f, z0f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

        //render bottom face
        v.addVertex(x1f, y0f, z0f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x1f, y0f, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x0f, y0f, z1f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x0f, y0f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);

        // Render east face (x+ side)
        if (y1f - y0BottomOffsetEast > e) {
            v.addVertex(x1f, y0BottomOffsetEast, z0f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex(x1f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex(x1f, y1f, z1f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex(x1f, y0BottomOffsetEast, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
        }
        // Render west face (x- side)
        if (y1f - y0BottomOffsetWest > e) {
            v.addVertex(x0f, y0BottomOffsetWest, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex(x0f, y1f, z1f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex(x0f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex(x0f, y0BottomOffsetWest, z0f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        }
        // Render south face (z+ side)#
        if (y1f - y0BottomOffsetSouth > e) {
            v.addVertex(x1f, y0BottomOffsetSouth, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex(x1f, y1f, z1f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex(x0f, y1f, z1f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex(x0f, y0BottomOffsetSouth, z1f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        }
        // Render north face (z- side)
        if (y1f - y0BottomOffsetNorth > e) {
            v.addVertex(x0f, y0BottomOffsetNorth, z0f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex(x0f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex(x1f, y1f, z0f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex(x1f, y0BottomOffsetNorth, z0f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        }
    }


    public static void renderHorizontalFluidFlowing(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            int color, Direction flowDirection,
            VertexConsumer v, int light, int overlay,
            float y0BottomOffsetNorth,
            float y0BottomOffsetSouth,
            float y0BottomOffsetEast,
            float y0BottomOffsetWest
    ) {
        y1 = Math.max(y1, y0 + 5 * e);

        if (flowDirection == Direction.NORTH) {
            //render top face
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
            if (y1 - y0BottomOffsetEast > e) {
                v.addVertex((float) x1, (float) y0BottomOffsetEast, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y0BottomOffsetEast, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            }
            // Render west face (x- side)
            if (y1 - y0BottomOffsetWest > e) {
                v.addVertex((float) x0, (float) y0BottomOffsetWest, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y0BottomOffsetWest, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            }
            // Render south face (z+ side)
            if (y1 - y0BottomOffsetSouth > e) {
                v.addVertex((float) x1, (float) y0BottomOffsetSouth, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x0, (float) y0BottomOffsetSouth, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            }
            // Render north face (z- side)
            if (y1 - y0BottomOffsetNorth > e) {
                v.addVertex((float) x0, (float) y0BottomOffsetNorth, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x1, (float) y0BottomOffsetNorth, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            }
        }

        if (flowDirection == Direction.SOUTH) {
            //render top face
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

            //render bottom face
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);

            // Render east face (x+ side)
            if (y1 - y0BottomOffsetEast > e) {
                v.addVertex((float) x1, (float) y0BottomOffsetEast, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y0BottomOffsetEast, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            }
            // Render west face (x- side)
            if (y1 - y0BottomOffsetWest > e) {
                v.addVertex((float) x0, (float) y0BottomOffsetWest, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y0BottomOffsetWest, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            }
            // Render south face (z+ side)
            if (y1 - y0BottomOffsetSouth > e) {
                v.addVertex((float) x1, (float) y0BottomOffsetSouth, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x0, (float) y0BottomOffsetSouth, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            }
            // Render north face (z- side)
            if (y1 - y0BottomOffsetNorth > e) {
                v.addVertex((float) x0, (float) y0BottomOffsetNorth, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x1, (float) y0BottomOffsetNorth, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            }
        }
        if (flowDirection == Direction.EAST) {
            //render top face
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

            //render bottom face
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);

            // Render east face (x+ side)
            if (y1 - y0BottomOffsetEast > e) {
                v.addVertex((float) x1, (float) y0BottomOffsetEast, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y0BottomOffsetEast, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            }
            // Render west face (x- side)
            if (y1 - y0BottomOffsetWest > e) {
                v.addVertex((float) x0, (float) y0BottomOffsetWest, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y0BottomOffsetWest, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            }
            // Render south face (z+ side)
            if (y1 - y0BottomOffsetSouth > e) {
                v.addVertex((float) x1, (float) y0BottomOffsetSouth, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x0, (float) y0BottomOffsetSouth, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            }
            // Render north face (z- side)
            if (y1 - y0BottomOffsetNorth > e) {
                v.addVertex((float) x0, (float) y0BottomOffsetNorth, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x1, (float) y0BottomOffsetNorth, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            }
        }
        if (flowDirection == Direction.WEST) {
            //render top face
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

            //render bottom face
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);

            // Render east face (x+ side)
            if (y1 - y0BottomOffsetEast > e) {
                v.addVertex((float) x1, (float) y0BottomOffsetEast, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                v.addVertex((float) x1, (float) y0BottomOffsetEast, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            }
            // Render west face (x- side)
            if (y1 - y0BottomOffsetWest > e) {
                v.addVertex((float) x0, (float) y0BottomOffsetWest, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
                v.addVertex((float) x0, (float) y0BottomOffsetWest, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            }
            // Render south face (z+ side)
            if (y1 - y0BottomOffsetSouth > e) {
                v.addVertex((float) x1, (float) y0BottomOffsetSouth, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
                v.addVertex((float) x0, (float) y0BottomOffsetSouth, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            }
            // Render north face (z- side)
            if (y1 - y0BottomOffsetNorth > e) {
                v.addVertex((float) x0, (float) y0BottomOffsetNorth, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
                v.addVertex((float) x1, (float) y0BottomOffsetNorth, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            }
        }
    }


    public static void renderHorizontalFluidStillCentered(
            float x0f, float x1f, float z0f, float z1f, float y0f, float y1f,
            float u0, float u1, float v0, float v1,
            int color,
            VertexConsumer v, int light, int overlay,
            Direction direction
    ) {

        //render top face
        v.addVertex(x0f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x0f, y1f, z1f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x1f, y1f, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x1f, y1f, z0f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

        //render bottom face
        v.addVertex(x1f, y0f, z0f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x1f, y0f, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x0f, y0f, z1f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x0f, y0f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);

        if (direction != Direction.EAST && direction != Direction.WEST) {
            // Render east face (x+ side)
            v.addVertex(x1f, y0f, z0f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex(x1f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex(x1f, y1f, z1f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex(x1f, y0f, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);

            // Render west face (x- side)
            v.addVertex(x0f, y0f, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex(x0f, y1f, z1f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex(x0f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex(x0f, y0f, z0f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
        }
        if (direction != Direction.NORTH && direction != Direction.SOUTH) {
            // Render south face (z+ side)#
            v.addVertex(x1f, y0f, z1f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex(x1f, y1f, z1f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex(x0f, y1f, z1f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex(x0f, y0f, z1f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

            // Render north face (z- side)
            v.addVertex(x0f, y0f, z0f).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex(x0f, y1f, z0f).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex(x1f, y1f, z0f).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex(x1f, y0f, z0f).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        }
    }

    public static void renderFluidFlowingCentered(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            int color, Direction flowDirection,
            VertexConsumer v, int light, int overlay
    ) {

        if (flowDirection == Direction.NORTH) {
            // Render east face (x+ side)
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);

            // Render west face (x- side)
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

            // Render up  face (y+ side)
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

            // Render down face (y- side)
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        }

        if (flowDirection == Direction.SOUTH) {
            // Render east face (x+ side)
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);

            // Render west face (x- side)
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

            // Render up  face (y+ side)
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

            // Render down face (y- side)
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        }

        if (flowDirection == Direction.EAST) {
            // Render south face (z+ side)
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

            // Render north face (z- side)
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);

            // Render up  face (y+ side)
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

            // Render down face (y- side)
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        }

        if (flowDirection == Direction.WEST) {
            // Render south face (z+ side)
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

            // Render north face (z- side)
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);

            // Render up  face (y+ side)
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);

            // Render down face (y- side)
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, -1, 0);
        }

        if (flowDirection == Direction.UP) {
            // Render east face (x+ side)
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);

            // Render west face (x- side)
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

            // Render south face (z+ side)
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

            // Render north face (z- side)
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        }

        if (flowDirection == Direction.DOWN) {
            // Render east face (x+ side)
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);

            // Render west face (x- side)
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(-1, 0, 0);

            // Render south face (z+ side)
            v.addVertex((float) x1, (float) y0, (float) z1).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x1, (float) y1, (float) z1).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x0, (float) y1, (float) z1).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
            v.addVertex((float) x0, (float) y0, (float) z1).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);

            // Render north face (z- side)
            v.addVertex((float) x0, (float) y0, (float) z0).setColor(color).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x0, (float) y1, (float) z0).setColor(color).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x1, (float) y1, (float) z0).setColor(color).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
            v.addVertex((float) x1, (float) y0, (float) z0).setColor(color).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        }
    }

    // Helper to interpolate UV coordinates
    static float interpolate(float outerStart, float outerEnd, float innerStart, float innerEnd, float value) {
        return ((value - outerStart) / (outerEnd - outerStart)) * (innerEnd - innerStart) + innerStart;
    }

    static void renderDownFaceCutOut(
            float x0, float x1, float z0, float z1, float y0,
            float xh0, float xh1, float zh0, float zh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light,
            int color) {

// Interpolated UVs for the hole
        float uh0 = interpolate(x0, x1, u0, u1, xh0);
        float uh1 = interpolate(x0, x1, u0, u1, xh1);
        float vh0 = interpolate(z0, z1, v0, v1, zh0);
        float vh1 = interpolate(z0, z1, v0, v1, zh1);


        v.addVertex(x0, y0, z0).setColor(color).setUv(u0, v0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh0, y0, z0).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh0, y0, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x0, y0, zh0).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(0, -1, 0);

        v.addVertex(xh0, y0, z0).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh1, y0, z0).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh1, y0, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh0, y0, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, -1, 0);

        v.addVertex(xh1, y0, z0).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x1, y0, z0).setColor(color).setUv(u1, v0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x1, y0, zh0).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh1, y0, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, -1, 0);

        v.addVertex(xh1, y0, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x1, y0, zh0).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x1, y0, zh1).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh1, y0, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, -1, 0);

        v.addVertex(xh1, y0, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x1, y0, zh1).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x1, y0, z1).setColor(color).setUv(u1, v1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh1, y0, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(0, -1, 0);

        v.addVertex(xh0, y0, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh1, y0, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh1, y0, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh0, y0, z1).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(0, -1, 0);

        v.addVertex(x0, y0, zh1).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh0, y0, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh0, y0, z1).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x0, y0, z1).setColor(color).setUv(u0, v1).setOverlay(0).setLight(light).setNormal(0, -1, 0);

        v.addVertex(x0, y0, zh0).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh0, y0, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(xh0, y0, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
        v.addVertex(x0, y0, zh1).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(0, -1, 0);
    }

    static void renderUpFaceCutOut(
            float x0, float x1, float z0, float z1, float y1,
            float xh0, float xh1, float zh0, float zh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light,
            int color) {

// Interpolated UVs for the hole
        float uh0 = interpolate(x0, x1, u0, u1, xh0);
        float uh1 = interpolate(x0, x1, u0, u1, xh1);
        float vh0 = interpolate(z0, z1, v0, v1, zh0);
        float vh1 = interpolate(z0, z1, v0, v1, zh1);


        v.addVertex(x0, y1, zh0).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh0, y1, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh0, y1, z0).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x0, y1, z0).setColor(color).setUv(u0, v0).setOverlay(0).setLight(light).setNormal(0, 1, 0);

        v.addVertex(xh0, y1, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh1, y1, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh1, y1, z0).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh0, y1, z0).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(0, 1, 0);

        v.addVertex(xh1, y1, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x1, y1, zh0).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x1, y1, z0).setColor(color).setUv(u1, v0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh1, y1, z0).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(0, 1, 0);

        v.addVertex(xh1, y1, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x1, y1, zh1).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x1, y1, zh0).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh1, y1, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, 1, 0);

        v.addVertex(xh1, y1, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x1, y1, z1).setColor(color).setUv(u1, v1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x1, y1, zh1).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh1, y1, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, 1, 0);

        v.addVertex(xh0, y1, z1).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh1, y1, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh1, y1, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh0, y1, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, 1, 0);

        v.addVertex(x0, y1, z1).setColor(color).setUv(u0, v1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh0, y1, z1).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh0, y1, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x0, y1, zh1).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(0, 1, 0);

        v.addVertex(x0, y1, zh1).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh0, y1, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(xh0, y1, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(x0, y1, zh0).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(0, 1, 0);
    }

    static void renderEastFaceCutOut(
            float z0, float z1, float y0, float y1, float x1,
            float zh0, float zh1, float yh0, float yh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light,
            int color) {

        // Interpolated UVs for the hole
        float uh0 = interpolate(y0, y1, u0, u1, yh0);
        float uh1 = interpolate(y0, y1, u0, u1, yh1);
        float vh0 = interpolate(z0, z1, v0, v1, zh0);
        float vh1 = interpolate(z0, z1, v0, v1, zh1);

        v.addVertex(x1, yh1, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y1, zh1).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y1, z1).setColor(color).setUv(u1, v1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh1, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(1, 0, 0);

        v.addVertex(x1, yh1, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh1, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y1, zh0).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y1, zh1).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(1, 0, 0);

        v.addVertex(x1, yh1, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh1, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh0, z1).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh0, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(1, 0, 0);

        v.addVertex(x1, yh0, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh0, z1).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y0, z1).setColor(color).setUv(u0, v1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y0, zh1).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(1, 0, 0);

        v.addVertex(x1, yh0, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh0, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y0, zh1).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y0, zh0).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(1, 0, 0);

        v.addVertex(x1, yh0, z0).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh0, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y0, zh0).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y0, z0).setColor(color).setUv(u0, v0).setOverlay(0).setLight(light).setNormal(1, 0, 0);

        v.addVertex(x1, yh1, z0).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh1, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh0, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh0, z0).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(1, 0, 0);

        v.addVertex(x1, y1, z0).setColor(color).setUv(u1, v0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, y1, zh0).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh1, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
        v.addVertex(x1, yh1, z0).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(1, 0, 0);
    }

    static void renderWestFaceCutOut(
            float z0, float z1, float y0, float y1, float x0,
            float zh0, float zh1, float yh0, float yh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light,
            int color) {

        // Interpolated UVs for the hole
        float uh0 = interpolate(y0, y1, u0, u1, yh0);
        float uh1 = interpolate(y0, y1, u0, u1, yh1);
        float vh0 = interpolate(z0, z1, v0, v1, zh0);
        float vh1 = interpolate(z0, z1, v0, v1, zh1);

        v.addVertex(x0, yh1, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, y1, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, y1, zh1).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh1, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);

        v.addVertex(x0, y1, zh1).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, y1, zh0).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh1, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh1, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);

        v.addVertex(x0, yh0, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh0, z1).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh1, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh1, zh1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);

        v.addVertex(x0, y0, zh1).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, y0, z1).setColor(color).setUv(u0, v1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh0, z1).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh0, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);

        v.addVertex(x0, y0, zh0).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, y0, zh1).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh0, zh1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh0, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);

        v.addVertex(x0, y0, z0).setColor(color).setUv(u0, v0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, y0, zh0).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh0, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh0, z0).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);

        v.addVertex(x0, yh0, z0).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh0, zh0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh1, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh1, z0).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);

        v.addVertex(x0, yh1, z0).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, yh1, zh0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, y1, zh0).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
        v.addVertex(x0, y1, z0).setColor(color).setUv(u1, v0).setOverlay(0).setLight(light).setNormal(-1, 0, 0);
    }

    static void renderSouthFaceCutOut(
            float x0, float x1, float y0, float y1, float z1,
            float xh0, float xh1, float yh0, float yh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light,
            int color) {

        // Interpolated UVs for the hole
        float uh0 = interpolate(x0, x1, u0, u1, xh0);
        float uh1 = interpolate(x0, x1, u0, u1, xh1);
        float vh0 = interpolate(y0, y1, v0, v1, yh0);
        float vh1 = interpolate(y0, y1, v0, v1, yh1);

        v.addVertex(x1, y0, z1).setColor(color).setUv(u1, v0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x1, yh0, z1).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh1, yh0, z1).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh1, y0, z1).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(0, 0, 1);

        v.addVertex(x1, yh0, z1).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x1, yh1, z1).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh1, yh1, z1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh1, yh0, z1).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, 0, 1);

        v.addVertex(x1, yh1, z1).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x1, y1, z1).setColor(color).setUv(u1, v1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh1, y1, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh1, yh1, z1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, 0, 1);

        v.addVertex(xh1, y0, z1).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh1, yh0, z1).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh0, yh0, z1).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh0, y0, z1).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(0, 0, 1);

        v.addVertex(xh1, yh1, z1).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh1, y1, z1).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh0, y1, z1).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh0, yh1, z1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, 0, 1);

        v.addVertex(xh0, y0, z1).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh0, yh0, z1).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x0, yh0, z1).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x0, y0, z1).setColor(color).setUv(u0, v0).setOverlay(0).setLight(light).setNormal(0, 0, 1);

        v.addVertex(xh0, yh0, z1).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh0, yh1, z1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x0, yh1, z1).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x0, yh0, z1).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(0, 0, 1);

        v.addVertex(xh0, yh1, z1).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(xh0, y1, z1).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x0, y1, z1).setColor(color).setUv(u0, v1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
        v.addVertex(x0, yh1, z1).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(0, 0, 1);
    }

    static void renderNorthFaceCutOut(
            float x0, float x1, float y0, float y1, float z0,
            float xh0, float xh1, float yh0, float yh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light,
            int color) {

        // Interpolated UVs for the hole
        float uh0 = interpolate(x0, x1, u0, u1, xh0);
        float uh1 = interpolate(x0, x1, u0, u1, xh1);
        float vh0 = interpolate(y0, y1, v0, v1, yh0);
        float vh1 = interpolate(y0, y1, v0, v1, yh1);

        v.addVertex(xh1, y0, z0).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh1, yh0, z0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x1, yh0, z0).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x1, y0, z0).setColor(color).setUv(u1, v0).setOverlay(0).setLight(light).setNormal(0, 0, -1);

        v.addVertex(xh1, yh0, z0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh1, yh1, z0).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x1, yh1, z0).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x1, yh0, z0).setColor(color).setUv(u1, vh0).setOverlay(0).setLight(light).setNormal(0, 0, -1);

        v.addVertex(xh1, yh1, z0).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh1, y1, z0).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x1, y1, z0).setColor(color).setUv(u1, v1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x1, yh1, z0).setColor(color).setUv(u1, vh1).setOverlay(0).setLight(light).setNormal(0, 0, -1);

        v.addVertex(xh0, y0, z0).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh0, yh0, z0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh1, yh0, z0).setColor(color).setUv(uh1, vh0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh1, y0, z0).setColor(color).setUv(uh1, v0).setOverlay(0).setLight(light).setNormal(0, 0, -1);

        v.addVertex(xh0, yh1, z0).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh0, y1, z0).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh1, y1, z0).setColor(color).setUv(uh1, v1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh1, yh1, z0).setColor(color).setUv(uh1, vh1).setOverlay(0).setLight(light).setNormal(0, 0, -1);

        v.addVertex(x0, y0, z0).setColor(color).setUv(u0, v0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x0, yh0, z0).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh0, yh0, z0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh0, y0, z0).setColor(color).setUv(uh0, v0).setOverlay(0).setLight(light).setNormal(0, 0, -1);

        v.addVertex(x0, yh0, z0).setColor(color).setUv(u0, vh0).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x0, yh1, z0).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh0, yh1, z0).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh0, yh0, z0).setColor(color).setUv(uh0, vh0).setOverlay(0).setLight(light).setNormal(0, 0, -1);

        v.addVertex(x0, yh1, z0).setColor(color).setUv(u0, vh1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(x0, y1, z0).setColor(color).setUv(u0, v1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh0, y1, z0).setColor(color).setUv(uh0, v1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
        v.addVertex(xh0, yh1, z0).setColor(color).setUv(uh0, vh1).setOverlay(0).setLight(light).setNormal(0, 0, -1);
    }

    public static boolean shouldRenderCentered(Fluid f) {
        //return true;
        return f.getFluidType().isLighterThanAir();
    }

    public static void renderFluids(EntityPipe tile, VertexConsumer v, int light, int overlay) {
        BlockState tileState = tile.getLevel().getBlockState(tile.getBlockPos());
        if (!(tileState.getBlock() instanceof BlockPipe)) return;
        // render center
        if (!tile.tank.isEmpty()) {

            float u0f = tile.renderData.spriteFLowing.getU0();
            float u1f = tile.renderData.spriteFLowing.getU1();
            float v0f = tile.renderData.spriteFLowing.getV0();
            float v1f = tile.renderData.spriteFLowing.getV1();

            float u0s = tile.renderData.spriteStill.getU0();
            float u1s = tile.renderData.spriteStill.getU1();
            float v0s = tile.renderData.spriteStill.getV0();
            float v1s = tile.renderData.spriteStill.getV1();

            int color = tile.renderData.color;


            if (
                    !tile.connections.get(Direction.NORTH).isEnabled(tileState) &&
                            !tile.connections.get(Direction.SOUTH).isEnabled(tileState) &&
                            !tile.connections.get(Direction.WEST).isEnabled(tileState) &&
                            !tile.connections.get(Direction.EAST).isEnabled(tileState)
            ) {
                // is vertical render because no horizontal connections are found

                float relativeFill = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();


                float actualW = wMin + (wMax - wMin) * relativeFill;
                float y0 = -0.25f;
                float y1 = 0.25f;
                float x0 = -actualW;
                float x1 = actualW;
                float z0 = -actualW;
                float z1 = actualW;


                // first scan if it has only one output to use this as flow direction, second look if it has only one input to use as flow direction
                Direction outFlow = null;
                Direction inFlow = null;
                int numOutputs = 0;
                int numInputs = 0;
                PipeConnection conn;
                conn = tile.connections.get(Direction.UP);
                if (conn.getsInputFromInside) {
                    outFlow = Direction.UP;
                    numOutputs++;
                }
                if (conn.getsInputFromOutside) {
                    inFlow = Direction.UP.getOpposite();
                    numInputs++;
                }
                conn = tile.connections.get(Direction.DOWN);
                if (conn.getsInputFromInside) {
                    outFlow = Direction.DOWN;
                    numOutputs++;
                }
                if (conn.getsInputFromOutside) {
                    inFlow = Direction.DOWN.getOpposite();
                    numInputs++;
                }
                if (numOutputs == 1) {
                    // exactly one output is found, great! use this as flow direction
                    renderFluidFlowingCentered(
                            x0, x1, z0, z1, y0, y1,
                            u0f, u1f, v0f, v1f,
                            color, outFlow, v, light, overlay);

                } else if (numInputs == 1) {
                    // exactly one input is found, great! use this as flow direction
                    renderFluidFlowingCentered(
                            x0, x1, z0, z1, y0, y1,
                            u0f, u1f, v0f, v1f,
                            color, inFlow, v, light, overlay);
                } else {
                    // do not use flowing animation
                    renderVerticalFluidStill(
                            x0, x1, z0, z1, y0, y1,
                            u0s, u1s, v0s, v1s,
                            color, v, light, overlay);
                }


                float relativeFillUp = (float) tile.connections.get(Direction.UP).tank.getFluidAmount() / tile.connections.get(Direction.UP).tank.getTankCapacity(0);
                float actualWUp = 0;
                if (relativeFillUp > e)
                    actualWUp = wMin + (wMax - wMin) * relativeFillUp;
                float xh2 = -actualWUp;
                float xh3 = actualWUp;
                float zh2 = -actualWUp;
                float zh3 = actualWUp;
                if (actualWUp + e < actualW) {
                    renderUpFaceCutOut(x0, x1, z0, z1, y1, xh2, xh3, zh2, zh3, u0s, u1s, v0s, v1s, v, light, color);
                }

                float relativeFillDown = (float) tile.connections.get(Direction.DOWN).tank.getFluidAmount() / tile.connections.get(Direction.DOWN).tank.getTankCapacity(0);
                float actualWDown = 0;
                if (relativeFillDown > e)
                    actualWDown = wMin + (wMax - wMin) * relativeFillDown;
                float xh0 = -actualWDown;
                float xh1 = actualWDown;
                float zh0 = -actualWDown;
                float zh1 = actualWDown;
                if (actualWDown - e < actualW) {
                    renderDownFaceCutOut(x0, x1, z0, z1, y0, xh0, xh1, zh0, zh1, u0s, u1s, v0s, v1s, v, light, color);
                }

            } else {
                if (!shouldRenderCentered(tile.tank.getFluid().getFluid())) {
                    // is horizontal render
                    float x0 = -0.25f + e;
                    float x1 = 0.25f - e;
                    float z0 = -0.25f + e;
                    float z1 = 0.25f - e;
                    float y0 = -0.25f + e;
                    float y1 = y0 - 2 * e + 0.5f * (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                    float y0BottomOffsetNorth = y0;
                    float y0BottomOffsetSouth = y0;
                    float y0BottomOffsetWest = y0;
                    float y0BottomOffsetEast = y0;

                    // first scan if it has only one output to use this as flow direction
                    Direction outFlow = null;
                    Direction inFlow = null;
                    int numOutputs = 0;
                    int numInputs = 0;
                    PipeConnection conn;
                    conn = tile.connections.get(Direction.NORTH);
                    if (conn.isEnabled(tileState)) {
                        y0BottomOffsetNorth = y0 - 2 * e + 0.5f * (float) conn.tank.getFluidAmount() / conn.tank.getCapacity();
                        if (conn.getsInputFromInside) {
                            numOutputs++;
                            outFlow = Direction.NORTH;
                        }
                        if (conn.getsInputFromOutside) {
                            numInputs++;
                            inFlow = Direction.NORTH.getOpposite();
                        }
                    }
                    conn = tile.connections.get(Direction.SOUTH);
                    if (conn.isEnabled(tileState)) {
                        y0BottomOffsetSouth = y0 - 2 * e + 0.5f * (float) conn.tank.getFluidAmount() / conn.tank.getCapacity();
                        if (conn.getsInputFromInside) {
                            numOutputs++;
                            outFlow = Direction.SOUTH;
                        }
                        if (conn.getsInputFromOutside) {
                            numInputs++;
                            inFlow = Direction.SOUTH.getOpposite();
                        }
                    }
                    conn = tile.connections.get(Direction.EAST);
                    if (conn.isEnabled(tileState)) {
                        y0BottomOffsetEast = y0 - 2 * e + 0.5f * (float) conn.tank.getFluidAmount() / conn.tank.getCapacity();
                        if (conn.getsInputFromInside) {
                            numOutputs++;
                            outFlow = Direction.EAST;
                        }
                        if (conn.getsInputFromOutside) {
                            numInputs++;
                            inFlow = Direction.EAST.getOpposite();
                        }
                    }
                    conn = tile.connections.get(Direction.WEST);
                    if (conn.isEnabled(tileState)) {
                        y0BottomOffsetWest = y0 - 2 * e + 0.5f * (float) conn.tank.getFluidAmount() / conn.tank.getCapacity();
                        if (conn.getsInputFromInside) {
                            numOutputs++;
                            outFlow = Direction.WEST;
                        }
                        if (conn.getsInputFromOutside) {
                            numInputs++;
                            inFlow = Direction.WEST.getOpposite();
                        }
                    }
                    if (numOutputs == 1) {
                        // exactly one output is found, great! use this as flow direction
                        renderHorizontalFluidFlowing(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f, v1f,
                                color, outFlow, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);

                    } else if (numInputs == 1) {
                        // exactly one input is found, great! use this as flow direction
                        renderHorizontalFluidFlowing(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f, v1f,
                                color, inFlow, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);

                    } else {
                        // do not use flowing animation
                        renderHorizontalFluidStill(
                                x0, x1, z0, z1, y0, y1,
                                u0s, u1s, v0s, v1s,
                                color, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    }

                    if (tile.connections.get(Direction.UP).isEnabled(tileState)) {

                        float relativeFill = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();

                        float actualW = wMin + (wMax - wMin) * relativeFill;
                        y0 = -0.25f - e + 0.5f * (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                        y1 = 0.25f;
                        x0 = -actualW;
                        x1 = actualW;
                        z0 = -actualW;
                        z1 = actualW;


                        conn = tile.connections.get(Direction.UP);
                        if (conn.getsInputFromInside) {
                            //add render up animation
                            renderFluidFlowingCentered(
                                    x0, x1, z0, z1, y0, y1,
                                    u0f, u1f, v0f, v1f,
                                    color, Direction.UP, v, light, overlay);
                        }
                        if (conn.outputsToInside) {
                            //add render down animation
                            renderFluidFlowingCentered(
                                    x0, x1, z0, z1, y0, y1,
                                    u0f, u1f, v0f, v1f,
                                    color, Direction.DOWN, v, light, overlay);
                        }
                    }
                } else {
                    // it is a centered horizontal render
                    Direction outFlow = null;
                    Direction inFlow = null;
                    int numOutputs = 0;
                    int numInputs = 0;

                    float relativeFill = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();

                    float actualW = wMin + (wMax - wMin) * relativeFill;
                    for (Direction d : Direction.values()) {
                        PipeConnection conn = tile.connections.get(d);
                        if (conn.isEnabled(tileState)) {
                            float y0 = -actualW;
                            float y1 = actualW;
                            float x0 = -actualW;
                            float x1 = actualW;
                            float z0 = -actualW;
                            float z1 = actualW;

                            if (d == Direction.UP) {
                                y0 = actualW;
                                y1 = 0.25f;
                                float relativeFillconn = (float) tile.connections.get(d).tank.getFluidAmount() / tile.connections.get(d).tank.getTankCapacity(0);
                                float actualWconn = 0;
                                if (relativeFillconn > e)
                                    actualWconn = wMin + (wMax - wMin) * actualWconn;
                                float xh0 = -actualWconn;
                                float xh1 = actualWconn;
                                float zh0 = -actualWconn;
                                float zh1 = actualWconn;
                                if (actualWconn - e < actualW) {
                                    renderUpFaceCutOut(x0, x1, z0, z1, y1, xh0, xh1, zh0, zh1, u0s, u1s, v0s, v1s, v, light, color);
                                }
                            }
                            if (d == Direction.DOWN) {
                                y0 = -0.25f;
                                y1 = -actualW;
                                float relativeFillconn = (float) tile.connections.get(d).tank.getFluidAmount() / tile.connections.get(d).tank.getTankCapacity(0);
                                float actualWconn = 0;
                                if (relativeFillconn > e)
                                    actualWconn = wMin + (wMax - wMin) * actualWconn;
                                float xh0 = -actualWconn;
                                float xh1 = actualWconn;
                                float zh0 = -actualWconn;
                                float zh1 = actualWconn;
                                if (actualWconn - e < actualW) {
                                    renderDownFaceCutOut(x0, x1, z0, z1, y0, xh0, xh1, zh0, zh1, u0s, u1s, v0s, v1s, v, light, color);
                                }
                            }
                            if (d == Direction.EAST) {
                                x0 = actualW;
                                x1 = 0.25f;
                                float relativeFillconn = (float) tile.connections.get(d).tank.getFluidAmount() / tile.connections.get(d).tank.getTankCapacity(0);
                                float actualWconn = 0;
                                if (relativeFillconn > e)
                                    actualWconn = wMin + (wMax - wMin) * actualWconn;
                                float zh0 = -actualWconn;
                                float zh1 = actualWconn;
                                float yh0 = -actualWconn;
                                float yh1 = actualWconn;
                                if (actualWconn - e < actualW) {
                                    renderEastFaceCutOut(z0, z1, y0, y1, x1, zh0, zh1, yh0, yh1, u0s, u1s, v0s, v1s, v, light, color);
                                }
                            }
                            if (d == Direction.WEST) {
                                x0 = -0.25f;
                                x1 = -actualW;
                                float relativeFillconn = (float) tile.connections.get(d).tank.getFluidAmount() / tile.connections.get(d).tank.getTankCapacity(0);
                                float actualWconn = 0;
                                if (relativeFillconn > e)
                                    actualWconn = wMin + (wMax - wMin) * actualWconn;
                                float xh0 = -actualWconn;
                                float xh1 = actualWconn;
                                float zh0 = -actualWconn;
                                float zh1 = actualWconn;
                                if (actualWconn - e < actualW) {
                                    renderWestFaceCutOut(z0, z1, y0, y1, x0, xh0, xh1, zh0, zh1, u0s, u1s, v0s, v1s, v, light, color);
                                }
                            }
                            if (d == Direction.SOUTH) {
                                z0 = actualW;
                                z1 = 0.25f;
                                float relativeFillconn = (float) tile.connections.get(d).tank.getFluidAmount() / tile.connections.get(d).tank.getTankCapacity(0);
                                float actualWconn = 0;
                                if (relativeFillconn > e)
                                    actualWconn = wMin + (wMax - wMin) * actualWconn;
                                float yh0 = -actualWconn;
                                float yh1 = actualWconn;
                                float xh0 = -actualWconn;
                                float xh1 = actualWconn;
                                if (actualWconn - e < actualW) {
                                    renderSouthFaceCutOut(x0, x1, y0, y1, z1, xh0, xh1, yh0, yh1, u0s, u1s, v0s, v1s, v, light, color);
                                }
                            }
                            if (d == Direction.NORTH) {
                                z0 = -0.25f;
                                z1 = -actualW;
                                float relativeFillconn = (float) tile.connections.get(d).tank.getFluidAmount() / tile.connections.get(d).tank.getTankCapacity(0);
                                float actualWconn = 0;
                                if (relativeFillconn > e)
                                    actualWconn = wMin + (wMax - wMin) * actualWconn;
                                float yh0 = -actualWconn;
                                float yh1 = actualWconn;
                                float xh0 = -actualWconn;
                                float xh1 = actualWconn;
                                if (actualWconn - e < actualW) {
                                    renderNorthFaceCutOut(x0, x1, y0, y1, z0, xh0, xh1, yh0, yh1, u0s, u1s, v0s, v1s, v, light, color);
                                }
                            }
                            if (conn.outputsToInside) {
                                numInputs++;
                                inFlow = d;
                                renderFluidFlowingCentered(
                                        x0, x1, z0, z1, y0, y1,
                                        u0f, u1f, v0f, v0f + (0.5f - actualW * 2) * (v1f - v0f),
                                        color, d.getOpposite(), v, light, overlay);
                            } else if (conn.getsInputFromInside) {
                                numOutputs++;
                                outFlow = d;
                                renderFluidFlowingCentered(
                                        x0, x1, z0, z1, y0, y1,
                                        u0f, u1f, v1f - (0.5f - actualW * 2) * (v1f - v0f), v1f,
                                        color, d, v, light, overlay);
                            } else {
                                renderVerticalFluidStill(
                                        x0, x1, z0, z1, y0, y1,
                                        u0s, u1s, v0s, v1s,
                                        color, v, light, overlay);
                            }
                        }
                    }

                    float y0 = -actualW;
                    float y1 = actualW;
                    float x0 = -actualW;
                    float x1 = actualW;
                    float z0 = -actualW;
                    float z1 = actualW;

                    if (numOutputs == 1) {
                        // exactly one output is found, great! use this as flow direction
                        renderFluidFlowingCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f + (0.5f - actualW * 2) * (v1f - v0f), v1f - (0.5f - actualW * 2) * (v1f - v0f),
                                color, outFlow, v, light, overlay);

                        if (!tile.connections.get(outFlow.getOpposite()).isEnabled(tileState) || tile.connections.get(outFlow.getOpposite()).tank.isEmpty()) {
                            renderFluidCubeFacebyDirection(
                                    x0, x1, z0, z1, y0, y1,
                                    u0s, u1s, v0s, v1s,
                                    outFlow.getOpposite(), color, v, light, overlay);
                        }


                    } else if (numInputs == 1) {
                        // exactly one input is found, great! use this as flow direction
                        renderFluidFlowingCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f + (0.5f - actualW * 2) * (v1f - v0f), v1f - (0.5f - actualW * 2) * (v1f - v0f),
                                color, inFlow.getOpposite(), v, light, overlay);

                        if (!tile.connections.get(inFlow.getOpposite()).isEnabled(tileState) || tile.connections.get(inFlow.getOpposite()).tank.isEmpty()) {
                            renderFluidCubeFacebyDirection(
                                    x0, x1, z0, z1, y0, y1,
                                    u0s, u1s, v0s, v1s,
                                    inFlow.getOpposite(), color, v, light, overlay);
                        }

                    } else {
                        // do not use flowing animation
                        renderFluidCubeStill(
                                x0, x1, z0, z1, y0, y1,
                                u0s, u1s, v0s, v1s,
                                color, v, light, overlay);
                    }
                }
            }
        }

        //render connections
        if (tile.connections.get(Direction.UP).isEnabled(tileState)) {
            PipeConnection conn = tile.connections.get(Direction.UP);

            float u0f = conn.renderData.spriteFLowing.getU0();
            float u1f = conn.renderData.spriteFLowing.getU1();
            float v0f = conn.renderData.spriteFLowing.getV0();
            float v1f = conn.renderData.spriteFLowing.getV1();

            float u0s = conn.renderData.spriteStill.getU0();
            float u1s = conn.renderData.spriteStill.getU1();
            float v0s = conn.renderData.spriteStill.getV0();
            float v1s = conn.renderData.spriteStill.getV1();

            int color = conn.renderData.color;

            int fluidInTank = conn.tank.getFluidAmount();
            if (fluidInTank > 0) {
                int max = conn.tank.getCapacity();
                float relativeFill = (float) fluidInTank / max;

                float actualW = wMin + (wMax - wMin) * relativeFill;
                float y0 = 0.25f - e;
                float y1 = 0.5f;
                float x0 = -actualW;
                float x1 = actualW;
                float z0 = -actualW;
                float z1 = actualW;


                if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                    renderFluidFlowingCentered(
                            x0, x1, z0, z1, y0, y1,
                            u0f, u1f, v0f + 0.5f * (v1f - v0f), v1f,
                            color, Direction.DOWN, v, light, overlay);
                } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                    renderFluidFlowingCentered(
                            x0, x1, z0, z1, y0, y1,
                            u0f, u1f, v0f, v1f - 0.5f * (v1f - v0f),
                            color, Direction.UP, v, light, overlay);
                } else {
                    renderVerticalFluidStill(
                            x0, x1, z0, z1, y0, y1,
                            u0s, u1s, v0s, v1s,
                            color, v, light, overlay);
                }

                float relativeFillOut = (float) conn.neighborFluidHandler().getFluidInTank(0).getAmount() / conn.neighborFluidHandler().getTankCapacity(0);
                float actualWOut = 0;
                if (relativeFillOut > e)
                    actualWOut = wMin + (wMax - wMin) * relativeFillOut;
                float xh2 = -actualWOut;
                float xh3 = actualWOut;
                float zh2 = -actualWOut;
                float zh3 = actualWOut;
                if (actualWOut < actualW) {
                    renderUpFaceCutOut(x0, x1, z0, z1, y1, xh2, xh3, zh2, zh3, u0s, u1s, v0s, v1s, v, light, color);
                }

                float relativeFillIn = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                float actualWIn = 0;
                if (relativeFillIn > e)
                    actualWIn = wMin + (wMax - wMin) * relativeFillIn;
                float xh0 = -actualWIn;
                float xh1 = actualWIn;
                float zh0 = -actualWIn;
                float zh1 = actualWIn;
                if (actualWIn < actualW) {
                    renderDownFaceCutOut(x0, x1, z0, z1, y0, xh0, xh1, zh0, zh1, u0s, u1s, v0s, v1s, v, light, color);
                }

            }
        }
        if (tile.connections.get(Direction.DOWN).isEnabled(tileState)) {
            PipeConnection conn = tile.connections.get(Direction.DOWN);

            float u0f = conn.renderData.spriteFLowing.getU0();
            float u1f = conn.renderData.spriteFLowing.getU1();
            float v0f = conn.renderData.spriteFLowing.getV0();
            float v1f = conn.renderData.spriteFLowing.getV1();

            float u0s = conn.renderData.spriteStill.getU0();
            float u1s = conn.renderData.spriteStill.getU1();
            float v0s = conn.renderData.spriteStill.getV0();
            float v1s = conn.renderData.spriteStill.getV1();

            int color = conn.renderData.color;

            int fluidInTank = conn.tank.getFluidAmount();
            if (fluidInTank > 0) {
                int max = conn.tank.getCapacity();
                float relativeFill = (float) fluidInTank / max;

                float actualW = wMin + (wMax - wMin) * relativeFill;
                float y0 = -0.5f;
                float y1 = -0.25f + e;
                float x0 = -actualW;
                float x1 = actualW;
                float z0 = -actualW;
                float z1 = actualW;


                if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                    renderFluidFlowingCentered(
                            x0, x1, z0, z1, y0, y1,
                            u0f, u1f, v0f + 0.5f * (v1f - v0f), v1f,
                            color, Direction.UP, v, light, overlay);
                } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                    renderFluidFlowingCentered(
                            x0, x1, z0, z1, y0, y1,
                            u0f, u1f, v0f, v1f - 0.5f * (v1f - v0f),
                            color, Direction.DOWN, v, light, overlay);
                } else {
                    renderVerticalFluidStill(
                            x0, x1, z0, z1, y0, y1,
                            u0s, u1s, v0s, v1s,
                            color, v, light, overlay);
                }


                float relativeFillOut = (float) conn.neighborFluidHandler().getFluidInTank(0).getAmount() / conn.neighborFluidHandler().getTankCapacity(0);
                float actualWOut = 0;
                if (relativeFillOut > e)
                    actualWOut = wMin + (wMax - wMin) * relativeFillOut;
                float xh2 = -actualWOut;
                float xh3 = actualWOut;
                float zh2 = -actualWOut;
                float zh3 = actualWOut;
                if (actualWOut < actualW) {
                    renderDownFaceCutOut(x0, x1, z0, z1, y0, xh2, xh3, zh2, zh3, u0s, u1s, v0s, v1s, v, light, color);
                }

                float relativeFillIn = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                float actualWIn = 0;
                if (relativeFillIn > e)
                    actualWIn = wMin + (wMax - wMin) * relativeFillIn;
                float xh0 = -actualWIn;
                float xh1 = actualWIn;
                float zh0 = -actualWIn;
                float zh1 = actualWIn;
                if (actualWIn < actualW) {
                    renderUpFaceCutOut(x0, x1, z0, z1, y1, xh0, xh1, zh0, zh1, u0s, u1s, v0s, v1s, v, light, color);
                }

            }
        }
        if (tile.connections.get(Direction.WEST).isEnabled(tileState)) {
            PipeConnection conn = tile.connections.get(Direction.WEST);

            float u0f = conn.renderData.spriteFLowing.getU0();
            float u1f = conn.renderData.spriteFLowing.getU1();
            float v0f = conn.renderData.spriteFLowing.getV0();
            float v1f = conn.renderData.spriteFLowing.getV1();

            float u0s = conn.renderData.spriteStill.getU0();
            float u1s = conn.renderData.spriteStill.getU1();
            float v0s = conn.renderData.spriteStill.getV0();
            float v1s = conn.renderData.spriteStill.getV1();

            int color = conn.renderData.color;

            int fluidInTank = conn.tank.getFluidAmount();
            if (fluidInTank > 0) {
                int max = conn.tank.getCapacity();
                float relativeFill = (float) fluidInTank / max;

                if (!shouldRenderCentered(conn.tank.getFluid().getFluid())) {
                    float x0 = -0.5f;
                    if (tile.tankWest)
                        x0 -= 0.125f;
                    float x1 = -0.25f + e;
                    float z0 = -0.25f + e;
                    float z1 = 0.25f - e;
                    float y0 = -0.25f + e;
                    float y1 = -0.25f - e + 0.5f * relativeFill;
                    float y0BottomOffsetNorth = y0;
                    float y0BottomOffsetSouth = y0;
                    float y0BottomOffsetWest = y0;
                    float y0BottomOffsetEast = y0 - 2 * e + 0.5f * (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                    if (conn.neighborFluidHandler() instanceof PipeConnection p)
                        y0BottomOffsetWest = y0 - 2 * e + 0.5f * (float) p.tank.getFluidAmount() / p.tank.getCapacity();

                    if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f + 0.5f * (v1f - v0f), v1f,
                                color, Direction.EAST, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f, v1f - 0.5f * (v1f - v0f),
                                color, Direction.WEST, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    } else {
                        renderHorizontalFluidStill(
                                x0, x1, z0, z1, y0, y1,
                                u0s, u1s, v0s, v1s,
                                color, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    }
                } else {

                    float actualW = wMin + (wMax - wMin) * relativeFill;
                    float y0 = -actualW;
                    float y1 = actualW;
                    float x0 = -0.5f;
                    if (tile.tankWest)
                        x0 -= 0.125f;
                    float x1 = -0.25f + e;
                    float z0 = -actualW;
                    float z1 = actualW;

                    if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                        renderFluidFlowingCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f + 0.5f * (v1f - v0f), v1f,
                                color, Direction.EAST, v, light, overlay);
                    } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                        renderFluidFlowingCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f, v1f - 0.5f * (v1f - v0f),
                                color, Direction.WEST, v, light, overlay);
                    } else {
                        renderHorizontalFluidStillCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0s, u1s, v0s, v1s,
                                color, v, light, overlay, Direction.WEST);
                    }

                    float relativeFillOut = (float) conn.neighborFluidHandler().getFluidInTank(0).getAmount() / conn.neighborFluidHandler().getTankCapacity(0);
                    float actualWOut = 0;
                    if (relativeFillOut > e)
                        actualWOut = wMin + (wMax - wMin) * relativeFillOut;
                    float yh2 = -actualWOut;
                    float yh3 = actualWOut;
                    float zh2 = -actualWOut;
                    float zh3 = actualWOut;
                    if (actualWOut < actualW) {
                        renderWestFaceCutOut(z0, z1, y0, y1, x0, zh2, zh3, yh2, yh3, u0s, u1s, v0s, v1s, v, light, color);
                    }

                    float relativeFillIn = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                    float actualWIn = 0;
                    if (relativeFillIn > e)
                        actualWIn = wMin + (wMax - wMin) * relativeFillIn;
                    float yh0 = -actualWIn;
                    float yh1 = actualWIn;
                    float zh0 = -actualWIn;
                    float zh1 = actualWIn;
                    if (actualWIn < actualW) {
                        renderEastFaceCutOut(z0, z1, y0, y1, x1, zh0, zh1, yh0, yh1, u0s, u1s, v0s, v1s, v, light, color);
                    }
                }
            }
        }
        if (tile.connections.get(Direction.EAST).isEnabled(tileState)) {
            PipeConnection conn = tile.connections.get(Direction.EAST);

            float u0f = conn.renderData.spriteFLowing.getU0();
            float u1f = conn.renderData.spriteFLowing.getU1();
            float v0f = conn.renderData.spriteFLowing.getV0();
            float v1f = conn.renderData.spriteFLowing.getV1();

            float u0s = conn.renderData.spriteStill.getU0();
            float u1s = conn.renderData.spriteStill.getU1();
            float v0s = conn.renderData.spriteStill.getV0();
            float v1s = conn.renderData.spriteStill.getV1();

            int color = conn.renderData.color;

            int fluidInTank = conn.tank.getFluidAmount();
            if (fluidInTank > 0) {
                int max = conn.tank.getCapacity();
                float relativeFill = (float) fluidInTank / max;

                if (!shouldRenderCentered(conn.tank.getFluid().getFluid())) {
                    float x0 = 0.25f - e;
                    float x1 = 0.5f;
                    if (tile.tankEast)
                        x1 += 0.125f;
                    float z0 = -0.25f + e;
                    float z1 = 0.25f - e;
                    float y0 = -0.25f + e;
                    float y1 = -0.25f - e + 0.5f * relativeFill;
                    float y0BottomOffsetNorth = y0;
                    float y0BottomOffsetSouth = y0;
                    float y0BottomOffsetWest = y0 - 2 * e + 0.5f * (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                    float y0BottomOffsetEast = y0;
                    if (conn.neighborFluidHandler() instanceof PipeConnection p)
                        y0BottomOffsetEast = y0 - 2 * e + 0.5f * (float) p.tank.getFluidAmount() / p.tank.getCapacity();

                    if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f + 0.5f * (v1f - v0f), v1f,
                                color, Direction.WEST, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f, v1f - 0.5f * (v1f - v0f),
                                color, Direction.EAST, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    } else {
                        renderHorizontalFluidStill(
                                x0, x1, z0, z1, y0, y1,
                                u0s, u1s, v0s, v1s,
                                color, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    }
                } else {

                    float actualW = wMin + (wMax - wMin) * relativeFill;
                    float y0 = -actualW;
                    float y1 = actualW;
                    float x0 = 0.25f - e;
                    float x1 = 0.5f;
                    if (tile.tankEast)
                        x1 += 0.125f;
                    float z0 = -actualW;
                    float z1 = actualW;

                    if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                        renderFluidFlowingCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f + 0.5f * (v1f - v0f), v1f,
                                color, Direction.WEST, v, light, overlay);
                    } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                        renderFluidFlowingCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f, v1f - 0.5f * (v1f - v0f),
                                color, Direction.EAST, v, light, overlay);
                    } else {
                        renderHorizontalFluidStillCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0s, u1s, v0s, v1s,
                                color, v, light, overlay, Direction.EAST);
                    }


                    float relativeFillOut = (float) conn.neighborFluidHandler().getFluidInTank(0).getAmount() / conn.neighborFluidHandler().getTankCapacity(0);
                    float actualWOut = 0;
                    if (relativeFillOut > e)
                        actualWOut = wMin + (wMax - wMin) * relativeFillOut;
                    float yh2 = -actualWOut;
                    float yh3 = actualWOut;
                    float zh2 = -actualWOut;
                    float zh3 = actualWOut;
                    if (actualWOut < actualW) {
                        renderEastFaceCutOut(z0, z1, y0, y1, x1, zh2, zh3, yh2, yh3, u0s, u1s, v0s, v1s, v, light, color);
                    }

                    float relativeFillIn = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                    float actualWIn = 0;
                    if (relativeFillIn > e)
                        actualWIn = wMin + (wMax - wMin) * relativeFillIn;
                    float yh0 = -actualWIn;
                    float yh1 = actualWIn;
                    float zh0 = -actualWIn;
                    float zh1 = actualWIn;
                    if (actualWIn < actualW) {
                        renderWestFaceCutOut(z0, z1, y0, y1, x0, zh0, zh1, yh0, yh1, u0s, u1s, v0s, v1s, v, light, color);
                    }
                }
            }
        }
        if (tile.connections.get(Direction.SOUTH).isEnabled(tileState)) {
            PipeConnection conn = tile.connections.get(Direction.SOUTH);

            float u0f = conn.renderData.spriteFLowing.getU0();
            float u1f = conn.renderData.spriteFLowing.getU1();
            float v0f = conn.renderData.spriteFLowing.getV0();
            float v1f = conn.renderData.spriteFLowing.getV1();

            float u0s = conn.renderData.spriteStill.getU0();
            float u1s = conn.renderData.spriteStill.getU1();
            float v0s = conn.renderData.spriteStill.getV0();
            float v1s = conn.renderData.spriteStill.getV1();

            int color = conn.renderData.color;

            int fluidInTank = conn.tank.getFluidAmount();
            if (fluidInTank > 0) {
                int max = conn.tank.getCapacity();
                float relativeFill = (float) fluidInTank / max;

                if (!shouldRenderCentered(conn.tank.getFluid().getFluid())) {
                    float x0 = -0.25f + e;
                    float x1 = 0.25f - e;
                    float z0 = 0.25f - e;
                    float z1 = 0.5f;
                    if (tile.tankSouth)
                        z1 += 0.125f;
                    float y0 = -0.25f + e;
                    float y1 = -0.25f - e + 0.5f * relativeFill;
                    float y0BottomOffsetNorth = y0 - 2 * e + 0.5f * (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                    float y0BottomOffsetSouth = y0;
                    float y0BottomOffsetWest = y0;
                    float y0BottomOffsetEast = y0;
                    if (conn.neighborFluidHandler() instanceof PipeConnection p)
                        y0BottomOffsetSouth = y0 - 2 * e + 0.5f * (float) p.tank.getFluidAmount() / p.tank.getCapacity();

                    if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f + 0.5f * (v1f - v0f), v1f,
                                color, Direction.NORTH, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f, v1f - 0.5f * (v1f - v0f),
                                color, Direction.SOUTH, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    } else {
                        renderHorizontalFluidStill(
                                x0, x1, z0, z1, y0, y1,
                                u0s, u1s, v0s, v1s,
                                color, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    }
                } else {


                    float actualW = wMin + (wMax - wMin) * relativeFill;
                    float y0 = -actualW;
                    float y1 = actualW;
                    float x0 = -actualW;
                    float x1 = actualW;
                    float z0 = 0.25f - e;
                    float z1 = 0.5f;
                    if (tile.tankSouth)
                        z1 += 0.125f;

                    if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                        renderFluidFlowingCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f + 0.5f * (v1f - v0f), v1f,
                                color, Direction.NORTH, v, light, overlay);
                    } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                        renderFluidFlowingCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f, v1f - 0.5f * (v1f - v0f),
                                color, Direction.SOUTH, v, light, overlay);
                    } else {
                        renderHorizontalFluidStillCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0s, u1s, v0s, v1s,
                                color, v, light, overlay, Direction.SOUTH);
                    }

                    float relativeFillOut = (float) conn.neighborFluidHandler().getFluidInTank(0).getAmount() / conn.neighborFluidHandler().getTankCapacity(0);
                    float actualWOut = 0;
                    if (relativeFillOut > e)
                        actualWOut = wMin + (wMax - wMin) * relativeFillOut;
                    float yh2 = -actualWOut;
                    float yh3 = actualWOut;
                    float xh2 = -actualWOut;
                    float xh3 = actualWOut;
                    if (actualWOut < actualW) {
                        renderSouthFaceCutOut(x0, x1, y0, y1, z1, xh2, xh3, yh2, yh3, u0s, u1s, v0s, v1s, v, light, color);
                    }

                    float relativeFillIn = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                    float actualWIn = 0;
                    if (relativeFillIn > e)
                        actualWIn = wMin + (wMax - wMin) * relativeFillIn;
                    float yh0 = -actualWIn;
                    float yh1 = actualWIn;
                    float xh0 = -actualWIn;
                    float xh1 = actualWIn;
                    if (actualWIn < actualW) {
                        renderNorthFaceCutOut(x0, x1, y0, y1, z0, xh0, xh1, yh0, yh1, u0s, u1s, v0s, v1s, v, light, color);
                    }
                }
            }
        }
        if (tile.connections.get(Direction.NORTH).isEnabled(tileState)) {
            PipeConnection conn = tile.connections.get(Direction.NORTH);

            float u0f = conn.renderData.spriteFLowing.getU0();
            float u1f = conn.renderData.spriteFLowing.getU1();
            float v0f = conn.renderData.spriteFLowing.getV0();
            float v1f = conn.renderData.spriteFLowing.getV1();

            float u0s = conn.renderData.spriteStill.getU0();
            float u1s = conn.renderData.spriteStill.getU1();
            float v0s = conn.renderData.spriteStill.getV0();
            float v1s = conn.renderData.spriteStill.getV1();

            int color = conn.renderData.color;

            int fluidInTank = conn.tank.getFluidAmount();
            if (fluidInTank > 0) {
                int max = conn.tank.getCapacity();
                float relativeFill = (float) fluidInTank / max;

                if (!shouldRenderCentered(conn.tank.getFluid().getFluid())) {
                    float x0 = -0.25f + e;
                    float x1 = 0.25f - e;
                    float z0 = -0.5f;
                    if (tile.tankNorth)
                        z0 -= 0.125f;
                    float z1 = -0.25f + e;
                    float y0 = -0.25f + e;
                    float y1 = -0.25f - e + 0.5f * relativeFill;
                    float y0BottomOffsetNorth = y0;
                    float y0BottomOffsetSouth = y0 - 2 * e + 0.5f * (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                    float y0BottomOffsetWest = y0;
                    ;
                    float y0BottomOffsetEast = y0;
                    if (conn.neighborFluidHandler() instanceof PipeConnection p)
                        y0BottomOffsetNorth = y0 - 2 * e + 0.5f * (float) p.tank.getFluidAmount() / p.tank.getCapacity();

                    if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f + 0.5f * (v1f - v0f), v1f,
                                color, Direction.SOUTH, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f, v1f - 0.5f * (v1f - v0f),
                                color, Direction.NORTH, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    } else {
                        renderHorizontalFluidStill(
                                x0, x1, z0, z1, y0, y1,
                                u0s, u1s, v0s, v1s,
                                color, v, light, overlay,
                                y0BottomOffsetNorth, y0BottomOffsetSouth, y0BottomOffsetEast, y0BottomOffsetWest);
                    }
                } else {


                    float actualW = wMin + (wMax - wMin) * relativeFill;
                    float y0 = -actualW;
                    float y1 = actualW;
                    float x0 = -actualW;
                    float x1 = actualW;
                    float z0 = -0.5f;
                    if (tile.tankNorth)
                        z0 -= 0.125f;
                    float z1 = -0.25f + e;

                    if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                        renderFluidFlowingCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f + 0.5f * (v1f - v0f), v1f,
                                color, Direction.SOUTH, v, light, overlay);
                    } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                        renderFluidFlowingCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0f, u1f, v0f, v1f - 0.5f * (v1f - v0f),
                                color, Direction.NORTH, v, light, overlay);
                    } else {
                        renderHorizontalFluidStillCentered(
                                x0, x1, z0, z1, y0, y1,
                                u0s, u1s, v0s, v1s,
                                color, v, light, overlay, Direction.NORTH);
                    }

                    float relativeFillOut = (float) conn.neighborFluidHandler().getFluidInTank(0).getAmount() / conn.neighborFluidHandler().getTankCapacity(0);
                    float actualWOut = 0;
                    if (relativeFillOut > e)
                        actualWOut = wMin + (wMax - wMin) * relativeFillOut;
                    float yh2 = -actualWOut;
                    float yh3 = actualWOut;
                    float xh2 = -actualWOut;
                    float xh3 = actualWOut;
                    if (actualWOut < actualW) {
                        renderNorthFaceCutOut(x0, x1, y0, y1, z0, xh2, xh3, yh2, yh3, u0s, u1s, v0s, v1s, v, light, color);
                    }

                    float relativeFillIn = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                    float actualWIn = 0;
                    if (relativeFillIn > e)
                        actualWIn = wMin + (wMax - wMin) * relativeFillIn;
                    float yh0 = -actualWIn;
                    float yh1 = actualWIn;
                    float xh0 = -actualWIn;
                    float xh1 = actualWIn;
                    if (actualWIn < actualW) {
                        renderSouthFaceCutOut(x0, x1, y0, y1, z1, xh0, xh1, yh0, yh1, u0s, u1s, v0s, v1s, v, light, color);
                    }
                }
            }
        }
    }

    static WavefrontObject pipe_pump_arm;

    static {
        try {
            pipe_pump_arm = new WavefrontObject(ResourceLocation.fromNamespaceAndPath("betterpipes", "models/block/pipe_pump_arm.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void render(EntityPipe tile, float partialTick, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {


        if (tile.requiresMeshUpdate || packedLight != tile.lastLight) {

            // updae the fluid buffer on light change or fluid change
            ByteBufferBuilder myByteBuffer = new ByteBufferBuilder(TRANSIENT_BUFFER_SIZE);
            BufferBuilder bufferBuilder = new BufferBuilder(myByteBuffer, VertexFormat.Mode.QUADS, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            RenderPipe.renderFluids(tile, bufferBuilder, packedLight, 0);
            tile.mesh = bufferBuilder.build();
            if (tile.mesh != null) {
                tile.vertexBuffer.bind();
                tile.vertexBuffer.upload(tile.mesh);
            }
            myByteBuffer.close();

            // update the arm only when light changes
            if (tile.lastLight != packedLight) {
                ByteBufferBuilder byteBufferBuilder1 = new ByteBufferBuilder(1024);
                bufferBuilder = new BufferBuilder(byteBufferBuilder1, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
                for (Face i : pipe_pump_arm.groupObjects.get("Cube").faces) {
                    i.addFaceForRender(new PoseStack(), bufferBuilder, packedLight, 0, 0xffffffff);
                }
                MeshData m1 = bufferBuilder.build();
                tile.vertexBufferPumpCube.bind();
                tile.vertexBufferPumpCube.upload(m1);
                byteBufferBuilder1.close();

                ByteBufferBuilder byteBufferBuilder2 = new ByteBufferBuilder(1024);
                bufferBuilder = new BufferBuilder(byteBufferBuilder2, VertexFormat.Mode.TRIANGLES, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
                for (Face i : pipe_pump_arm.groupObjects.get("Arm").faces) {
                    i.addFaceForRender(new PoseStack(), bufferBuilder, packedLight, 0, 0xffffffff);
                }
                MeshData m2 = bufferBuilder.build();
                tile.vertexBufferCrankshaftConnection.bind();
                tile.vertexBufferCrankshaftConnection.upload(m2);
                byteBufferBuilder2.close();
            }
            tile.lastLight = packedLight;
            tile.requiresMeshUpdate = false;
        }
        LEQUAL_DEPTH_TEST.setupRenderState();
        LIGHTMAP.setupRenderState();

        if (tile.crankShaftSide != null && tile.hasAnyExtractionConnections) {
            // below you will see some complicated transformations to get the pump arm render correctly to the crankshaft
            // it took some trial and error to get the params right. I originally designed this for the sieve block in my other mod and just copied the logic.
            RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
            ShaderInstance shader = RenderSystem.getShader();
            RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath("betterpipes", "textures/block/fluid_pipe1_structure.png"));

            Matrix4f m1 = new Matrix4f();
            m1 = m1.mul(stack.last().pose());
            m1 = m1.translate(0.5f, 0.5f, 0.5f);

            if (tile.crankShaftSide == Direction.WEST) {
                m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 0f));
            }
            if (tile.crankShaftSide == Direction.EAST) {
                m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 180f));
            }
            if (tile.crankShaftSide == Direction.SOUTH) {
                m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 90f));
            }
            if (tile.crankShaftSide == Direction.NORTH) {
                m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1.0f, 0, 270f));
            }
            if (tile.crankShaftSide == Direction.DOWN) {
                BlockState blockBelow = tile.getLevel().getBlockState(tile.getBlockPos().below());
                if (blockBelow.getBlock() instanceof BlockCrankShaftBase crankShaftBase) {
                    m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1, 90f));
                    Direction.Axis axis = blockBelow.getValue(BlockCrankShaftBase.ROTATION_AXIS);
                    if (axis == axis.X) {
                        m1 = m1.rotate(new Quaternionf().fromAxisAngleDeg(1f, 0f, 0, 90f));
                    }
                }
            }
            Matrix4f m2 = new Matrix4f(m1);
            float crankshaftR = 0.075f;
            double targetHeight = 0;
            double armLength = 0.8;
            float XRotationMultiplier = 1;
            if (tile.crankShaftSide == Direction.SOUTH) XRotationMultiplier = -1;
            if (tile.crankShaftSide == Direction.WEST) XRotationMultiplier = -1;

            XRotationMultiplier *= (tile.crankShaftSide.getAxis() == Direction.Axis.Y ? -1 : 1);

            double a = tile.myMechanicalBlock.currentRotation / 180 * Math.PI + tile.myMechanicalBlock.internalVelocity / TPS * partialTick;
            if (tile.crankShaftSide == Direction.DOWN) {
                a -= Math.PI / 2;
            }
            float translationX = -1 + (float) Math.sin(a) * crankshaftR * XRotationMultiplier;
            float translationY = (float) Math.cos(a) * crankshaftR;
            double b = Math.asin((translationY - targetHeight) / armLength);
            m2.translate(translationX, translationY, -0.04f);
            m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, -(float) b * 180f / (float) Math.PI));
            m2.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, 180f));

            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            Uniform NormalMat = shader.getUniform("NormalMat");
            Matrix3f normalMat = new Matrix3f(m2); // take upper-left 3x3
            normalMat.invert().transpose(); // compute normal matrix
            NormalMat.set(normalMat);
            shader.apply();
            tile.vertexBufferCrankshaftConnection.bind();
            tile.vertexBufferCrankshaftConnection.draw();
            shader.clear();

            m2 = new Matrix4f(m1);
            float pumpCubeTargetX = -0.24f + (float) (translationX + Math.cos(b) * armLength)*0.6f; // this will reduce the movement of the pump cube bc it looks better
            m2.translate(pumpCubeTargetX, 0, 0);
            m2.scale(1f, 0.6f, 0.6f);

            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
             NormalMat = shader.getUniform("NormalMat");
             normalMat = new Matrix3f(m2); // take upper-left 3x3
            normalMat.invert().transpose(); // compute normal matrix
            NormalMat.set(normalMat);
            shader.apply();
            tile.vertexBufferPumpCube.bind();
            tile.vertexBufferPumpCube.draw();



            m2 = new Matrix4f(m1);
            pumpCubeTargetX = -0.3f;
            m2.translate(pumpCubeTargetX, 0, 0);
            m2.scale(1f, 0.75f, 0.75f);

            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, new Matrix4f(RenderSystem.getModelViewMatrix()).mul(m2), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
             NormalMat = shader.getUniform("NormalMat");
             normalMat = new Matrix3f(m2); // take upper-left 3x3
            normalMat.invert().transpose(); // compute normal matrix
            NormalMat.set(normalMat);
            shader.apply();
            tile.vertexBufferPumpCube.bind();
            tile.vertexBufferPumpCube.draw();


            NormalMat.set(new Matrix3f());
            shader.clear();
        }


        if (tile.mesh != null) {
            RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER.setupRenderState();
            TRANSLUCENT_TRANSPARENCY.setupRenderState();
            // this should be in the block atlas but shaders change shit around so
            // I just use the flowing texture and if the still texture is not in the same location
            // you are just fucked
            RenderSystem.setShaderTexture(0, tile.renderData.spriteFLowing.atlasLocation());
            ShaderInstance shader = RenderSystem.getShader();
            Matrix4f m1 = new Matrix4f(RenderSystem.getModelViewMatrix());
            Matrix4f m2 = m1.mul(stack.last().pose());
            m2 = m2.translate(0.5f, 0.5f, 0.5f);
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, m2, RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.apply();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.draw();
            shader.clear();

            RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER.clearRenderState();
            TRANSLUCENT_TRANSPARENCY.clearRenderState();
        }
        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        VertexBuffer.unbind();
    }
}