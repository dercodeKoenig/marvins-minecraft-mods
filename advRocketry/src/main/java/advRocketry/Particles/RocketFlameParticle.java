package advRocketry.Particles;

import advRocketry.Dimension.RocketSpaceTravelManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustParticleBase;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

public class RocketFlameParticle extends DustParticleBase<DustParticleOptions> {

    private float rotSpeed;
private float targetSize;

    public RocketFlameParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, vx, vy, vz, new DustParticleOptions(new Vector3f(0, 0, 0), 10), spriteSet);

        boolean isSmoke = random.nextBoolean();

        if(level.dimension().location().equals(RocketSpaceTravelManager.dimId)) {
            if (isSmoke) {
                // no smoke in space, looks strange
                this.remove();
            }
            // thrust is 0 in space but because we fly fast, add velocity
            vx *= 10;
            vy *= 10;
            vz *= 10;
        }

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
            this.lifetime = (int) (200);
        }

        this.targetSize = quadSize;

        this.xd =1* vx + (Math.random() * (double) 2.0F - (double) 1.0F) * (double) 0.1F;
        this.yd = 1*vy + (Math.random() * (double) 2.0F - (double) 1.0F) * (double) 0.1F;
        this.zd = 1*vz + (Math.random() * (double) 2.0F - (double) 1.0F) * (double) 0.1F;

        this.rotSpeed = this.random.nextFloat() / 50f;

        this.roll = this.random.nextFloat();

        tick(); // initial position correction
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
        if(this.lifetime < 20){
            this.quadSize = targetSize * (float)this.lifetime / 20f;
        }else{
            quadSize = targetSize;
        }

        this.oRoll = this.roll;
        this.roll = this.roll + this.rotSpeed;
        this.rotSpeed *= 0.99f;
    }
}

