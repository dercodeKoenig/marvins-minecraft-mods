package advRocketry.Render.Particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class RocketCloudParticle extends TextureSheetParticle {

    private static float GROWTH_TICKS = 80;

    private final SpriteSet sprites;

    protected RocketCloudParticle(ClientLevel level, double x, double y, double z,
                                  double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.friction = 0.98F;
        this.sprites = sprites;
        this.xd *= 0.1F;
        this.yd *= 0.1F;
        this.zd *= 0.1F;
        this.xd += dx;
        this.yd += dy;
        this.zd += dz;
        float f1 = 1.0F - (float)(Math.random() * (double)0.3F);
        this.rCol = f1;
        this.gCol = f1;
        this.bCol = f1;
        this.quadSize *= 1.875F;
        this.lifetime = (int)(800 * (Math.random() * 0.9 + 0.1));
        this.hasPhysics = true;
        this.gravity = 0f;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
        }
        if (this.onGround) {
            scatterOnGround(0.3, 0, 0.1);
            this.lifetime /= 2;
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / GROWTH_TICKS, 0.0f, 1.0f);
        return this.quadSize * progress;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Scatter the particle sideways with a slight upward drift on ground impact. */
    private void scatterOnGround(double hSpeed, double upMin, double upMax) {
        RandomSource r = this.random;
        this.xd = (r.nextDouble() - 0.5) * hSpeed;
        this.zd = (r.nextDouble() - 0.5) * hSpeed;
        this.yd = upMin + r.nextDouble() * (upMax - upMin);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new RocketCloudParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
