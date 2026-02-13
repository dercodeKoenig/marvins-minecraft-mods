package advRocketry.Particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class SmokeParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    public SmokeParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                   double x, double y, double z,
                                   double dx, double dy, double dz) {
        return new SmokeParticle(level, x, y, z, dx, dy, dz, this.sprites);
    }
}
