package advRocketry.Particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustParticleBase;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import org.joml.Vector3f;

public class RocketFlameParticle extends DustParticleBase<DustParticleOptions> {

    public RocketFlameParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, vx, vy, vz, new DustParticleOptions(new Vector3f(0, 0, 0), 10*new Vector3f((float) vx,(float)vy,(float)vz).length()), spriteSet);

        boolean isSmoke = random.nextBoolean();

        if (isSmoke) {
            float f = this.random.nextFloat() * 0.5F + 0.2F;
            float SingleColor = randomizeColor(0.7f, f);
            this.rCol = SingleColor;
            this.gCol = SingleColor;
            this.bCol = SingleColor;
        } else {
            Vector3f color = new Vector3f(1.0F, 0.7F, 0.1F);
            float f = this.random.nextFloat() * 0.4F + 0.6F;
            this.rCol = this.randomizeColor(color.x(), f);
            this.gCol = this.randomizeColor(color.y(), f);
            this.bCol = this.randomizeColor(color.z(), f);
        }


        if (!isSmoke) {
            this.friction = 1;
            this.lifetime = 5;
        } else {
            this.lifetime = 200;
        }

        this.hasPhysics = true;


        this.xd = vx + (Math.random() * (double) 2.0F - (double) 1.0F) * (double) 0.1F;
        this.yd = vy + (Math.random() * (double) 2.0F - (double) 1.0F) * (double) 0.1F;
        this.zd = vz + (Math.random() * (double) 2.0F - (double) 1.0F) * (double) 0.1F;


    }

    @Override
    public void tick() {
        super.tick();
        if(super.onGround){
            float f = this.random.nextFloat() * 0.5F;
            yd = -yd * f;
            xd = (this.random.nextFloat()-0.5) * 0.5F;
            zd = (this.random.nextFloat()-0.5) * 0.5F;
        }
    }
}

