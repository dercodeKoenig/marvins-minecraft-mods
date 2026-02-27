package advRocketry.Particles;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class RocketParticle extends TextureSheetParticle implements RocketParticleEngine.ARParticle {

    public RocketParticle(ClientLevel level, double x, double y, double z,
                          double dx, double dy, double dz) {
        this (level,x,y,z,dx,dy,dz,new Vector3f(1,1,1),1f, 1f, 200, false);
    }

    float alphaMultiplier = 1f;
    float size = 1f;
    boolean isGlowing = false;

    public RocketParticle(ClientLevel level, double x, double y, double z,
                          double dx, double dy, double dz, Vector3f color, float alphaMultiplier, float size, int lifetime, boolean isGlowing) {
        super(level, x, y, z, dx, dy, dz);

        this.friction = 0.99F;
        this.gravity = 0f;
        this.lifetime = lifetime;
        this.isGlowing = isGlowing;

        xd = dx;
        yd = dy;
        zd = dz;

        this.setSpriteFromAge(RocketParticleProvider.sprites);

        ResourceLocation key = level.dimension().location();
        RocketParticleEngine.addParticle(key, this);

        this.setColor(color.x,color.y,color.z);
        this.alphaMultiplier = alphaMultiplier;
        this.size = size;
    }

    public boolean isGlowing(){
        return isGlowing;
    }

    @Override
    public ParticleRenderType getRenderType() {
        //return Static.transparentDelayedParticleRenderType;
        return ParticleRenderType.NO_RENDER;
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = (1.0F - ((float) this.age / (float) this.lifetime)) * alphaMultiplier;
        //this.size *= 0.999f;

        if (super.onGround) {
            float f = (float) (Math.random() * 0.5F);
            yd = -yd * f;
            xd = (Math.random() - 0.5) * 0.5F;
            zd = (Math.random() - 0.5) * 0.5F;
        }
        if (this.lifetime < 20) {
            this.quadSize = size * (float) this.lifetime / 20f;
        } else {
            this.quadSize = size;
        }
    }

    int lastLight = 0;
    @Override
    // modified to skip no light errors
    protected int getLightColor(float partialTick) {
        BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
        if(this.level.hasChunkAt(blockpos)){
            lastLight = LevelRenderer.getLightColor(this.level, blockpos);
        }
        return lastLight;
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        return;
    }

    @Override
    public void renderDelayed(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        super.render(buffer, renderInfo, partialTicks);
    }

    @Override
    public Vec3 getPrevPos(){
        return new Vec3(xo, yo, zo);
    }
}
