package advRocketry;

import advRocketry.BlockEntities.EntityFuelingStation;
import advRocketry.BlockEntities.EntityGuidanceComputer;
import advRocketry.BlockEntities.EntityObservatory;
import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.Blocks.*;
import advRocketry.Fluid.RocketFuel;
import advRocketry.Items.ItemLinker;
import advRocketry.Particles.RocketFlameParticle;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.joml.Vector3f;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Registry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Main.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Main.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Main.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Main.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Main.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Main.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, Main.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Main.MODID);


    public static final Supplier<CreativeModeTab> CUSTOM_CREATIVE_TAB = CREATIVE_TAB.register(Main.MODID, () -> new CustomCreativeTab());

    public static Supplier<Item> registerBlockItem(String name, Supplier<Block> b) {
        return ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<Fluid> ROCKET_FUEL = FLUIDS.register("rocket_fuel", () -> new RocketFuel());
    public static final Supplier<Item> ROCKET_FUEL_BUCKET = ITEMS.register("rocket_fuel_bucket", () -> new BucketItem(ROCKET_FUEL.get(),new Item.Properties().stacksTo(16).craftRemainder(Items.BUCKET)));
    public static final Supplier<FluidType> ROCKET_FUEL_TYPE = FLUID_TYPES.register("rocket_fuel_type", () -> new FluidType(FluidType.Properties.create()));

    public static final Supplier<Item> ITEM_LINKER = ITEMS.register("linker", () -> new ItemLinker());

    public static final Supplier<Block> LAUNCHPAD = BLOCKS.register("launchpad", () -> new LaunchPad());
    public static final Supplier<Item> ITEM_LAUNCHPAD = registerBlockItem("launchpad", LAUNCHPAD);

    public static final Supplier<Block> STRUCTURE_TOWER = BLOCKS.register("structure_tower", () -> new StructureTower());

    public static final Supplier<Block> ROCKET_MOTOR = BLOCKS.register("rocket_motor", () -> new RocketMotor());

    public static final Supplier<Block> FUEL_TANK = BLOCKS.register("fuel_tank", () -> new FuelTank());

    public static final Supplier<Block> ROCKET_ASSEMBLER = BLOCKS.register("rocket_assembler", () -> new RocketAssembler());
    public static final Supplier<BlockEntityType<EntityRocketAssembler>> ENTITY_ROCKET_ASSEMBLER = BLOCK_ENTITIES.register("rocket_assembler", () -> BlockEntityType.Builder.of(EntityRocketAssembler::new, ROCKET_ASSEMBLER.get()).build(null));

    public static final Supplier<Block> FUELING_STATION = BLOCKS.register("fueling_station", () -> new FuelingStation());
    public static final Supplier<BlockEntityType<EntityFuelingStation>> ENTITY_FUELING_STATION = BLOCK_ENTITIES.register("fueling_station", () -> BlockEntityType.Builder.of(EntityFuelingStation::new, FUELING_STATION.get()).build(null));

    public static final Supplier<Block> GUIDANCE_COMPUTER = BLOCKS.register("guidance_computer", () -> new GuidanceComputer());
    public static final Supplier<BlockEntityType<EntityGuidanceComputer>> ENTITY_GUIDANCE_COMPUTER = BLOCK_ENTITIES.register("guidance_computer", () -> BlockEntityType.Builder.of(EntityGuidanceComputer::new, GUIDANCE_COMPUTER.get()).build(null));

    public static final Supplier<Block> SEAT = BLOCKS.register("seat", () -> new Seat());

    public static final Supplier<Block> OBSERVATORY = BLOCKS.register("observatory", () -> new Observatory());
    public static final Supplier<BlockEntityType<EntityObservatory>> ENTITY_OBSERVATORY = BLOCK_ENTITIES.register("observatory", () -> BlockEntityType.Builder.of(EntityObservatory::new, OBSERVATORY.get()).build(null));

    public static final Supplier<EntityType<EntityRocket>> ENTITY_ROCKET = ENTITIES.register(
            "rocket",
            () -> EntityType.Builder.of(EntityRocket::new, MobCategory.MISC).clientTrackingRange(1000).build(Main.MODID+":rocket")
    );

    public static final Supplier<SimpleParticleType> ROCKET_FLAME = PARTICLES.register("rocketflame",() -> new SimpleParticleType(true));


    static {
        registerBlockItem("structure_tower", STRUCTURE_TOWER);
        registerBlockItem("rocket_assembler", ROCKET_ASSEMBLER);
        registerBlockItem("rocket_motor", ROCKET_MOTOR);
        registerBlockItem("fuel_tank", FUEL_TANK);
        registerBlockItem("guidance_computer", GUIDANCE_COMPUTER);
        registerBlockItem("seat", SEAT);
        registerBlockItem("fueling_station", FUELING_STATION);
        registerBlockItem("observatory", OBSERVATORY);
    }
}
