package advRocketry.Render.Particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.LargeSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A large smoke particle with a longer lifetime (3× vanilla) but <em>no physics</em> —
 * it does not collide with blocks or detect ground contact.  This means it floats
 * through walls and terrain and simply ages out, which is useful for entities where
 * a clean, non-interactive smoke trail is desired (e.g. fluid releases, laser drills).
 */
public class RocketSmokeNoPhysicsParticle extends LargeSmokeParticle {

    public RocketSmokeNoPhysicsParticle(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz, sprites);
        this.lifetime = this.lifetime * 3;
        this.hasPhysics = false;
    }

    // No tick() override — inherits LargeSmokeParticle's vanilla tick (no scatter).

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new RocketSmokeNoPhysicsParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
