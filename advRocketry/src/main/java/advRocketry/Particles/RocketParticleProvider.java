package advRocketry.Particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class RocketParticleProvider {
    public static SpriteSet spriteSoft;
    public static SpriteSet spriteDust;

    public static class SoftParticleProvider implements ParticleProvider<SimpleParticleType> {
        public SoftParticleProvider(SpriteSet sprites) {
            RocketParticleProvider.spriteSoft = sprites;
        }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new RocketParticle(level, x, y, z, dx, dy, dz);
        }
    }
    public static class DustParticleProvider implements ParticleProvider<SimpleParticleType> {
        public DustParticleProvider(SpriteSet sprites) {
            RocketParticleProvider.spriteDust = sprites;
        }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new RocketParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
