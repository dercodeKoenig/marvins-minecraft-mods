package advRocketry.Render.Particles;

import advRocketry.ClientConfig;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class RocketParticle extends TextureSheetParticle implements RocketParticleEngine.ARParticle {

    public RocketParticle(ClientLevel level, double x, double y, double z,
                          double dx, double dy, double dz) {
        this (level,x,y,z,dx,dy,dz,new Vector3f(0.5f,0.5f,0.5f),0.2f, 1f, 200, false);
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

        this.setColor(color.x, color.y, color.z);
        this.alphaMultiplier = alphaMultiplier;
        this.size = size;

        if (ClientConfig.INSTANCE.use_Transparent_Particle_Engine) {
            ResourceLocation key = level.dimension().location();
            this.setSpriteFromAge(RocketParticleProvider.spriteSoft);
            RocketParticleEngine.addParticle(key, this);
        } else {
            this.alpha = 1f; // minecraft will cut low alpha, so the soft particles will not work
            this.size /= 10; // since particles are now full alpha, make them smaller
            this.setSpriteFromAge(RocketParticleProvider.spriteDust);
            Minecraft.getInstance().particleEngine.add(this);
        }
    }

    public boolean isGlowing(){
        return isGlowing;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if(ClientConfig.INSTANCE.use_Transparent_Particle_Engine) {
            // transparent particles
            this.alpha = (1.0F - ((float) this.age / (float) this.lifetime)) * alphaMultiplier;
        }
        else {
            // dust particles
            this.setSpriteFromAge(RocketParticleProvider.spriteDust);
        }

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

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        super.render(buffer, renderInfo, partialTicks);
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
