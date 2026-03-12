package advRocketry.Registry;

import advRocketry.CustomCreativeTab;
import advRocketry.Main;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class GeneralRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Main.MODID);
    public static final Supplier<EntityType<EntityRocket>> ENTITY_ROCKET = ENTITIES.register(
            "rocket",
            () -> EntityType.Builder.of(EntityRocket::new, MobCategory.MISC).clientTrackingRange(1000).build(Main.MODID+":rocket")
    );

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Main.MODID);
    public static final Supplier<CreativeModeTab> CUSTOM_CREATIVE_TAB = CREATIVE_TAB.register(Main.MODID, () -> new CustomCreativeTab());

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Main.MODID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST_PARTICLE = PARTICLES.register("dust_particle", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOFT_PARTICLE = PARTICLES.register("soft_particle", () -> new SimpleParticleType(false));

}
