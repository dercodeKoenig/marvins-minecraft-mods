package advRocketry.Render.Particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.LargeSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class RocketSmokeParticle extends LargeSmokeParticle {

    public RocketSmokeParticle(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz, sprites);
        this.lifetime = this.lifetime * 3;
        this.friction = 0.98f;
        this.gravity = -0.05f;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.onGround) {
            scatterOnGround(0.3, 0, 0.1);
            this.lifetime /= 2;
        }
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
            return new RocketSmokeParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
