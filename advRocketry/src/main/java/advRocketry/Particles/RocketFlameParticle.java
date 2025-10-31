package advRocketry.Particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustParticleBase;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Random;
import org.joml.Vector3f;

public class RocketFlameParticle extends DustParticleBase<DustParticleOptions> {

    private float rotSpeed;

    public RocketFlameParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, vx, vy, vz, new DustParticleOptions(new Vector3f(0, 0, 0), 10 * new Vector3f((float) vx, (float) vy, (float) vz).length() * (new Random().nextFloat()*0.2f+0.8f)), spriteSet);

        boolean isSmoke = random.nextBoolean();

        if (isSmoke) {
            float f = this.random.nextFloat() * 0.5F + 0.2F;
            float SingleColor = randomizeColor(0.7f, f);
            this.rCol = SingleColor;
            this.gCol = SingleColor;
            this.bCol = SingleColor;
        } else {
            Vector3f color = new Vector3f(1F, 0.7F, 0.3F);
            float f = this.random.nextFloat() * 0.2F + 0.8F;
            this.rCol = this.randomizeColor(color.x(), f);
            this.gCol = this.randomizeColor(color.y(), f);
            this.bCol = this.randomizeColor(color.z(), f);
        }

        this.hasPhysics = true;

        if (!isSmoke) {
            this.lifetime = 20;
        } else {
            this.lifetime = (int) (200 * new Vector3f((float) vx, (float) vy, (float) vz).length());
        }


        this.xd = vx + (Math.random() * (double) 2.0F - (double) 1.0F) * (double) 0.1F;
        this.yd = vy + (Math.random() * (double) 2.0F - (double) 1.0F) * (double) 0.1F;
        this.zd = vz + (Math.random() * (double) 2.0F - (double) 1.0F) * (double) 0.1F;

        this.rotSpeed = this.random.nextFloat() / 50f;

        this.roll = this.random.nextFloat();
    }

    @Override
    public void tick() {
        super.tick();
        if (super.onGround) {
            float f = this.random.nextFloat() * 0.5F;
            yd = -yd * f;
            xd = (this.random.nextFloat() - 0.5) * 0.5F;
            zd = (this.random.nextFloat() - 0.5) * 0.5F;
        }

        this.oRoll = this.roll;
        this.roll = this.roll + this.rotSpeed;
        this.rotSpeed *= 0.99f;
    }
}

