package advRocketry.Registry;

import advRocketry.Main;
import advRocketry.Rocket.EntityRocket;
import advRocketry.SpaceSuit.SpaceSuit;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class GeneralRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Main.MODID);
    public static final Supplier<EntityType<EntityRocket>> ENTITY_ROCKET = ENTITIES.register(
            "rocket",
            () -> EntityType.Builder.of(EntityRocket::new, MobCategory.MISC).clientTrackingRange(1000).build(Main.MODID + ":rocket")
    );

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Main.MODID);
    public static final Supplier<CreativeModeTab> CUSTOM_CREATIVE_TAB = CREATIVE_TAB.register(
            Main.MODID,
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Advanced Rocketry"))
                    .icon(()->new ItemStack(Items.ITEM_LAUNCHPAD.get()))
                    .build()
    );

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Main.MODID);

    public static final Supplier<AttachmentType<CompoundTag>> CUSTOM_CHUNK_DATA = ATTACHMENT_TYPES.register("raw_chunk_data",
            () -> AttachmentType.builder(() -> new CompoundTag())
                    .serialize(CompoundTag.CODEC)
                    .build()
    );

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, Main.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTAINER_DATA =
            COMPONENTS.register("fluid_container_data", () ->
                    DataComponentType.<SimpleFluidContent>builder()
                            .persistent(SimpleFluidContent.CODEC)
                            .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                            .build()
            );

    public static final DeferredRegister<ArmorMaterial>ARMOR_MATERIALS = DeferredRegister.create(BuiltInRegistries.ARMOR_MATERIAL, Main.MODID);

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Main.MODID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCKET_SMOKE =
            PARTICLES.register("rocket_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCKET_CLOUD =
            PARTICLES.register("rocket_cloud", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCKET_FLAME =
            PARTICLES.register("rocket_flame", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCKET_SMOKE_NO_PHYSICS =
            PARTICLES.register("rocket_smoke_no_physics", () -> new SimpleParticleType(false));

    public static final Holder<ArmorMaterial> SPACE_SUIT_MATERIAL = ARMOR_MATERIALS.register(
            "space_suit",
            () -> new ArmorMaterial(
                    SpaceSuit.protection,
                    0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.EMPTY,
                    SpaceSuit.spaceSuitLayers,
                    0.1f,
                    0.1f
            )
    );
}
