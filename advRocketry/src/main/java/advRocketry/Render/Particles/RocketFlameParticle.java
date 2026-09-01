package advRocketry.Render.Particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.RisingParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class RocketFlameParticle extends RisingParticle {

    protected RocketFlameParticle(ClientLevel level, double x, double y, double z,
                                  double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.quadSize *= 1.5F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = (this.age + partialTick) / this.lifetime;
        return this.quadSize * (1.0f - progress * progress * 0.5f);
    }

    @Override
    protected int getLightColor(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0f, 1.0f);
        int base = super.getLightColor(partialTick);
        int blockLight = base & 0xFF;
        int skyLight = (base >> 16) & 0xFF;
        blockLight = Math.min(240, blockLight + (int)(progress * 15.0f * 16.0f));
        return blockLight | (skyLight << 16);
    }

    @Override
    public void tick() {
        // Capture velocity before super.tick() applies movement & friction, so we
        // can preserve the incoming speed for the ground scatter.
        double preXd = this.xd, preYd = this.yd, preZd = this.zd;
        super.tick();
        if (this.onGround) {
            scatterOnGround(preXd, preYd, preZd);
        }
    }

    /** Scatter in a random horizontal direction, preserving half the incoming speed. */
    private void scatterOnGround(double preXd, double preYd, double preZd) {
        double speed = Math.sqrt(preXd * preXd + preYd * preYd + preZd * preZd);
        // A flame that barely moved forward still gets a small scatter impulse
        speed = Math.max(speed * 0.25, 0.02);

        RandomSource r = this.random;
        double angle = r.nextDouble() * Math.PI * 2.0;
        this.xd = Math.cos(angle) * speed;
        this.zd = Math.sin(angle) * speed;
        this.yd = 0.03 + r.nextDouble() * 0.05; // slight upward bounce
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
            RocketFlameParticle particle = new RocketFlameParticle(level, x, y, z, dx, dy, dz);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
