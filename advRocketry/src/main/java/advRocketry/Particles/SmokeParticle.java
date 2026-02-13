package advRocketry.Particles;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;

public class SmokeParticle extends TextureSheetParticle implements DelayedTransparentParticles.delayedTransparentParticle{
    private final SpriteSet sprites;

    public static HashMap<ResourceLocation, ArrayList<DelayedTransparentParticles.delayedTransparentParticle>> smokeParticles = new HashMap<>();

    protected SmokeParticle(ClientLevel level, double x, double y, double z,
                            double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;

        this.friction = 0.95F;
        this.gravity = -0.05f;
        this.lifetime = 500;

        this.setSpriteFromAge(sprites);

        ResourceLocation key = level.dimension().location();
        smokeParticles.putIfAbsent(key, new ArrayList<>());
        smokeParticles.get(key).add(this);

        this.setColor(1f,0.5f,0.9f);
    }

    @Override
    public ParticleRenderType getRenderType() {
        //return Static.transparentDelayedParticleRenderType;
        return ParticleRenderType.NO_RENDER;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(sprites);
        this.alpha = 1.0F - ((float) this.age / (float) this.lifetime);

        if (super.onGround) {
            float f = this.random.nextFloat() * 0.5F;
            yd = -yd * f;
            xd = (this.random.nextFloat() - 0.5) * 0.5F;
            zd = (this.random.nextFloat() - 0.5) * 0.5F;
        }
        if (this.lifetime < 20) {
            this.quadSize = 1 * (float) this.lifetime / 20f;
        } else {
            quadSize = 1;
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        return;
    }

    @Override
    public void renderDelayed(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        super.render(buffer, renderInfo, partialTicks);
    }

    @Override
    public Vec3 getPrevPos(){
        return new Vec3(xo, yo, zo);
    }
}
