package advRocketry.Particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class RocketFlameParticleProvider implements ParticleProvider<SimpleParticleType> {

    private final SpriteSet sprites;

    public RocketFlameParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }


    @Override
    public @Nullable Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        RocketFlameParticle p = new RocketFlameParticle(level, x, y, z, vx, vy, vz, this.sprites);
        return p;
    }
}

