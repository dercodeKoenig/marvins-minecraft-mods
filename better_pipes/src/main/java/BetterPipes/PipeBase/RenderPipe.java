package BetterPipes.PipeBase;

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

    private static final VertexFormat POSITION_COLOR_TEXTURE_NORMAL_LIGHT =
            VertexFormat.builder()
                    .add("Position", VertexFormatElement.POSITION)
                    .add("Color",    VertexFormatElement.COLOR)
                    .add("UV0",      VertexFormatElement.UV0)
                    .add("UV1",      VertexFormatElement.UV1)
                    .add("UV2",      VertexFormatElement.UV2)
                    .add("Normal",   VertexFormatElement.NORMAL)
                    .build();

    // Small epsilon for geometry comparisons and offsets.
    static final float EPS   = 0.001f;
    // Minimum / maximum pipe half-width based on fill level.
    static final float W_MIN = 0.02f;
    static final float W_MAX = 0.25f - EPS;

    ResourceLocation pumpArmTexture;

    public RenderPipe(BlockEntityRendererProvider.Context c) {
        super();
        this.pumpArmTexture = ResourceLocation.fromNamespaceAndPath("betterpipes", "textures/block/crankshaft_pump.png");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** Maps a fluid fill fraction [0,1] to a pipe half-width. */
    private static float halfWidth(float fill) {
        return W_MIN + (W_MAX - W_MIN) * fill;
    }

    /** Emits a single fully-attributed vertex. Saves ~40 characters per call. */
    private static void vtx(VertexConsumer vc,
                            float x,  float y, float z,
                            int color, float u, float v,
                            int overlay, int light,
                            float nx, float ny, float nz) {
        vc.addVertex(x, y, z)
                .setColor(color).setUv(u, v)
                .setOverlay(overlay).setLight(light)
                .setNormal(nx, ny, nz);
    }

    /**
     * UV coordinates extracted from a connection's sprite.
     * Avoids repeating the same 8-line extraction block for every connection direction.
     */
    private record UVSet(float u0, float u1, float v0, float v1) {
        static UVSet flowing(PipeConnection conn) {
            var s = conn.renderData.spriteFLowing;
            return new UVSet(s.getU0(), s.getU1(), s.getV0(), s.getV1());
        }
        static UVSet still(PipeConnection conn) {
            var s = conn.renderData.spriteStill;
            return new UVSet(s.getU0(), s.getU1(), s.getV0(), s.getV1());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Cube face rendering
    // ──────────────────────────────────────────────────────────────────────────

    public static void renderFluidCubeStill(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            int color, VertexConsumer v, int light, int overlay) {

        // Up (y+)
        vtx(v, x0, y1, z0, color, u1, v1, overlay, light,  0,  1,  0);
        vtx(v, x0, y1, z1, color, u1, v0, overlay, light,  0,  1,  0);
        vtx(v, x1, y1, z1, color, u0, v0, overlay, light,  0,  1,  0);
        vtx(v, x1, y1, z0, color, u0, v1, overlay, light,  0,  1,  0);
        // Down (y-)
        vtx(v, x1, y0, z0, color, u1, v1, overlay, light,  0, -1,  0);
        vtx(v, x1, y0, z1, color, u1, v0, overlay, light,  0, -1,  0);
        vtx(v, x0, y0, z1, color, u0, v0, overlay, light,  0, -1,  0);
        vtx(v, x0, y0, z0, color, u0, v1, overlay, light,  0, -1,  0);
        // East (x+)
        vtx(v, x1, y0, z0, color, u0, v1, overlay, light,  1,  0,  0);
        vtx(v, x1, y1, z0, color, u1, v1, overlay, light,  1,  0,  0);
        vtx(v, x1, y1, z1, color, u1, v0, overlay, light,  1,  0,  0);
        vtx(v, x1, y0, z1, color, u0, v0, overlay, light,  1,  0,  0);
        // West (x-)
        vtx(v, x0, y0, z1, color, u1, v0, overlay, light, -1,  0,  0);
        vtx(v, x0, y1, z1, color, u0, v0, overlay, light, -1,  0,  0);
        vtx(v, x0, y1, z0, color, u0, v1, overlay, light, -1,  0,  0);
        vtx(v, x0, y0, z0, color, u1, v1, overlay, light, -1,  0,  0);
        // South (z+)
        vtx(v, x1, y0, z1, color, u0, v0, overlay, light,  0,  0,  1);
        vtx(v, x1, y1, z1, color, u0, v1, overlay, light,  0,  0,  1);
        vtx(v, x0, y1, z1, color, u1, v1, overlay, light,  0,  0,  1);
        vtx(v, x0, y0, z1, color, u1, v0, overlay, light,  0,  0,  1);
        // North (z-)
        vtx(v, x0, y0, z0, color, u1, v1, overlay, light,  0,  0, -1);
        vtx(v, x0, y1, z0, color, u1, v0, overlay, light,  0,  0, -1);
        vtx(v, x1, y1, z0, color, u0, v0, overlay, light,  0,  0, -1);
        vtx(v, x1, y0, z0, color, u0, v1, overlay, light,  0,  0, -1);
    }

    public static void renderFluidCubeFacebyDirection(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            Direction d, int color, VertexConsumer v, int light, int overlay) {

        switch (d) {
            case UP -> {
                vtx(v, x0, y1, z0, color, u1, v1, overlay, light,  0,  1,  0);
                vtx(v, x0, y1, z1, color, u1, v0, overlay, light,  0,  1,  0);
                vtx(v, x1, y1, z1, color, u0, v0, overlay, light,  0,  1,  0);
                vtx(v, x1, y1, z0, color, u0, v1, overlay, light,  0,  1,  0);
            }
            case DOWN -> {
                vtx(v, x1, y0, z0, color, u1, v1, overlay, light,  0, -1,  0);
                vtx(v, x1, y0, z1, color, u1, v0, overlay, light,  0, -1,  0);
                vtx(v, x0, y0, z1, color, u0, v0, overlay, light,  0, -1,  0);
                vtx(v, x0, y0, z0, color, u0, v1, overlay, light,  0, -1,  0);
            }
            case EAST -> {
                vtx(v, x1, y0, z0, color, u0, v1, overlay, light,  1,  0,  0);
                vtx(v, x1, y1, z0, color, u1, v1, overlay, light,  1,  0,  0);
                vtx(v, x1, y1, z1, color, u1, v0, overlay, light,  1,  0,  0);
                vtx(v, x1, y0, z1, color, u0, v0, overlay, light,  1,  0,  0);
            }
            case WEST -> {
                vtx(v, x0, y0, z1, color, u1, v0, overlay, light, -1,  0,  0);
                vtx(v, x0, y1, z1, color, u0, v0, overlay, light, -1,  0,  0);
                vtx(v, x0, y1, z0, color, u0, v1, overlay, light, -1,  0,  0);
                vtx(v, x0, y0, z0, color, u1, v1, overlay, light, -1,  0,  0);
            }
            case SOUTH -> {
                vtx(v, x1, y0, z1, color, u0, v0, overlay, light,  0,  0,  1);
                vtx(v, x1, y1, z1, color, u0, v1, overlay, light,  0,  0,  1);
                vtx(v, x0, y1, z1, color, u1, v1, overlay, light,  0,  0,  1);
                vtx(v, x0, y0, z1, color, u1, v0, overlay, light,  0,  0,  1);
            }
            case NORTH -> {
                vtx(v, x0, y0, z0, color, u1, v1, overlay, light,  0,  0, -1);
                vtx(v, x0, y1, z0, color, u1, v0, overlay, light,  0,  0, -1);
                vtx(v, x1, y1, z0, color, u0, v0, overlay, light,  0,  0, -1);
                vtx(v, x1, y0, z0, color, u0, v1, overlay, light,  0,  0, -1);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fluid tube rendering (vertical and horizontal)
    // ──────────────────────────────────────────────────────────────────────────

    /** Four vertical side faces only (no caps) — for a vertically aligned pipe section. */
    public static void renderVerticalFluidStill(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            int color, VertexConsumer v, int light, int overlay) {

        // East (x+)
        vtx(v, x1, y0, z0, color, u0, v1, overlay, light,  1,  0,  0);
        vtx(v, x1, y1, z0, color, u0, v0, overlay, light,  1,  0,  0);
        vtx(v, x1, y1, z1, color, u1, v0, overlay, light,  1,  0,  0);
        vtx(v, x1, y0, z1, color, u1, v1, overlay, light,  1,  0,  0);
        // West (x-)
        vtx(v, x0, y0, z1, color, u1, v1, overlay, light, -1,  0,  0);
        vtx(v, x0, y1, z1, color, u1, v0, overlay, light, -1,  0,  0);
        vtx(v, x0, y1, z0, color, u0, v0, overlay, light, -1,  0,  0);
        vtx(v, x0, y0, z0, color, u0, v1, overlay, light, -1,  0,  0);
        // South (z+)
        vtx(v, x1, y0, z1, color, u1, v1, overlay, light,  0,  0,  1);
        vtx(v, x1, y1, z1, color, u1, v0, overlay, light,  0,  0,  1);
        vtx(v, x0, y1, z1, color, u0, v0, overlay, light,  0,  0,  1);
        vtx(v, x0, y0, z1, color, u0, v1, overlay, light,  0,  0,  1);
        // North (z-)
        vtx(v, x0, y0, z0, color, u0, v1, overlay, light,  0,  0, -1);
        vtx(v, x0, y1, z0, color, u0, v0, overlay, light,  0,  0, -1);
        vtx(v, x1, y1, z0, color, u1, v0, overlay, light,  0,  0, -1);
        vtx(v, x1, y0, z0, color, u1, v1, overlay, light,  0,  0, -1);
    }

    public static void renderHorizontalFluidStill(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            int color, VertexConsumer v, int light, int overlay,
            float y0OffN, float y0OffS, float y0OffE, float y0OffW) {

        y1 = Math.max(y1, y0 + 5 * EPS);

        // Top
        vtx(v, x0, y1, z0, color, u0, v0, overlay, light,  0,  1,  0);
        vtx(v, x0, y1, z1, color, u0, v1, overlay, light,  0,  1,  0);
        vtx(v, x1, y1, z1, color, u1, v1, overlay, light,  0,  1,  0);
        vtx(v, x1, y1, z0, color, u1, v0, overlay, light,  0,  1,  0);
        // Bottom
        vtx(v, x1, y0, z0, color, u1, v0, overlay, light,  0, -1,  0);
        vtx(v, x1, y0, z1, color, u1, v1, overlay, light,  0, -1,  0);
        vtx(v, x0, y0, z1, color, u0, v1, overlay, light,  0, -1,  0);
        vtx(v, x0, y0, z0, color, u0, v0, overlay, light,  0, -1,  0);
        // East (x+)
        if (y1 - y0OffE > EPS) {
            vtx(v, x1, y0OffE, z0, color, u0, v1, overlay, light,  1, 0, 0);
            vtx(v, x1, y1,     z0, color, u0, v0, overlay, light,  1, 0, 0);
            vtx(v, x1, y1,     z1, color, u1, v0, overlay, light,  1, 0, 0);
            vtx(v, x1, y0OffE, z1, color, u1, v1, overlay, light,  1, 0, 0);
        }
        // West (x-)
        if (y1 - y0OffW > EPS) {
            vtx(v, x0, y0OffW, z1, color, u1, v1, overlay, light, -1, 0, 0);
            vtx(v, x0, y1,     z1, color, u1, v0, overlay, light, -1, 0, 0);
            vtx(v, x0, y1,     z0, color, u0, v0, overlay, light, -1, 0, 0);
            vtx(v, x0, y0OffW, z0, color, u0, v1, overlay, light, -1, 0, 0);
        }
        // South (z+)
        if (y1 - y0OffS > EPS) {
            vtx(v, x1, y0OffS, z1, color, u1, v1, overlay, light, 0, 0,  1);
            vtx(v, x1, y1,     z1, color, u1, v0, overlay, light, 0, 0,  1);
            vtx(v, x0, y1,     z1, color, u0, v0, overlay, light, 0, 0,  1);
            vtx(v, x0, y0OffS, z1, color, u0, v1, overlay, light, 0, 0,  1);
        }
        // North (z-)
        if (y1 - y0OffN > EPS) {
            vtx(v, x0, y0OffN, z0, color, u0, v1, overlay, light, 0, 0, -1);
            vtx(v, x0, y1,     z0, color, u0, v0, overlay, light, 0, 0, -1);
            vtx(v, x1, y1,     z0, color, u1, v0, overlay, light, 0, 0, -1);
            vtx(v, x1, y0OffN, z0, color, u1, v1, overlay, light, 0, 0, -1);
        }
    }

    public static void renderHorizontalFluidFlowing(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            int color, Direction flowDirection, VertexConsumer v, int light, int overlay,
            float y0OffN, float y0OffS, float y0OffE, float y0OffW) {

        y1 = Math.max(y1, y0 + 5 * EPS);

        if (flowDirection == Direction.NORTH) {
            vtx(v, x0, y1, z0, color, u1, v1, overlay, light,  0,  1,  0);
            vtx(v, x0, y1, z1, color, u1, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z1, color, u0, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z0, color, u0, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y0, z0, color, u1, v1, overlay, light,  0, -1,  0);
            vtx(v, x1, y0, z1, color, u1, v0, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z1, color, u0, v0, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z0, color, u0, v1, overlay, light,  0, -1,  0);
            if (y1 - y0OffE > EPS) {
                vtx(v, x1, y0OffE, z0, color, u0, v1, overlay, light,  1, 0, 0);
                vtx(v, x1, y1,     z0, color, u1, v1, overlay, light,  1, 0, 0);
                vtx(v, x1, y1,     z1, color, u1, v0, overlay, light,  1, 0, 0);
                vtx(v, x1, y0OffE, z1, color, u0, v0, overlay, light,  1, 0, 0);
            }
            if (y1 - y0OffW > EPS) {
                vtx(v, x0, y0OffW, z1, color, u1, v0, overlay, light, -1, 0, 0);
                vtx(v, x0, y1,     z1, color, u0, v0, overlay, light, -1, 0, 0);
                vtx(v, x0, y1,     z0, color, u0, v1, overlay, light, -1, 0, 0);
                vtx(v, x0, y0OffW, z0, color, u1, v1, overlay, light, -1, 0, 0);
            }
            if (y1 - y0OffS > EPS) {
                vtx(v, x1, y0OffS, z1, color, u0, v0, overlay, light, 0, 0,  1);
                vtx(v, x1, y1,     z1, color, u0, v1, overlay, light, 0, 0,  1);
                vtx(v, x0, y1,     z1, color, u1, v1, overlay, light, 0, 0,  1);
                vtx(v, x0, y0OffS, z1, color, u1, v0, overlay, light, 0, 0,  1);
            }
            if (y1 - y0OffN > EPS) {
                vtx(v, x0, y0OffN, z0, color, u1, v1, overlay, light, 0, 0, -1);
                vtx(v, x0, y1,     z0, color, u1, v0, overlay, light, 0, 0, -1);
                vtx(v, x1, y1,     z0, color, u0, v0, overlay, light, 0, 0, -1);
                vtx(v, x1, y0OffN, z0, color, u0, v1, overlay, light, 0, 0, -1);
            }
        }

        if (flowDirection == Direction.SOUTH) {
            vtx(v, x0, y1, z0, color, u0, v0, overlay, light,  0,  1,  0);
            vtx(v, x0, y1, z1, color, u0, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z1, color, u1, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z0, color, u1, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y0, z0, color, u0, v0, overlay, light,  0, -1,  0);
            vtx(v, x1, y0, z1, color, u0, v1, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z1, color, u1, v1, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z0, color, u1, v0, overlay, light,  0, -1,  0);
            if (y1 - y0OffE > EPS) {
                vtx(v, x1, y0OffE, z0, color, u1, v0, overlay, light,  1, 0, 0);
                vtx(v, x1, y1,     z0, color, u0, v0, overlay, light,  1, 0, 0);
                vtx(v, x1, y1,     z1, color, u0, v1, overlay, light,  1, 0, 0);
                vtx(v, x1, y0OffE, z1, color, u1, v1, overlay, light,  1, 0, 0);
            }
            if (y1 - y0OffW > EPS) {
                vtx(v, x0, y0OffW, z1, color, u0, v1, overlay, light, -1, 0, 0);
                vtx(v, x0, y1,     z1, color, u1, v1, overlay, light, -1, 0, 0);
                vtx(v, x0, y1,     z0, color, u1, v0, overlay, light, -1, 0, 0);
                vtx(v, x0, y0OffW, z0, color, u0, v0, overlay, light, -1, 0, 0);
            }
            if (y1 - y0OffS > EPS) {
                vtx(v, x1, y0OffS, z1, color, u1, v1, overlay, light, 0, 0,  1);
                vtx(v, x1, y1,     z1, color, u1, v0, overlay, light, 0, 0,  1);
                vtx(v, x0, y1,     z1, color, u0, v0, overlay, light, 0, 0,  1);
                vtx(v, x0, y0OffS, z1, color, u0, v1, overlay, light, 0, 0,  1);
            }
            if (y1 - y0OffN > EPS) {
                vtx(v, x0, y0OffN, z0, color, u0, v0, overlay, light, 0, 0, -1);
                vtx(v, x0, y1,     z0, color, u0, v1, overlay, light, 0, 0, -1);
                vtx(v, x1, y1,     z0, color, u1, v1, overlay, light, 0, 0, -1);
                vtx(v, x1, y0OffN, z0, color, u1, v0, overlay, light, 0, 0, -1);
            }
        }

        if (flowDirection == Direction.EAST) {
            vtx(v, x0, y1, z0, color, u1, v0, overlay, light,  0,  1,  0);
            vtx(v, x0, y1, z1, color, u0, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z1, color, u0, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z0, color, u1, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y0, z0, color, u0, v1, overlay, light,  0, -1,  0);
            vtx(v, x1, y0, z1, color, u1, v1, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z1, color, u1, v0, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z0, color, u0, v0, overlay, light,  0, -1,  0);
            if (y1 - y0OffE > EPS) {
                // FIX: v1 at bottom (y0OffE), v0 at top (y1) → animation scrolls downward
                vtx(v, x1, y0OffE, z0, color, u0, v1, overlay, light,  1, 0, 0);
                vtx(v, x1, y1,     z0, color, u0, v0, overlay, light,  1, 0, 0);
                vtx(v, x1, y1,     z1, color, u1, v0, overlay, light,  1, 0, 0);
                vtx(v, x1, y0OffE, z1, color, u1, v1, overlay, light,  1, 0, 0);
            }
            if (y1 - y0OffW > EPS) {
                vtx(v, x0, y0OffW, z1, color, u0, v0, overlay, light, -1, 0, 0);
                vtx(v, x0, y1,     z1, color, u0, v1, overlay, light, -1, 0, 0);
                vtx(v, x0, y1,     z0, color, u1, v1, overlay, light, -1, 0, 0);
                vtx(v, x0, y0OffW, z0, color, u1, v0, overlay, light, -1, 0, 0);
            }
            if (y1 - y0OffS > EPS) {
                vtx(v, x1, y0OffS, z1, color, u0, v1, overlay, light, 0, 0,  1);
                vtx(v, x1, y1,     z1, color, u1, v1, overlay, light, 0, 0,  1);
                vtx(v, x0, y1,     z1, color, u1, v0, overlay, light, 0, 0,  1);
                vtx(v, x0, y0OffS, z1, color, u0, v0, overlay, light, 0, 0,  1);
            }
            if (y1 - y0OffN > EPS) {
                vtx(v, x0, y0OffN, z0, color, u1, v0, overlay, light, 0, 0, -1);
                vtx(v, x0, y1,     z0, color, u0, v0, overlay, light, 0, 0, -1);
                vtx(v, x1, y1,     z0, color, u0, v1, overlay, light, 0, 0, -1);
                vtx(v, x1, y0OffN, z0, color, u1, v1, overlay, light, 0, 0, -1);
            }
        }

        if (flowDirection == Direction.WEST) {
            vtx(v, x0, y1, z0, color, u0, v1, overlay, light,  0,  1,  0);
            vtx(v, x0, y1, z1, color, u1, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z1, color, u1, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z0, color, u0, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y0, z0, color, u1, v0, overlay, light,  0, -1,  0);
            vtx(v, x1, y0, z1, color, u0, v0, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z1, color, u0, v1, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z0, color, u1, v1, overlay, light,  0, -1,  0);
            if (y1 - y0OffE > EPS) {
                // FIX: v0 at bottom (y0OffE), v1 at top (y1) → animation scrolls upward
                vtx(v, x1, y0OffE, z0, color, u1, v0, overlay, light,  1, 0, 0);
                vtx(v, x1, y1,     z0, color, u1, v1, overlay, light,  1, 0, 0);
                vtx(v, x1, y1,     z1, color, u0, v1, overlay, light,  1, 0, 0);
                vtx(v, x1, y0OffE, z1, color, u0, v0, overlay, light,  1, 0, 0);
            }
            if (y1 - y0OffW > EPS) {
                vtx(v, x0, y0OffW, z1, color, u1, v1, overlay, light, -1, 0, 0);
                vtx(v, x0, y1,     z1, color, u1, v0, overlay, light, -1, 0, 0);
                vtx(v, x0, y1,     z0, color, u0, v0, overlay, light, -1, 0, 0);
                vtx(v, x0, y0OffW, z0, color, u0, v1, overlay, light, -1, 0, 0);
            }
            if (y1 - y0OffS > EPS) {
                vtx(v, x1, y0OffS, z1, color, u1, v0, overlay, light, 0, 0,  1);
                vtx(v, x1, y1,     z1, color, u0, v0, overlay, light, 0, 0,  1);
                vtx(v, x0, y1,     z1, color, u0, v1, overlay, light, 0, 0,  1);
                vtx(v, x0, y0OffS, z1, color, u1, v1, overlay, light, 0, 0,  1);
            }
            if (y1 - y0OffN > EPS) {
                vtx(v, x0, y0OffN, z0, color, u0, v1, overlay, light, 0, 0, -1);
                vtx(v, x0, y1,     z0, color, u1, v1, overlay, light, 0, 0, -1);
                vtx(v, x1, y1,     z0, color, u1, v0, overlay, light, 0, 0, -1);
                vtx(v, x1, y0OffN, z0, color, u0, v0, overlay, light, 0, 0, -1);
            }
        }
    }

    public static void renderHorizontalFluidStillCentered(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            int color, VertexConsumer v, int light, int overlay,
            Direction direction) {

        // Top
        vtx(v, x0, y1, z0, color, u0, v0, overlay, light,  0,  1,  0);
        vtx(v, x0, y1, z1, color, u0, v1, overlay, light,  0,  1,  0);
        vtx(v, x1, y1, z1, color, u1, v1, overlay, light,  0,  1,  0);
        vtx(v, x1, y1, z0, color, u1, v0, overlay, light,  0,  1,  0);
        // Bottom
        vtx(v, x1, y0, z0, color, u1, v0, overlay, light,  0, -1,  0);
        vtx(v, x1, y0, z1, color, u1, v1, overlay, light,  0, -1,  0);
        vtx(v, x0, y0, z1, color, u0, v1, overlay, light,  0, -1,  0);
        vtx(v, x0, y0, z0, color, u0, v0, overlay, light,  0, -1,  0);
        // East/West — omit if connection runs along X axis
        if (direction != Direction.EAST && direction != Direction.WEST) {
            vtx(v, x1, y0, z0, color, u0, v1, overlay, light,  1, 0, 0);
            vtx(v, x1, y1, z0, color, u0, v0, overlay, light,  1, 0, 0);
            vtx(v, x1, y1, z1, color, u1, v0, overlay, light,  1, 0, 0);
            vtx(v, x1, y0, z1, color, u1, v1, overlay, light,  1, 0, 0);

            vtx(v, x0, y0, z1, color, u1, v1, overlay, light, -1, 0, 0);
            vtx(v, x0, y1, z1, color, u1, v0, overlay, light, -1, 0, 0);
            vtx(v, x0, y1, z0, color, u0, v0, overlay, light, -1, 0, 0);
            vtx(v, x0, y0, z0, color, u0, v1, overlay, light, -1, 0, 0);
        }
        // North/South — omit if connection runs along Z axis
        if (direction != Direction.NORTH && direction != Direction.SOUTH) {
            vtx(v, x1, y0, z1, color, u1, v1, overlay, light, 0, 0,  1);
            vtx(v, x1, y1, z1, color, u1, v0, overlay, light, 0, 0,  1);
            vtx(v, x0, y1, z1, color, u0, v0, overlay, light, 0, 0,  1);
            vtx(v, x0, y0, z1, color, u0, v1, overlay, light, 0, 0,  1);

            vtx(v, x0, y0, z0, color, u0, v1, overlay, light, 0, 0, -1);
            vtx(v, x0, y1, z0, color, u0, v0, overlay, light, 0, 0, -1);
            vtx(v, x1, y1, z0, color, u1, v0, overlay, light, 0, 0, -1);
            vtx(v, x1, y0, z0, color, u1, v1, overlay, light, 0, 0, -1);
        }
    }

    public static void renderFluidFlowingCentered(
            float x0, float x1, float z0, float z1, float y0, float y1,
            float u0, float u1, float v0, float v1,
            int color, Direction flowDirection, VertexConsumer v, int light, int overlay) {

        if (flowDirection == Direction.NORTH) {
            vtx(v, x1, y0, z0, color, u0, v1, overlay, light,  1,  0,  0);
            vtx(v, x1, y1, z0, color, u1, v1, overlay, light,  1,  0,  0);
            vtx(v, x1, y1, z1, color, u1, v0, overlay, light,  1,  0,  0);
            vtx(v, x1, y0, z1, color, u0, v0, overlay, light,  1,  0,  0);
            vtx(v, x0, y0, z1, color, u1, v0, overlay, light, -1,  0,  0);
            vtx(v, x0, y1, z1, color, u0, v0, overlay, light, -1,  0,  0);
            vtx(v, x0, y1, z0, color, u0, v1, overlay, light, -1,  0,  0);
            vtx(v, x0, y0, z0, color, u1, v1, overlay, light, -1,  0,  0);
            vtx(v, x0, y1, z0, color, u1, v1, overlay, light,  0,  1,  0);
            vtx(v, x0, y1, z1, color, u1, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z1, color, u0, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z0, color, u0, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y0, z0, color, u1, v1, overlay, light,  0, -1,  0);
            vtx(v, x1, y0, z1, color, u1, v0, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z1, color, u0, v0, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z0, color, u0, v1, overlay, light,  0, -1,  0);
        }

        if (flowDirection == Direction.SOUTH) {
            vtx(v, x1, y0, z0, color, u1, v0, overlay, light,  1,  0,  0);
            vtx(v, x1, y1, z0, color, u0, v0, overlay, light,  1,  0,  0);
            vtx(v, x1, y1, z1, color, u0, v1, overlay, light,  1,  0,  0);
            vtx(v, x1, y0, z1, color, u1, v1, overlay, light,  1,  0,  0);
            vtx(v, x0, y0, z1, color, u0, v1, overlay, light, -1,  0,  0);
            vtx(v, x0, y1, z1, color, u1, v1, overlay, light, -1,  0,  0);
            vtx(v, x0, y1, z0, color, u1, v0, overlay, light, -1,  0,  0);
            vtx(v, x0, y0, z0, color, u0, v0, overlay, light, -1,  0,  0);
            vtx(v, x0, y1, z0, color, u0, v0, overlay, light,  0,  1,  0);
            vtx(v, x0, y1, z1, color, u0, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z1, color, u1, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z0, color, u1, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y0, z0, color, u0, v0, overlay, light,  0, -1,  0);
            vtx(v, x1, y0, z1, color, u0, v1, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z1, color, u1, v1, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z0, color, u1, v0, overlay, light,  0, -1,  0);
        }

        if (flowDirection == Direction.EAST) {
            vtx(v, x1, y0, z1, color, u0, v1, overlay, light,  0,  0,  1);
            vtx(v, x1, y1, z1, color, u1, v1, overlay, light,  0,  0,  1);
            vtx(v, x0, y1, z1, color, u1, v0, overlay, light,  0,  0,  1);
            vtx(v, x0, y0, z1, color, u0, v0, overlay, light,  0,  0,  1);
            vtx(v, x0, y0, z0, color, u1, v0, overlay, light,  0,  0, -1);
            vtx(v, x0, y1, z0, color, u0, v0, overlay, light,  0,  0, -1);
            vtx(v, x1, y1, z0, color, u0, v1, overlay, light,  0,  0, -1);
            vtx(v, x1, y0, z0, color, u1, v1, overlay, light,  0,  0, -1);
            vtx(v, x0, y1, z0, color, u1, v0, overlay, light,  0,  1,  0);
            vtx(v, x0, y1, z1, color, u0, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z1, color, u0, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z0, color, u1, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y0, z0, color, u0, v1, overlay, light,  0, -1,  0);
            vtx(v, x1, y0, z1, color, u1, v1, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z1, color, u1, v0, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z0, color, u0, v0, overlay, light,  0, -1,  0);
        }

        if (flowDirection == Direction.WEST) {
            vtx(v, x1, y0, z1, color, u1, v0, overlay, light,  0,  0,  1);
            vtx(v, x1, y1, z1, color, u0, v0, overlay, light,  0,  0,  1);
            vtx(v, x0, y1, z1, color, u0, v1, overlay, light,  0,  0,  1);
            vtx(v, x0, y0, z1, color, u1, v1, overlay, light,  0,  0,  1);
            vtx(v, x0, y0, z0, color, u0, v1, overlay, light,  0,  0, -1);
            vtx(v, x0, y1, z0, color, u1, v1, overlay, light,  0,  0, -1);
            vtx(v, x1, y1, z0, color, u1, v0, overlay, light,  0,  0, -1);
            vtx(v, x1, y0, z0, color, u0, v0, overlay, light,  0,  0, -1);
            vtx(v, x0, y1, z0, color, u0, v1, overlay, light,  0,  1,  0);
            vtx(v, x0, y1, z1, color, u1, v1, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z1, color, u1, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y1, z0, color, u0, v0, overlay, light,  0,  1,  0);
            vtx(v, x1, y0, z0, color, u1, v0, overlay, light,  0, -1,  0);
            vtx(v, x1, y0, z1, color, u0, v0, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z1, color, u0, v1, overlay, light,  0, -1,  0);
            vtx(v, x0, y0, z0, color, u1, v1, overlay, light,  0, -1,  0);
        }

        if (flowDirection == Direction.UP) {
            vtx(v, x1, y0, z0, color, u0, v0, overlay, light,  1,  0,  0);
            vtx(v, x1, y1, z0, color, u0, v1, overlay, light,  1,  0,  0);
            vtx(v, x1, y1, z1, color, u1, v1, overlay, light,  1,  0,  0);
            vtx(v, x1, y0, z1, color, u1, v0, overlay, light,  1,  0,  0);
            vtx(v, x0, y0, z1, color, u0, v0, overlay, light, -1,  0,  0);
            vtx(v, x0, y1, z1, color, u0, v1, overlay, light, -1,  0,  0);
            vtx(v, x0, y1, z0, color, u1, v1, overlay, light, -1,  0,  0);
            vtx(v, x0, y0, z0, color, u1, v0, overlay, light, -1,  0,  0);
            vtx(v, x1, y0, z1, color, u0, v0, overlay, light,  0,  0,  1);
            vtx(v, x1, y1, z1, color, u0, v1, overlay, light,  0,  0,  1);
            vtx(v, x0, y1, z1, color, u1, v1, overlay, light,  0,  0,  1);
            vtx(v, x0, y0, z1, color, u1, v0, overlay, light,  0,  0,  1);
            vtx(v, x0, y0, z0, color, u0, v0, overlay, light,  0,  0, -1);
            vtx(v, x0, y1, z0, color, u0, v1, overlay, light,  0,  0, -1);
            vtx(v, x1, y1, z0, color, u1, v1, overlay, light,  0,  0, -1);
            vtx(v, x1, y0, z0, color, u1, v0, overlay, light,  0,  0, -1);
        }

        if (flowDirection == Direction.DOWN) {
            vtx(v, x1, y0, z0, color, u1, v1, overlay, light,  1,  0,  0);
            vtx(v, x1, y1, z0, color, u1, v0, overlay, light,  1,  0,  0);
            vtx(v, x1, y1, z1, color, u0, v0, overlay, light,  1,  0,  0);
            vtx(v, x1, y0, z1, color, u0, v1, overlay, light,  1,  0,  0);
            vtx(v, x0, y0, z1, color, u1, v1, overlay, light, -1,  0,  0);
            vtx(v, x0, y1, z1, color, u1, v0, overlay, light, -1,  0,  0);
            vtx(v, x0, y1, z0, color, u0, v0, overlay, light, -1,  0,  0);
            vtx(v, x0, y0, z0, color, u0, v1, overlay, light, -1,  0,  0);
            vtx(v, x1, y0, z1, color, u1, v1, overlay, light,  0,  0,  1);
            vtx(v, x1, y1, z1, color, u1, v0, overlay, light,  0,  0,  1);
            vtx(v, x0, y1, z1, color, u0, v0, overlay, light,  0,  0,  1);
            vtx(v, x0, y0, z1, color, u0, v1, overlay, light,  0,  0,  1);
            vtx(v, x0, y0, z0, color, u1, v1, overlay, light,  0,  0, -1);
            vtx(v, x0, y1, z0, color, u1, v0, overlay, light,  0,  0, -1);
            vtx(v, x1, y1, z0, color, u0, v0, overlay, light,  0,  0, -1);
            vtx(v, x1, y0, z0, color, u0, v1, overlay, light,  0,  0, -1);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Face cut-out rendering (ring face with a square hole at the pipe junction)
    // ──────────────────────────────────────────────────────────────────────────

    /** Linear UV interpolation helper. */
    static float interpolate(float outerStart, float outerEnd, float innerStart, float innerEnd, float value) {
        return ((value - outerStart) / (outerEnd - outerStart)) * (innerEnd - innerStart) + innerStart;
    }

    static void renderDownFaceCutOut(
            float x0, float x1, float z0, float z1, float y0,
            float xh0, float xh1, float zh0, float zh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light, int color) {

        float uh0 = interpolate(x0, x1, u0, u1, xh0);
        float uh1 = interpolate(x0, x1, u0, u1, xh1);
        float vh0 = interpolate(z0, z1, v0, v1, zh0);
        float vh1 = interpolate(z0, z1, v0, v1, zh1);

        vtx(v, x0,  y0, z0,  color, u0,  v0,  0, light,  0, -1, 0);
        vtx(v, xh0, y0, z0,  color, uh0, v0,  0, light,  0, -1, 0);
        vtx(v, xh0, y0, zh0, color, uh0, vh0, 0, light,  0, -1, 0);
        vtx(v, x0,  y0, zh0, color, u0,  vh0, 0, light,  0, -1, 0);

        vtx(v, xh0, y0, z0,  color, uh0, v0,  0, light,  0, -1, 0);
        vtx(v, xh1, y0, z0,  color, uh1, v0,  0, light,  0, -1, 0);
        vtx(v, xh1, y0, zh0, color, uh1, vh0, 0, light,  0, -1, 0);
        vtx(v, xh0, y0, zh0, color, uh0, vh0, 0, light,  0, -1, 0);

        vtx(v, xh1, y0, z0,  color, uh1, v0,  0, light,  0, -1, 0);
        vtx(v, x1,  y0, z0,  color, u1,  v0,  0, light,  0, -1, 0);
        vtx(v, x1,  y0, zh0, color, u1,  vh0, 0, light,  0, -1, 0);
        vtx(v, xh1, y0, zh0, color, uh1, vh0, 0, light,  0, -1, 0);

        vtx(v, xh1, y0, zh0, color, uh1, vh0, 0, light,  0, -1, 0);
        vtx(v, x1,  y0, zh0, color, u1,  vh0, 0, light,  0, -1, 0);
        vtx(v, x1,  y0, zh1, color, u1,  vh1, 0, light,  0, -1, 0);
        vtx(v, xh1, y0, zh1, color, uh1, vh1, 0, light,  0, -1, 0);

        vtx(v, xh1, y0, zh1, color, uh1, vh1, 0, light,  0, -1, 0);
        vtx(v, x1,  y0, zh1, color, u1,  vh1, 0, light,  0, -1, 0);
        vtx(v, x1,  y0, z1,  color, u1,  v1,  0, light,  0, -1, 0);
        vtx(v, xh1, y0, z1,  color, uh1, v1,  0, light,  0, -1, 0);

        vtx(v, xh0, y0, zh1, color, uh0, vh1, 0, light,  0, -1, 0);
        vtx(v, xh1, y0, zh1, color, uh1, vh1, 0, light,  0, -1, 0);
        vtx(v, xh1, y0, z1,  color, uh1, v1,  0, light,  0, -1, 0);
        vtx(v, xh0, y0, z1,  color, uh0, v1,  0, light,  0, -1, 0);

        vtx(v, x0,  y0, zh1, color, u0,  vh1, 0, light,  0, -1, 0);
        vtx(v, xh0, y0, zh1, color, uh0, vh1, 0, light,  0, -1, 0);
        vtx(v, xh0, y0, z1,  color, uh0, v1,  0, light,  0, -1, 0);
        vtx(v, x0,  y0, z1,  color, u0,  v1,  0, light,  0, -1, 0);

        vtx(v, x0,  y0, zh0, color, u0,  vh0, 0, light,  0, -1, 0);
        vtx(v, xh0, y0, zh0, color, uh0, vh0, 0, light,  0, -1, 0);
        vtx(v, xh0, y0, zh1, color, uh0, vh1, 0, light,  0, -1, 0);
        vtx(v, x0,  y0, zh1, color, u0,  vh1, 0, light,  0, -1, 0);
    }

    static void renderUpFaceCutOut(
            float x0, float x1, float z0, float z1, float y1,
            float xh0, float xh1, float zh0, float zh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light, int color) {

        float uh0 = interpolate(x0, x1, u0, u1, xh0);
        float uh1 = interpolate(x0, x1, u0, u1, xh1);
        float vh0 = interpolate(z0, z1, v0, v1, zh0);
        float vh1 = interpolate(z0, z1, v0, v1, zh1);

        vtx(v, x0,  y1, zh0, color, u0,  vh0, 0, light,  0, 1, 0);
        vtx(v, xh0, y1, zh0, color, uh0, vh0, 0, light,  0, 1, 0);
        vtx(v, xh0, y1, z0,  color, uh0, v0,  0, light,  0, 1, 0);
        vtx(v, x0,  y1, z0,  color, u0,  v0,  0, light,  0, 1, 0);

        vtx(v, xh0, y1, zh0, color, uh0, vh0, 0, light,  0, 1, 0);
        vtx(v, xh1, y1, zh0, color, uh1, vh0, 0, light,  0, 1, 0);
        vtx(v, xh1, y1, z0,  color, uh1, v0,  0, light,  0, 1, 0);
        vtx(v, xh0, y1, z0,  color, uh0, v0,  0, light,  0, 1, 0);

        vtx(v, xh1, y1, zh0, color, uh1, vh0, 0, light,  0, 1, 0);
        vtx(v, x1,  y1, zh0, color, u1,  vh0, 0, light,  0, 1, 0);
        vtx(v, x1,  y1, z0,  color, u1,  v0,  0, light,  0, 1, 0);
        vtx(v, xh1, y1, z0,  color, uh1, v0,  0, light,  0, 1, 0);

        vtx(v, xh1, y1, zh1, color, uh1, vh1, 0, light,  0, 1, 0);
        vtx(v, x1,  y1, zh1, color, u1,  vh1, 0, light,  0, 1, 0);
        vtx(v, x1,  y1, zh0, color, u1,  vh0, 0, light,  0, 1, 0);
        vtx(v, xh1, y1, zh0, color, uh1, vh0, 0, light,  0, 1, 0);

        vtx(v, xh1, y1, z1,  color, uh1, v1,  0, light,  0, 1, 0);
        vtx(v, x1,  y1, z1,  color, u1,  v1,  0, light,  0, 1, 0);
        vtx(v, x1,  y1, zh1, color, u1,  vh1, 0, light,  0, 1, 0);
        vtx(v, xh1, y1, zh1, color, uh1, vh1, 0, light,  0, 1, 0);

        vtx(v, xh0, y1, z1,  color, uh0, v1,  0, light,  0, 1, 0);
        vtx(v, xh1, y1, z1,  color, uh1, v1,  0, light,  0, 1, 0);
        vtx(v, xh1, y1, zh1, color, uh1, vh1, 0, light,  0, 1, 0);
        vtx(v, xh0, y1, zh1, color, uh0, vh1, 0, light,  0, 1, 0);

        vtx(v, x0,  y1, z1,  color, u0,  v1,  0, light,  0, 1, 0);
        vtx(v, xh0, y1, z1,  color, uh0, v1,  0, light,  0, 1, 0);
        vtx(v, xh0, y1, zh1, color, uh0, vh1, 0, light,  0, 1, 0);
        vtx(v, x0,  y1, zh1, color, u0,  vh1, 0, light,  0, 1, 0);

        vtx(v, x0,  y1, zh1, color, u0,  vh1, 0, light,  0, 1, 0);
        vtx(v, xh0, y1, zh1, color, uh0, vh1, 0, light,  0, 1, 0);
        vtx(v, xh0, y1, zh0, color, uh0, vh0, 0, light,  0, 1, 0);
        vtx(v, x0,  y1, zh0, color, u0,  vh0, 0, light,  0, 1, 0);
    }

    static void renderEastFaceCutOut(
            float z0, float z1, float y0, float y1, float x1,
            float zh0, float zh1, float yh0, float yh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light, int color) {

        float uh0 = interpolate(y0, y1, u0, u1, yh0);
        float uh1 = interpolate(y0, y1, u0, u1, yh1);
        float vh0 = interpolate(z0, z1, v0, v1, zh0);
        float vh1 = interpolate(z0, z1, v0, v1, zh1);

        vtx(v, x1, yh1, zh1, color, uh1, vh1, 0, light,  1, 0, 0);
        vtx(v, x1, y1,  zh1, color, u1,  vh1, 0, light,  1, 0, 0);
        vtx(v, x1, y1,  z1,  color, u1,  v1,  0, light,  1, 0, 0);
        vtx(v, x1, yh1, z1,  color, uh1, v1,  0, light,  1, 0, 0);

        vtx(v, x1, yh1, zh1, color, uh1, vh1, 0, light,  1, 0, 0);
        vtx(v, x1, yh1, zh0, color, uh1, vh0, 0, light,  1, 0, 0);
        vtx(v, x1, y1,  zh0, color, u1,  vh0, 0, light,  1, 0, 0);
        vtx(v, x1, y1,  zh1, color, u1,  vh1, 0, light,  1, 0, 0);

        vtx(v, x1, yh1, zh1, color, uh1, vh1, 0, light,  1, 0, 0);
        vtx(v, x1, yh1, z1,  color, uh1, v1,  0, light,  1, 0, 0);
        vtx(v, x1, yh0, z1,  color, uh0, v1,  0, light,  1, 0, 0);
        vtx(v, x1, yh0, zh1, color, uh0, vh1, 0, light,  1, 0, 0);

        vtx(v, x1, yh0, zh1, color, uh0, vh1, 0, light,  1, 0, 0);
        vtx(v, x1, yh0, z1,  color, uh0, v1,  0, light,  1, 0, 0);
        vtx(v, x1, y0,  z1,  color, u0,  v1,  0, light,  1, 0, 0);
        vtx(v, x1, y0,  zh1, color, u0,  vh1, 0, light,  1, 0, 0);

        vtx(v, x1, yh0, zh0, color, uh0, vh0, 0, light,  1, 0, 0);
        vtx(v, x1, yh0, zh1, color, uh0, vh1, 0, light,  1, 0, 0);
        vtx(v, x1, y0,  zh1, color, u0,  vh1, 0, light,  1, 0, 0);
        vtx(v, x1, y0,  zh0, color, u0,  vh0, 0, light,  1, 0, 0);

        vtx(v, x1, yh0, z0,  color, uh0, v0,  0, light,  1, 0, 0);
        vtx(v, x1, yh0, zh0, color, uh0, vh0, 0, light,  1, 0, 0);
        vtx(v, x1, y0,  zh0, color, u0,  vh0, 0, light,  1, 0, 0);
        vtx(v, x1, y0,  z0,  color, u0,  v0,  0, light,  1, 0, 0);

        vtx(v, x1, yh1, z0,  color, uh1, v0,  0, light,  1, 0, 0);
        vtx(v, x1, yh1, zh0, color, uh1, vh0, 0, light,  1, 0, 0);
        vtx(v, x1, yh0, zh0, color, uh0, vh0, 0, light,  1, 0, 0);
        vtx(v, x1, yh0, z0,  color, uh0, v0,  0, light,  1, 0, 0);

        vtx(v, x1, y1,  z0,  color, u1,  v0,  0, light,  1, 0, 0);
        vtx(v, x1, y1,  zh0, color, u1,  vh0, 0, light,  1, 0, 0);
        vtx(v, x1, yh1, zh0, color, uh1, vh0, 0, light,  1, 0, 0);
        vtx(v, x1, yh1, z0,  color, uh1, v0,  0, light,  1, 0, 0);
    }

    static void renderWestFaceCutOut(
            float z0, float z1, float y0, float y1, float x0,
            float zh0, float zh1, float yh0, float yh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light, int color) {

        float uh0 = interpolate(y0, y1, u0, u1, yh0);
        float uh1 = interpolate(y0, y1, u0, u1, yh1);
        float vh0 = interpolate(z0, z1, v0, v1, zh0);
        float vh1 = interpolate(z0, z1, v0, v1, zh1);

        vtx(v, x0, yh1, z1,  color, uh1, v1,  0, light, -1, 0, 0);
        vtx(v, x0, y1,  z1,  color, uh1, v1,  0, light, -1, 0, 0);
        vtx(v, x0, y1,  zh1, color, u1,  vh1, 0, light, -1, 0, 0);
        vtx(v, x0, yh1, zh1, color, uh1, vh1, 0, light, -1, 0, 0);

        vtx(v, x0, y1,  zh1, color, u1,  vh1, 0, light, -1, 0, 0);
        vtx(v, x0, y1,  zh0, color, u1,  vh0, 0, light, -1, 0, 0);
        vtx(v, x0, yh1, zh0, color, uh1, vh0, 0, light, -1, 0, 0);
        vtx(v, x0, yh1, zh1, color, uh1, vh1, 0, light, -1, 0, 0);

        vtx(v, x0, yh0, zh1, color, uh0, vh1, 0, light, -1, 0, 0);
        vtx(v, x0, yh0, z1,  color, uh0, v1,  0, light, -1, 0, 0);
        vtx(v, x0, yh1, z1,  color, uh1, v1,  0, light, -1, 0, 0);
        vtx(v, x0, yh1, zh1, color, uh1, vh1, 0, light, -1, 0, 0);

        vtx(v, x0, y0,  zh1, color, u0,  vh1, 0, light, -1, 0, 0);
        vtx(v, x0, y0,  z1,  color, u0,  v1,  0, light, -1, 0, 0);
        vtx(v, x0, yh0, z1,  color, uh0, v1,  0, light, -1, 0, 0);
        vtx(v, x0, yh0, zh1, color, uh0, vh1, 0, light, -1, 0, 0);

        vtx(v, x0, y0,  zh0, color, u0,  vh0, 0, light, -1, 0, 0);
        vtx(v, x0, y0,  zh1, color, u0,  vh1, 0, light, -1, 0, 0);
        vtx(v, x0, yh0, zh1, color, uh0, vh1, 0, light, -1, 0, 0);
        vtx(v, x0, yh0, zh0, color, uh0, vh0, 0, light, -1, 0, 0);

        vtx(v, x0, y0,  z0,  color, u0,  v0,  0, light, -1, 0, 0);
        vtx(v, x0, y0,  zh0, color, u0,  vh0, 0, light, -1, 0, 0);
        vtx(v, x0, yh0, zh0, color, uh0, vh0, 0, light, -1, 0, 0);
        vtx(v, x0, yh0, z0,  color, uh0, v0,  0, light, -1, 0, 0);

        vtx(v, x0, yh0, z0,  color, uh0, v0,  0, light, -1, 0, 0);
        vtx(v, x0, yh0, zh0, color, uh0, vh0, 0, light, -1, 0, 0);
        vtx(v, x0, yh1, zh0, color, uh1, vh0, 0, light, -1, 0, 0);
        vtx(v, x0, yh1, z0,  color, uh1, v0,  0, light, -1, 0, 0);

        vtx(v, x0, yh1, z0,  color, uh1, v0,  0, light, -1, 0, 0);
        vtx(v, x0, yh1, zh0, color, uh1, vh0, 0, light, -1, 0, 0);
        vtx(v, x0, y1,  zh0, color, u1,  vh0, 0, light, -1, 0, 0);
        vtx(v, x0, y1,  z0,  color, u1,  v0,  0, light, -1, 0, 0);
    }

    static void renderSouthFaceCutOut(
            float x0, float x1, float y0, float y1, float z1,
            float xh0, float xh1, float yh0, float yh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light, int color) {

        float uh0 = interpolate(x0, x1, u0, u1, xh0);
        float uh1 = interpolate(x0, x1, u0, u1, xh1);
        float vh0 = interpolate(y0, y1, v0, v1, yh0);
        float vh1 = interpolate(y0, y1, v0, v1, yh1);

        vtx(v, x1,  y0,  z1, color, u1,  v0,  0, light, 0, 0, 1);
        vtx(v, x1,  yh0, z1, color, u1,  vh0, 0, light, 0, 0, 1);
        vtx(v, xh1, yh0, z1, color, uh1, vh0, 0, light, 0, 0, 1);
        vtx(v, xh1, y0,  z1, color, uh1, v0,  0, light, 0, 0, 1);

        vtx(v, x1,  yh0, z1, color, u1,  vh0, 0, light, 0, 0, 1);
        vtx(v, x1,  yh1, z1, color, u1,  vh1, 0, light, 0, 0, 1);
        vtx(v, xh1, yh1, z1, color, uh1, vh1, 0, light, 0, 0, 1);
        vtx(v, xh1, yh0, z1, color, uh1, vh0, 0, light, 0, 0, 1);

        vtx(v, x1,  yh1, z1, color, u1,  vh1, 0, light, 0, 0, 1);
        vtx(v, x1,  y1,  z1, color, u1,  v1,  0, light, 0, 0, 1);
        vtx(v, xh1, y1,  z1, color, uh1, v1,  0, light, 0, 0, 1);
        vtx(v, xh1, yh1, z1, color, uh1, vh1, 0, light, 0, 0, 1);

        vtx(v, xh1, y0,  z1, color, uh1, v0,  0, light, 0, 0, 1);
        vtx(v, xh1, yh0, z1, color, uh1, vh0, 0, light, 0, 0, 1);
        vtx(v, xh0, yh0, z1, color, uh0, vh0, 0, light, 0, 0, 1);
        vtx(v, xh0, y0,  z1, color, uh0, v0,  0, light, 0, 0, 1);

        vtx(v, xh1, yh1, z1, color, uh1, vh1, 0, light, 0, 0, 1);
        vtx(v, xh1, y1,  z1, color, uh1, v1,  0, light, 0, 0, 1);
        vtx(v, xh0, y1,  z1, color, uh0, v1,  0, light, 0, 0, 1);
        vtx(v, xh0, yh1, z1, color, uh0, vh1, 0, light, 0, 0, 1);

        vtx(v, xh0, y0,  z1, color, uh0, v0,  0, light, 0, 0, 1);
        vtx(v, xh0, yh0, z1, color, uh0, vh0, 0, light, 0, 0, 1);
        vtx(v, x0,  yh0, z1, color, u0,  vh0, 0, light, 0, 0, 1);
        vtx(v, x0,  y0,  z1, color, u0,  v0,  0, light, 0, 0, 1);

        vtx(v, xh0, yh0, z1, color, uh0, vh0, 0, light, 0, 0, 1);
        vtx(v, xh0, yh1, z1, color, uh0, vh1, 0, light, 0, 0, 1);
        vtx(v, x0,  yh1, z1, color, u0,  vh1, 0, light, 0, 0, 1);
        vtx(v, x0,  yh0, z1, color, u0,  vh0, 0, light, 0, 0, 1);

        vtx(v, xh0, yh1, z1, color, uh0, vh1, 0, light, 0, 0, 1);
        vtx(v, xh0, y1,  z1, color, uh0, v1,  0, light, 0, 0, 1);
        vtx(v, x0,  y1,  z1, color, u0,  v1,  0, light, 0, 0, 1);
        vtx(v, x0,  yh1, z1, color, u0,  vh1, 0, light, 0, 0, 1);
    }

    static void renderNorthFaceCutOut(
            float x0, float x1, float y0, float y1, float z0,
            float xh0, float xh1, float yh0, float yh1,
            float u0, float u1, float v0, float v1,
            VertexConsumer v, int light, int color) {

        float uh0 = interpolate(x0, x1, u0, u1, xh0);
        float uh1 = interpolate(x0, x1, u0, u1, xh1);
        float vh0 = interpolate(y0, y1, v0, v1, yh0);
        float vh1 = interpolate(y0, y1, v0, v1, yh1);

        vtx(v, xh1, y0,  z0, color, uh1, v0,  0, light, 0, 0, -1);
        vtx(v, xh1, yh0, z0, color, uh1, vh0, 0, light, 0, 0, -1);
        vtx(v, x1,  yh0, z0, color, u1,  vh0, 0, light, 0, 0, -1);
        vtx(v, x1,  y0,  z0, color, u1,  v0,  0, light, 0, 0, -1);

        vtx(v, xh1, yh0, z0, color, uh1, vh0, 0, light, 0, 0, -1);
        vtx(v, xh1, yh1, z0, color, uh1, vh1, 0, light, 0, 0, -1);
        vtx(v, x1,  yh1, z0, color, u1,  vh1, 0, light, 0, 0, -1);
        vtx(v, x1,  yh0, z0, color, u1,  vh0, 0, light, 0, 0, -1);

        vtx(v, xh1, yh1, z0, color, uh1, vh1, 0, light, 0, 0, -1);
        vtx(v, xh1, y1,  z0, color, uh1, v1,  0, light, 0, 0, -1);
        vtx(v, x1,  y1,  z0, color, u1,  v1,  0, light, 0, 0, -1);
        vtx(v, x1,  yh1, z0, color, u1,  vh1, 0, light, 0, 0, -1);

        vtx(v, xh0, y0,  z0, color, uh0, v0,  0, light, 0, 0, -1);
        vtx(v, xh0, yh0, z0, color, uh0, vh0, 0, light, 0, 0, -1);
        vtx(v, xh1, yh0, z0, color, uh1, vh0, 0, light, 0, 0, -1);
        vtx(v, xh1, y0,  z0, color, uh1, v0,  0, light, 0, 0, -1);

        vtx(v, xh0, yh1, z0, color, uh0, vh1, 0, light, 0, 0, -1);
        vtx(v, xh0, y1,  z0, color, uh0, v1,  0, light, 0, 0, -1);
        vtx(v, xh1, y1,  z0, color, uh1, v1,  0, light, 0, 0, -1);
        vtx(v, xh1, yh1, z0, color, uh1, vh1, 0, light, 0, 0, -1);

        vtx(v, x0,  y0,  z0, color, u0,  v0,  0, light, 0, 0, -1);
        vtx(v, x0,  yh0, z0, color, u0,  vh0, 0, light, 0, 0, -1);
        vtx(v, xh0, yh0, z0, color, uh0, vh0, 0, light, 0, 0, -1);
        vtx(v, xh0, y0,  z0, color, uh0, v0,  0, light, 0, 0, -1);

        vtx(v, x0,  yh0, z0, color, u0,  vh0, 0, light, 0, 0, -1);
        vtx(v, x0,  yh1, z0, color, u0,  vh1, 0, light, 0, 0, -1);
        vtx(v, xh0, yh1, z0, color, uh0, vh1, 0, light, 0, 0, -1);
        vtx(v, xh0, yh0, z0, color, uh0, vh0, 0, light, 0, 0, -1);

        vtx(v, x0,  yh1, z0, color, u0,  vh1, 0, light, 0, 0, -1);
        vtx(v, x0,  y1,  z0, color, u0,  v1,  0, light, 0, 0, -1);
        vtx(v, xh0, y1,  z0, color, uh0, v1,  0, light, 0, 0, -1);
        vtx(v, xh0, yh1, z0, color, uh0, vh1, 0, light, 0, 0, -1);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fluid rendering dispatch
    // ──────────────────────────────────────────────────────────────────────────

    public static boolean shouldRenderCentered(Fluid f) {
        return f.getFluidType().isLighterThanAir();
    }

    /**
     * Computes the neighbour-adjusted half-width for a connection's outer face.
     * Returns 0 if the fill is below EPS.
     */
    private static float neighbourHalfWidth(float fill) {
        return (fill > EPS) ? halfWidth(fill) : 0f;
    }

    public static void renderFluids(EntityPipe tile, VertexConsumer v, int light, int overlay) {
        BlockState tileState = tile.getLevel().getBlockState(tile.getBlockPos());
        if (!(tileState.getBlock() instanceof BlockPipe)) return;

        if (tile.tank.isEmpty()) return;

        float u0f = tile.renderData.spriteFLowing.getU0();
        float u1f = tile.renderData.spriteFLowing.getU1();
        float v0f = tile.renderData.spriteFLowing.getV0();
        float v1f = tile.renderData.spriteFLowing.getV1();

        float u0s = tile.renderData.spriteStill.getU0();
        float u1s = tile.renderData.spriteStill.getU1();
        float v0s = tile.renderData.spriteStill.getV0();
        float v1s = tile.renderData.spriteStill.getV1();

        int color = tile.renderData.color;

        boolean hasHorizontal =
                tile.connections.get(Direction.NORTH).isEnabled(tileState) ||
                        tile.connections.get(Direction.SOUTH).isEnabled(tileState) ||
                        tile.connections.get(Direction.WEST).isEnabled(tileState)  ||
                        tile.connections.get(Direction.EAST).isEnabled(tileState);

        // ── Center section ──────────────────────────────────────────────────
        if (!hasHorizontal) {
            // Vertical-only pipe: no horizontal connections
            float relativeFill = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
            float actualW = halfWidth(relativeFill);
            float x0 = -actualW, x1 = actualW;
            float z0 = -actualW, z1 = actualW;
            float y0 = -0.25f,   y1 = 0.25f;

            // Determine flow direction for animation
            Direction outFlow = null, inFlow = null;
            int numOut = 0, numIn = 0;
            for (Direction d : new Direction[]{Direction.UP, Direction.DOWN}) {
                PipeConnection conn = tile.connections.get(d);
                if (conn.getsInputFromInside) { outFlow = d; numOut++; }
                if (conn.getsInputFromOutside) { inFlow = d.getOpposite(); numIn++; }
            }

            if (numOut == 1) {
                renderFluidFlowingCentered(x0, x1, z0, z1, y0, y1, u0f, u1f, v0f, v1f, color, outFlow, v, light, overlay);
            } else if (numIn == 1) {
                renderFluidFlowingCentered(x0, x1, z0, z1, y0, y1, u0f, u1f, v0f, v1f, color, inFlow, v, light, overlay);
            } else {
                renderVerticalFluidStill(x0, x1, z0, z1, y0, y1, u0s, u1s, v0s, v1s, color, v, light, overlay);
            }

            // Top cap — render ring if connected pipe is narrower
            float fillUp = (float) tile.connections.get(Direction.UP).tank.getFluidAmount()
                    / tile.connections.get(Direction.UP).tank.getTankCapacity(0);
            float wUp = neighbourHalfWidth(fillUp);
            if (wUp + EPS < actualW)
                renderUpFaceCutOut(x0, x1, z0, z1, y1, -wUp, wUp, -wUp, wUp, u0s, u1s, v0s, v1s, v, light, color);

            // Bottom cap
            float fillDn = (float) tile.connections.get(Direction.DOWN).tank.getFluidAmount()
                    / tile.connections.get(Direction.DOWN).tank.getTankCapacity(0);
            float wDn = neighbourHalfWidth(fillDn);
            if (wDn - EPS < actualW)
                renderDownFaceCutOut(x0, x1, z0, z1, y0, -wDn, wDn, -wDn, wDn, u0s, u1s, v0s, v1s, v, light, color);

        } else if (!shouldRenderCentered(tile.tank.getFluid().getFluid())) {
            // Horizontal pipe, gravity-settled fluid
            float x0 = -0.25f + EPS, x1 = 0.25f - EPS;
            float z0 = -0.25f + EPS, z1 = 0.25f - EPS;
            float y0 = -0.25f + EPS;
            float y1 = y0 - 2 * EPS + 0.5f * (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
            float y0OffN = y0, y0OffS = y0, y0OffE = y0, y0OffW = y0;

            Direction outFlow = null, inFlow = null;
            int numOut = 0, numIn = 0;

            for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                PipeConnection conn = tile.connections.get(d);
                if (!conn.isEnabled(tileState)) continue;
                float offset = y0 - 2 * EPS + 0.5f * (float) conn.tank.getFluidAmount() / conn.tank.getCapacity();
                if (d == Direction.NORTH) y0OffN = offset;
                if (d == Direction.SOUTH) y0OffS = offset;
                if (d == Direction.EAST)  y0OffE = offset;
                if (d == Direction.WEST)  y0OffW = offset;
                if (conn.getsInputFromInside) { numOut++; outFlow = d; }
                if (conn.getsInputFromOutside) { numIn++; inFlow = d.getOpposite(); }
            }

            if (numOut == 1) {
                renderHorizontalFluidFlowing(x0, x1, z0, z1, y0, y1, u0f, u1f, v0f, v1f,
                        color, outFlow, v, light, overlay, y0OffN, y0OffS, y0OffE, y0OffW);
            } else if (numIn == 1) {
                renderHorizontalFluidFlowing(x0, x1, z0, z1, y0, y1, u0f, u1f, v0f, v1f,
                        color, inFlow, v, light, overlay, y0OffN, y0OffS, y0OffE, y0OffW);
            } else {
                renderHorizontalFluidStill(x0, x1, z0, z1, y0, y1, u0s, u1s, v0s, v1s,
                        color, v, light, overlay, y0OffN, y0OffS, y0OffE, y0OffW);
            }

            if (tile.connections.get(Direction.UP).isEnabled(tileState)) {
                float relativeFill = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
                float actualW = halfWidth(relativeFill);
                float cy0 = -0.25f - EPS + 0.5f * relativeFill;
                float cx0 = -actualW, cx1 = actualW, cz0 = -actualW, cz1 = actualW;
                PipeConnection up = tile.connections.get(Direction.UP);
                if (up.getsInputFromInside)
                    renderFluidFlowingCentered(cx0, cx1, cz0, cz1, cy0, 0.25f, u0f, u1f, v0f, v1f, color, Direction.UP, v, light, overlay);
                if (up.outputsToInside)
                    renderFluidFlowingCentered(cx0, cx1, cz0, cz1, cy0, 0.25f, u0f, u1f, v0f, v1f, color, Direction.DOWN, v, light, overlay);
            }

        } else {
            // Horizontal pipe, centered (lighter-than-air) fluid
            float relativeFill = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
            float actualW = halfWidth(relativeFill);
            Direction outFlow = null, inFlow = null;
            int numOut = 0, numIn = 0;

            for (Direction d : Direction.values()) {
                PipeConnection conn = tile.connections.get(d);
                if (!conn.isEnabled(tileState)) continue;

                float cx0 = -actualW, cx1 = actualW, cy0 = -actualW, cy1 = actualW, cz0 = -actualW, cz1 = actualW;

                if (d == Direction.UP)    { cy0 = actualW;  cy1 = 0.25f; }
                if (d == Direction.DOWN)  { cy0 = -0.25f;   cy1 = -actualW; }
                if (d == Direction.EAST)  { cx0 = actualW;  cx1 = 0.25f; }
                if (d == Direction.WEST)  { cx0 = -0.25f;   cx1 = -actualW; }
                if (d == Direction.SOUTH) { cz0 = actualW;  cz1 = 0.25f; }
                if (d == Direction.NORTH) { cz0 = -0.25f;   cz1 = -actualW; }

                // Cap faces at junction with neighbouring pipe
                float fillConn = (float) conn.tank.getFluidAmount() / conn.tank.getTankCapacity(0);
                // BUG FIX: was `actualWconn * actualWconn` (always 0); must use relativeFillConn.
                float wConn = (fillConn > EPS) ? halfWidth(fillConn) : 0f;

                if (d == Direction.UP    && wConn - EPS < actualW)
                    renderUpFaceCutOut(cx0, cx1, cz0, cz1, cy1, -wConn, wConn, -wConn, wConn, u0s, u1s, v0s, v1s, v, light, color);
                if (d == Direction.DOWN  && wConn - EPS < actualW)
                    renderDownFaceCutOut(cx0, cx1, cz0, cz1, cy0, -wConn, wConn, -wConn, wConn, u0s, u1s, v0s, v1s, v, light, color);
                if (d == Direction.EAST  && wConn - EPS < actualW)
                    renderEastFaceCutOut(cz0, cz1, cy0, cy1, cx1, -wConn, wConn, -wConn, wConn, u0s, u1s, v0s, v1s, v, light, color);
                if (d == Direction.WEST  && wConn - EPS < actualW)
                    renderWestFaceCutOut(cz0, cz1, cy0, cy1, cx0, -wConn, wConn, -wConn, wConn, u0s, u1s, v0s, v1s, v, light, color);
                if (d == Direction.SOUTH && wConn - EPS < actualW)
                    renderSouthFaceCutOut(cx0, cx1, cy0, cy1, cz1, -wConn, wConn, -wConn, wConn, u0s, u1s, v0s, v1s, v, light, color);
                if (d == Direction.NORTH && wConn - EPS < actualW)
                    renderNorthFaceCutOut(cx0, cx1, cy0, cy1, cz0, -wConn, wConn, -wConn, wConn, u0s, u1s, v0s, v1s, v, light, color);

                float vMid = v0f + (0.5f - actualW * 2) * (v1f - v0f);
                if (conn.outputsToInside) {
                    numIn++;
                    inFlow = d;
                    renderFluidFlowingCentered(cx0, cx1, cz0, cz1, cy0, cy1, u0f, u1f, v0f, vMid, color, d.getOpposite(), v, light, overlay);
                } else if (conn.getsInputFromInside) {
                    numOut++;
                    outFlow = d;
                    renderFluidFlowingCentered(cx0, cx1, cz0, cz1, cy0, cy1, u0f, u1f, v1f - vMid + v0f, v1f, color, d, v, light, overlay);
                } else {
                    renderVerticalFluidStill(cx0, cx1, cz0, cz1, cy0, cy1, u0s, u1s, v0s, v1s, color, v, light, overlay);
                }
            }

            // Center cube
            float cx0 = -actualW, cx1 = actualW, cy0 = -actualW, cy1 = actualW, cz0 = -actualW, cz1 = actualW;
            float vMidCenter = v0f + (0.5f - actualW * 2) * (v1f - v0f);
            float vMidCenter2 = v1f - (0.5f - actualW * 2) * (v1f - v0f);
            if (numOut == 1) {
                renderFluidFlowingCentered(cx0, cx1, cz0, cz1, cy0, cy1, u0f, u1f, vMidCenter, vMidCenter2, color, outFlow, v, light, overlay);
                if (!tile.connections.get(outFlow.getOpposite()).isEnabled(tileState) ||
                        tile.connections.get(outFlow.getOpposite()).tank.isEmpty())
                    renderFluidCubeFacebyDirection(cx0, cx1, cz0, cz1, cy0, cy1, u0s, u1s, v0s, v1s, outFlow.getOpposite(), color, v, light, overlay);
            } else if (numIn == 1) {
                renderFluidFlowingCentered(cx0, cx1, cz0, cz1, cy0, cy1, u0f, u1f, vMidCenter, vMidCenter2, color, inFlow.getOpposite(), v, light, overlay);
                if (!tile.connections.get(inFlow.getOpposite()).isEnabled(tileState) ||
                        tile.connections.get(inFlow.getOpposite()).tank.isEmpty())
                    renderFluidCubeFacebyDirection(cx0, cx1, cz0, cz1, cy0, cy1, u0s, u1s, v0s, v1s, inFlow.getOpposite(), color, v, light, overlay);
            } else {
                renderFluidCubeStill(cx0, cx1, cz0, cz1, cy0, cy1, u0s, u1s, v0s, v1s, color, v, light, overlay);
            }
        }

        // ── Connection arm rendering ─────────────────────────────────────────
        renderConnectionArm(tile, tileState, Direction.UP,    v, light, overlay);
        renderConnectionArm(tile, tileState, Direction.DOWN,  v, light, overlay);
        renderConnectionArm(tile, tileState, Direction.WEST,  v, light, overlay);
        renderConnectionArm(tile, tileState, Direction.EAST,  v, light, overlay);
        renderConnectionArm(tile, tileState, Direction.SOUTH, v, light, overlay);
        renderConnectionArm(tile, tileState, Direction.NORTH, v, light, overlay);
    }

    /**
     * Renders a single directional pipe arm between the center and the block face.
     * Extracted from the original per-direction copy-paste blocks.
     */
    private static void renderConnectionArm(EntityPipe tile, BlockState tileState,
                                            Direction dir, VertexConsumer v, int light, int overlay) {
        PipeConnection conn = tile.connections.get(dir);
        if (!conn.isEnabled(tileState)) return;

        UVSet f = UVSet.flowing(conn);
        UVSet s = UVSet.still(conn);
        int color = conn.renderData.color;

        int fluidInTank = conn.tank.getFluidAmount();
        if (fluidInTank <= 0) return;

        float relativeFill = (float) fluidInTank / conn.tank.getCapacity();
        float halfV = 0.5f * (f.v1() - f.v0());

        if (dir == Direction.UP || dir == Direction.DOWN) {
            float sign = (dir == Direction.UP) ? 1f : -1f;
            float y0 = (dir == Direction.UP) ?  0.25f - EPS : -0.5f;
            float y1 = (dir == Direction.UP) ?  0.5f        : -0.25f + EPS;

            float actualW = halfWidth(relativeFill);
            float x0 = -actualW, x1 = actualW, z0 = -actualW, z1 = actualW;

            if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                renderFluidFlowingCentered(x0, x1, z0, z1, y0, y1,
                        f.u0(), f.u1(), f.v0() + halfV, f.v1(),
                        color, dir.getOpposite(), v, light, overlay);
            } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                renderFluidFlowingCentered(x0, x1, z0, z1, y0, y1,
                        f.u0(), f.u1(), f.v0(), f.v1() - halfV,
                        color, dir, v, light, overlay);
            } else {
                renderVerticalFluidStill(x0, x1, z0, z1, y0, y1, s.u0(), s.u1(), s.v0(), s.v1(), color, v, light, overlay);
            }

            // Outer cap
            float fillOut = (float) conn.neighborFluidHandler().getFluidInTank(0).getAmount()
                    / conn.neighborFluidHandler().getTankCapacity(0);
            float wOut = neighbourHalfWidth(fillOut);
            if (wOut < actualW) {
                if (dir == Direction.UP)
                    renderUpFaceCutOut(x0, x1, z0, z1, y1, -wOut, wOut, -wOut, wOut, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
                else
                    renderDownFaceCutOut(x0, x1, z0, z1, y0, -wOut, wOut, -wOut, wOut, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
            }

            // Inner cap (joins center)
            float fillIn = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
            float wIn = neighbourHalfWidth(fillIn);
            if (wIn < actualW) {
                if (dir == Direction.UP)
                    renderDownFaceCutOut(x0, x1, z0, z1, y0, -wIn, wIn, -wIn, wIn, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
                else
                    renderUpFaceCutOut(x0, x1, z0, z1, y1, -wIn, wIn, -wIn, wIn, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
            }

        } else {
            // Horizontal connection (NORTH / SOUTH / EAST / WEST)
            boolean centered = shouldRenderCentered(conn.tank.getFluid().getFluid());

            // Axis-aligned bounds
            boolean tankExtended = switch (dir) {
                case WEST  -> tile.tankWest;
                case EAST  -> tile.tankEast;
                case NORTH -> tile.tankNorth;
                case SOUTH -> tile.tankSouth;
                default    -> false;
            };
            float armStart = switch (dir) {
                case WEST, NORTH -> -0.5f - (tankExtended ? 0.125f : 0f);
                default          ->  0.25f - EPS;
            };
            float armEnd = switch (dir) {
                case WEST, NORTH -> -0.25f + EPS;
                default          ->  0.5f + (tankExtended ? 0.125f : 0f);
            };
            // Flow directions for animation
            Direction intoCenter  = dir.getOpposite();  // texture flows this way when fluid enters
            Direction awayCenter  = dir;                // texture flows this way when fluid exits

            float neighborFill = (float) conn.neighborFluidHandler().getFluidInTank(0).getAmount()
                    / conn.neighborFluidHandler().getTankCapacity(0);
            float tileCenterFill = (float) tile.tank.getFluidAmount() / tile.tank.getCapacity();
            float wOut = neighbourHalfWidth(neighborFill);
            float wIn  = neighbourHalfWidth(tileCenterFill);

            if (!centered) {
                float half = 0.25f - EPS;
                float x0, x1, z0, z1;
                float y0 = -0.25f + EPS;
                float y1 = y0 - 2 * EPS + 0.5f * relativeFill;
                float y0OffN = y0, y0OffS = y0, y0OffE = y0, y0OffW = y0;

                if (dir == Direction.WEST || dir == Direction.EAST) {
                    x0 = (dir == Direction.WEST) ? armStart : armEnd - (armEnd - armStart);
                    x1 = (dir == Direction.WEST) ? armEnd   : armEnd;
                    x0 = (dir == Direction.WEST) ? armStart : 0.25f - EPS;
                    x1 = (dir == Direction.WEST) ? -0.25f + EPS : armEnd;
                    z0 = -half; z1 = half;
                    float centerOffset = y0 - 2 * EPS + 0.5f * tileCenterFill;
                    if (dir == Direction.WEST) y0OffE = centerOffset; else y0OffW = centerOffset;
                    if (conn.neighborFluidHandler() instanceof PipeConnection p) {
                        float pFill = (float) p.tank.getFluidAmount() / p.tank.getCapacity();
                        if (dir == Direction.WEST) y0OffW = y0 - 2 * EPS + 0.5f * pFill;
                        else                        y0OffE = y0 - 2 * EPS + 0.5f * pFill;
                    }
                    if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(x0, x1, z0, z1, y0, y1,
                                f.u0(), f.u1(), f.v0() + halfV, f.v1(),
                                color, intoCenter, v, light, overlay, y0OffN, y0OffS, y0OffE, y0OffW);
                    } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(x0, x1, z0, z1, y0, y1,
                                f.u0(), f.u1(), f.v0(), f.v1() - halfV,
                                color, awayCenter, v, light, overlay, y0OffN, y0OffS, y0OffE, y0OffW);
                    } else {
                        renderHorizontalFluidStill(x0, x1, z0, z1, y0, y1, s.u0(), s.u1(), s.v0(), s.v1(),
                                color, v, light, overlay, y0OffN, y0OffS, y0OffE, y0OffW);
                    }
                } else {
                    // NORTH or SOUTH
                    z0 = (dir == Direction.NORTH) ? armStart : 0.25f - EPS;
                    z1 = (dir == Direction.NORTH) ? -0.25f + EPS : armEnd;
                    x0 = -half; x1 = half;
                    float centerOffset = y0 - 2 * EPS + 0.5f * tileCenterFill;
                    if (dir == Direction.NORTH) y0OffS = centerOffset; else y0OffN = centerOffset;
                    if (conn.neighborFluidHandler() instanceof PipeConnection p) {
                        float pFill = (float) p.tank.getFluidAmount() / p.tank.getCapacity();
                        if (dir == Direction.NORTH) y0OffN = y0 - 2 * EPS + 0.5f * pFill;
                        else                         y0OffS = y0 - 2 * EPS + 0.5f * pFill;
                    }
                    if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(x0, x1, z0, z1, y0, y1,
                                f.u0(), f.u1(), f.v0() + halfV, f.v1(),
                                color, intoCenter, v, light, overlay, y0OffN, y0OffS, y0OffE, y0OffW);
                    } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                        renderHorizontalFluidFlowing(x0, x1, z0, z1, y0, y1,
                                f.u0(), f.u1(), f.v0(), f.v1() - halfV,
                                color, awayCenter, v, light, overlay, y0OffN, y0OffS, y0OffE, y0OffW);
                    } else {
                        renderHorizontalFluidStill(x0, x1, z0, z1, y0, y1, s.u0(), s.u1(), s.v0(), s.v1(),
                                color, v, light, overlay, y0OffN, y0OffS, y0OffE, y0OffW);
                    }
                }
            } else {
                // Centered (lighter-than-air) horizontal arm
                float actualW = halfWidth(relativeFill);
                float y0 = -actualW, y1 = actualW;
                float x0, x1, z0, z1;

                if (dir == Direction.WEST || dir == Direction.EAST) {
                    x0 = (dir == Direction.WEST) ? armStart : 0.25f - EPS;
                    x1 = (dir == Direction.WEST) ? -0.25f + EPS : armEnd;
                    z0 = -actualW; z1 = actualW;
                } else {
                    z0 = (dir == Direction.NORTH) ? armStart : 0.25f - EPS;
                    z1 = (dir == Direction.NORTH) ? -0.25f + EPS : armEnd;
                    x0 = -actualW; x1 = actualW;
                }

                if (conn.getsInputFromOutside && !conn.getsInputFromInside) {
                    renderFluidFlowingCentered(x0, x1, z0, z1, y0, y1,
                            f.u0(), f.u1(), f.v0() + halfV, f.v1(),
                            color, intoCenter, v, light, overlay);
                } else if (!conn.getsInputFromOutside && conn.getsInputFromInside) {
                    renderFluidFlowingCentered(x0, x1, z0, z1, y0, y1,
                            f.u0(), f.u1(), f.v0(), f.v1() - halfV,
                            color, awayCenter, v, light, overlay);
                } else {
                    renderHorizontalFluidStillCentered(x0, x1, z0, z1, y0, y1,
                            s.u0(), s.u1(), s.v0(), s.v1(), color, v, light, overlay, dir);
                }

                // Outer and inner cap faces
                if (wOut < actualW) {
                    if (dir == Direction.EAST)  renderEastFaceCutOut(z0, z1, y0, y1, x1, -wOut, wOut, -wOut, wOut, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
                    if (dir == Direction.WEST)  renderWestFaceCutOut(z0, z1, y0, y1, x0, -wOut, wOut, -wOut, wOut, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
                    if (dir == Direction.SOUTH) renderSouthFaceCutOut(x0, x1, y0, y1, z1, -wOut, wOut, -wOut, wOut, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
                    if (dir == Direction.NORTH) renderNorthFaceCutOut(x0, x1, y0, y1, z0, -wOut, wOut, -wOut, wOut, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
                }
                if (wIn < actualW) {
                    if (dir == Direction.EAST)  renderWestFaceCutOut(z0, z1, y0, y1, x0, -wIn, wIn, -wIn, wIn, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
                    if (dir == Direction.WEST)  renderEastFaceCutOut(z0, z1, y0, y1, x1, -wIn, wIn, -wIn, wIn, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
                    if (dir == Direction.SOUTH) renderNorthFaceCutOut(x0, x1, y0, y1, z0, -wIn, wIn, -wIn, wIn, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
                    if (dir == Direction.NORTH) renderSouthFaceCutOut(x0, x1, y0, y1, z1, -wIn, wIn, -wIn, wIn, s.u0(), s.u1(), s.v0(), s.v1(), v, light, color);
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Static mesh data (pump arm OBJ model)
    // ──────────────────────────────────────────────────────────────────────────

    static WavefrontObject PIPE_PUMP;
    static {
        try {
            PIPE_PUMP = new WavefrontObject(
                    ResourceLocation.fromNamespaceAndPath("betterpipes", "models/block/pipe_pump.obj"));
        } catch (ModelFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BlockEntityRenderer entry point
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void render(EntityPipe tile, float partialTick, PoseStack stack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (tile.requiresMeshUpdate || packedLight != tile.lastLight) {
            // Rebuild fluid mesh
            ByteBufferBuilder fluidBuf = new ByteBufferBuilder(TRANSIENT_BUFFER_SIZE);
            BufferBuilder bb = new BufferBuilder(fluidBuf, VertexFormat.Mode.QUADS, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
            RenderPipe.renderFluids(tile, bb, packedLight, 0);
            tile.fluidMesh = bb.build();
            if (tile.fluidMesh != null) {
                tile.vertexBuffer.bind();
                tile.vertexBuffer.upload(tile.fluidMesh);
            }
            fluidBuf.close();

            // Rebuild pump arm mesh only when light changes
            if (tile.lastLight != packedLight) {
                uploadObjGroup(tile.vertexBufferPumpCube,         PIPE_PUMP, "base", packedLight);
                uploadObjGroup(tile.vertexBufferPumpArm, PIPE_PUMP, "arm",  packedLight);
            }

            tile.lastLight = packedLight;
            tile.requiresMeshUpdate = false;
        }

        LEQUAL_DEPTH_TEST.setupRenderState();
        LIGHTMAP.setupRenderState();

        // ── Pump rendering (crankshaft-driven AND automatic) ────────────────
        // The little pump cube + moving inner cube are identical between the two;
        // they only differ by the rotation that drives the oscillation.
        boolean hasCrankPump = tile.crankShaftSide != null && tile.hasAnyExtractionConnections;
        boolean hasAutoPump = tile.pumpUpgradeSide != null;

        if (hasCrankPump || hasAutoPump) {
            RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
            ShaderInstance shader = RenderSystem.getShader();
            RenderSystem.setShaderTexture(0, pumpArmTexture);

            double angle = tile.myMechanicalBlock.currentRotation / 180.0 * Math.PI+ tile.myMechanicalBlock.internalVelocity / TPS * partialTick;
            if (hasCrankPump) {
                if (tile.crankShaftSide == Direction.DOWN) angle -= Math.PI / 2.0;
                renderPumpAssembly(tile, stack, tile.crankShaftSide, angle, shader, packedLight, true);
            }

            if (hasAutoPump) {
                renderPumpAssembly(tile, stack, tile.pumpUpgradeSide, angle, shader, packedLight, false);
            }

            shader.clear();
        }

        // ── Fluid mesh ─────────────────────────────────────────────────────
        if (tile.fluidMesh != null) {
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
            TRANSLUCENT_TRANSPARENCY.setupRenderState();
            // Use the flowing texture atlas location; still texture must reside at the same atlas.
            RenderSystem.setShaderTexture(0, tile.renderData.spriteFLowing.atlasLocation());
            ShaderInstance shader = RenderSystem.getShader();
            Matrix4f m = new Matrix4f(RenderSystem.getModelViewMatrix())
                    .mul(stack.last().pose())
                    .translate(0.5f, 0.5f, 0.5f);
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, m,
                    RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.apply();
            tile.vertexBuffer.bind();
            tile.vertexBuffer.draw();
            shader.clear();

            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
            TRANSLUCENT_TRANSPARENCY.clearRenderState();
        }

        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        VertexBuffer.unbind();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private render utilities
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Sets the shader's default uniforms for the given pose matrix and applies it.
     * Extracted from the three identical blocks in the crankshaft render path.
     */
    private static void applyShader(ShaderInstance shader, Matrix4f pose, VertexFormat.Mode mode) {
        shader.setDefaultUniforms(mode,
                new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose),
                RenderSystem.getProjectionMatrix(),
                Minecraft.getInstance().getWindow());
        Uniform normalMat = shader.getUniform("NormalMat");
        Matrix3f nm = new Matrix3f(pose);
        nm.invert().transpose();
        normalMat.set(nm);
        shader.apply();
    }

    /** Builds and uploads a named OBJ face group to the given vertex buffer. */
    private static void uploadObjGroup(VertexBuffer target, WavefrontObject obj,
                                       String groupName, int packedLight) {
        ByteBufferBuilder buf = new ByteBufferBuilder(1024);
        BufferBuilder bb = new BufferBuilder(buf, VertexFormat.Mode.QUADS, POSITION_COLOR_TEXTURE_NORMAL_LIGHT);
        for (Face face : obj.groupObjects.get(groupName).faces)
            face.addFaceForRender(new PoseStack(), bb, packedLight, 0, 0xffffffff);
        MeshData mesh = bb.build();
        target.bind();
        target.upload(mesh);
        buf.close();
    }

    private static void renderPumpAssembly(EntityPipe tile, PoseStack stack, Direction side,
                                            double angleRad, ShaderInstance shader, int packedLight,
                                            boolean renderArm) {
        // Build base matrix: translate to block centre, then rotate so the
        // assembly faces outward along `side`.
        Matrix4f m1 = new Matrix4f(stack.last().pose()).translate(0.5f, 0.5f, 0.5f);
        float yRot = switch (side) {
            case WEST  ->   0f;
            case EAST  -> 180f;
            case SOUTH ->  90f;
            case NORTH -> 270f;
            default    ->   0f; // UP / DOWN
        };
        m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 1f, 0f, yRot));

        // Downward-facing pumps hang below; tilt them so they extend downward.
        if (side == Direction.DOWN) {
            BlockState blockBelow = tile.getLevel().getBlockState(tile.getBlockPos().below());
            if (blockBelow.getBlock() instanceof BlockCrankShaftBase crankShaftBase) {
                m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, 90f));
                if (blockBelow.getValue(BlockCrankShaftBase.ROTATION_AXIS) == Direction.Axis.X)
                    m1.rotate(new Quaternionf().fromAxisAngleDeg(1f, 0f, 0f, 90f));
            } else {
                m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, 90f));
            }
        }

        // Upward-facing pumps extend along +Y: mirror the DOWN tilt with a -90°
        // rotation about Z (this sends "up" to +Y). An adjacent crankshaft above —
        // a crank pump on the UP face — is aligned like its DOWN counterpart; the
        // self-powered auto pump has no such neighbour and takes the plain tilt.
        if (side == Direction.UP) {
            BlockState blockAbove = tile.getLevel().getBlockState(tile.getBlockPos().above());
            if (blockAbove.getBlock() instanceof BlockCrankShaftBase crankShaftBase) {
                m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, -90f));
                if (blockAbove.getValue(BlockCrankShaftBase.ROTATION_AXIS) == Direction.Axis.X)
                    m1.rotate(new Quaternionf().fromAxisAngleDeg(1f, 0f, 0f, 90f));
            } else {
                m1.rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, -90f));
            }
        }

        // Crank oscillation geometry.
        float crankR = 0.075f;
        double armLength = 0.8;
        float xRotMul = switch (side) {
            case SOUTH, WEST -> -1f;
            default          ->  1f;
        };
        xRotMul *= (side.getAxis() == Direction.Axis.Y) ? -1f : 1f;

        float txX = -1f + (float) Math.sin(angleRad) * crankR * xRotMul;
        float txY = (float) Math.cos(angleRad) * crankR;
        double armAngle = Math.asin(txY / armLength);

        // Connecting arm (the rotating rod). Only drawn when the pump is actually
        // linked to a crankshaft; the self-powered auto pump has nothing to reach,
        // so the arm is skipped to keep it from floating.
        Matrix4f m2;
        if (renderArm) {
            m2 = new Matrix4f(m1)
                    .translate(txX, txY, 0f)
                    .rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, -(float) (armAngle * 180.0 / Math.PI)))
                    .rotate(new Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, 180f));
            applyShader(shader, m2, VertexFormat.Mode.TRIANGLES);
            tile.vertexBufferPumpArm.bind();
            tile.vertexBufferPumpArm.draw();
        }

        // Moving inner pump cube.
        float pumpX = -0.24f + (float) (txX + Math.cos(armAngle) * armLength) * 0.6f;
        m2 = new Matrix4f(m1).translate(pumpX, 0f, 0f).scale(1f, 0.75f, 0.75f);
        applyShader(shader, m2, VertexFormat.Mode.TRIANGLES);
        tile.vertexBufferPumpCube.bind();
        tile.vertexBufferPumpCube.draw();

        // Static outer pump shell.
        m2 = new Matrix4f(m1).translate(-0.3f, 0f, 0f);
        applyShader(shader, m2, VertexFormat.Mode.TRIANGLES);
        tile.vertexBufferPumpCube.bind();
        tile.vertexBufferPumpCube.draw();
    }
}