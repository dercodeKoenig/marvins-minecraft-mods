package advRocketry.Particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class RocketParticleEngine {

    public static HashMap<ResourceLocation, ArrayList<ARParticle>> particles = new HashMap<>();
    public static HashMap<ResourceLocation, ArrayList<ARParticle>> particlesGlowing = new HashMap<>();

    public interface ARParticle {
        // for delayed render with depth sort
        void renderDelayed(VertexConsumer buffer, Camera renderInfo, float partialTicks);

        // to interpolate position for depth sort
        Vec3 getPrevPos();

        // these methods are in Particle class defined
        Vec3 getPos();

        boolean isAlive();

        AABB getRenderBoundingBox(float t);

        void tick();

        boolean isGlowing();
    }

    public static void addParticle(ResourceLocation key, ARParticle particle) {
        particles.putIfAbsent(key, new ArrayList<>());
        particlesGlowing.putIfAbsent(key, new ArrayList<>());
        if (particle.isGlowing())
            particlesGlowing.get(key).add(particle);
        else
            particles.get(key).add(particle);
    }

    public static void tick() {
        for (ResourceLocation key : particles.keySet()) {
            particles.get(key).removeIf((p) -> !p.isAlive());
            for (ARParticle p : particles.get(key)) {
                p.tick();
            }
        }

        for (ResourceLocation key : particlesGlowing.keySet()) {
            particlesGlowing.get(key).removeIf((p) -> !p.isAlive());
            for (ARParticle p : particlesGlowing.get(key)) {
                p.tick();
            }
        }
    }

    public static double getPlanarDepth(ARParticle p, Vec3 camPos, Vector3f lookVec, float partialTicks) {
        // Interpolate position exactly like SingleQuadParticle does
        double x = Mth.lerp(partialTicks, p.getPrevPos().x, p.getPos().x) - camPos.x;
        double y = Mth.lerp(partialTicks, p.getPrevPos().y, p.getPos().y) - camPos.y;
        double z = Mth.lerp(partialTicks, p.getPrevPos().z, p.getPos().z) - camPos.z;

        // Dot product: project the relative vector onto the look vector
        return x * lookVec.x() + y * lookVec.y() + z * lookVec.z();
    }

    public static void renderParticles(List<ARParticle> particles, Frustum frustum, Camera camera, float partialTicks, boolean depthSort) {

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);

        if (particles != null) {
            // dont delete from original list when culling
            particles = new ArrayList<>(particles);
            // cull before expensive sort
            particles.removeIf((p) -> !p.isAlive() || !frustum.isVisible(p.getRenderBoundingBox(partialTicks)));
            if (depthSort) {
                // 1. Get camera info
                Vec3 camPos = camera.getPosition();
                Vector3f lookVec = camera.getLookVector();

                // 2. Sort by Planar Depth (Dot Product)
                // this is required bc particles are orthogonal to the look vector and not position vector
                particles.sort((p1, p2) -> {
                    // We must use interpolated positions to match the frame's render
                    double d1 = getPlanarDepth(p1, camPos, lookVec, partialTicks);
                    double d2 = getPlanarDepth(p2, camPos, lookVec, partialTicks);

                    // Sort Farthest First (Descending)
                    return Double.compare(d2, d1);
                });
            }
            for (ARParticle p : particles) {
                p.renderDelayed(buffer, camera, partialTicks);
            }
        }

        // upload & draw
        MeshData meshdata = buffer.build();
        if (meshdata != null) {
            BufferUploader.drawWithShader(meshdata);
        }

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

    }

    public static void renderAll(Frustum frustum, Camera renderInfo, float partialTicks) {

        // setup render state
        boolean is_fabulous = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FABULOUS;
        RenderSystem.setShader(GameRenderer::getParticleShader);
        ShaderInstance shader = RenderSystem.getShader();
        Uniform alphaCut = shader.getUniform("alphaCut");  // uniform added in shader overwrite
        if(alphaCut != null)
            shader.getUniform("alphaCut").set(0f);
        PARTICLES_TARGET.setupRenderState();
        TRANSLUCENT_TRANSPARENCY.setupRenderState();
        LIGHTMAP.setupRenderState();
        LEQUAL_DEPTH_TEST.setupRenderState();
        if (is_fabulous) {
            // this requires depth write and manual sort
            RenderSystem.depthMask(true);
        } else {
            RenderSystem.depthMask(false);
        }
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);


        ResourceLocation key = Minecraft.getInstance().level.dimension().location();

        if(!is_fabulous) {
            // render translucent particles
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            renderParticles(particles.get(key), frustum, renderInfo, partialTicks, is_fabulous);

            // render glowing particles
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            renderParticles(particlesGlowing.get(key), frustum, renderInfo, partialTicks, is_fabulous);
        }else{
            // fabulous makes so much problems... we will use only normal blending
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            ArrayList<ARParticle> join = new ArrayList<>();
            if(particles.get(key) != null)
                join.addAll(particles.get(key));
            if(particlesGlowing.get(key) != null)
                join.addAll(particlesGlowing.get(key));
            renderParticles(join, frustum, renderInfo, partialTicks, is_fabulous);
        }

        // clear render state
        if(alphaCut != null)
            alphaCut.set(0.1f); // reset uniform back to default value
        RenderSystem.depthMask(true);
        TRANSLUCENT_TRANSPARENCY.clearRenderState();
        PARTICLES_TARGET.clearRenderState();
        LIGHTMAP.clearRenderState();
        LEQUAL_DEPTH_TEST.clearRenderState();
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
    }
}
