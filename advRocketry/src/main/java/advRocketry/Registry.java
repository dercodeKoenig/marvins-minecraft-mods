package advRocketry;

import advRocketry.BlockEntities.*;
import advRocketry.Blocks.*;
import advRocketry.Fluid.RocketFuel;
import advRocketry.Items.ItemGalaxyStorageDisk;
import advRocketry.Items.ItemLinker;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Items.ItemSpaceStationContainer;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

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
    public static final Supplier<Item> ITEM_GALAXY_STORAGE_DISK = ITEMS.register("galaxy_storage_disk", () -> new ItemGalaxyStorageDisk());
    public static final Supplier<Item> ITEM_PLANET_ID_CHIP = ITEMS.register("planet_id_chip", () -> new ItemPlanetIdChip());
    public static final Supplier<Item> ITEM_SPACE_STATION_CONTAINER = ITEMS.register("space_station_container", () -> new ItemSpaceStationContainer());

    public static final Supplier<Block> LAUNCHPAD = BLOCKS.register("launchpad", () -> new LaunchPad());
    public static final Supplier<Item> ITEM_LAUNCHPAD = registerBlockItem("launchpad", LAUNCHPAD);

    public static final Supplier<Block> STRUCTURE_TOWER = BLOCKS.register("structure_tower", () -> new StructureTower());



    public static final Supplier<Block> ROCKET_MOTOR = BLOCKS.register("rocket_motor", () -> new RocketMotor());

    public static final Supplier<Block> FUEL_TANK = BLOCKS.register("fuel_tank", () -> new FuelTank());

    public static final Supplier<Block> GUIDANCE_COMPUTER = BLOCKS.register("guidance_computer", () -> new GuidanceComputer());
    public static final Supplier<BlockEntityType<EntityGuidanceComputer>> ENTITY_GUIDANCE_COMPUTER = BLOCK_ENTITIES.register("guidance_computer", () -> BlockEntityType.Builder.of(EntityGuidanceComputer::new, GUIDANCE_COMPUTER.get()).build(null));

    public static final Supplier<Block> SEAT = BLOCKS.register("seat", () -> new Seat());

    public static final Supplier<Block> CARGO_HOLD = BLOCKS.register("cargo_hold", () -> new CargoHold());
    public static final Supplier<BlockEntityType<EntityCargoHold>> ENTITY_CARGO_HOLD = BLOCK_ENTITIES.register("cargo_hold", () -> BlockEntityType.Builder.of(EntityCargoHold::new, CARGO_HOLD.get()).build(null));



    public static final Supplier<Block> ROCKET_ASSEMBLER = BLOCKS.register("rocket_assembler", () -> new RocketAssembler());
    public static final Supplier<BlockEntityType<EntityRocketAssembler>> ENTITY_ROCKET_ASSEMBLER = BLOCK_ENTITIES.register("rocket_assembler", () -> BlockEntityType.Builder.of(EntityRocketAssembler::new, ROCKET_ASSEMBLER.get()).build(null));

    public static final Supplier<Block> FUELING_STATION = BLOCKS.register("fueling_station", () -> new FuelingStation());
    public static final Supplier<BlockEntityType<EntityFuelingStation>> ENTITY_FUELING_STATION = BLOCK_ENTITIES.register("fueling_station", () -> BlockEntityType.Builder.of(EntityFuelingStation::new, FUELING_STATION.get()).build(null));

    public static final Supplier<Block> LAUNCH_STATION = BLOCKS.register("launch_station", () -> new LaunchStation());
    public static final Supplier<BlockEntityType<EntityLaunchStation>> ENTITY_LAUNCH_STATION = BLOCK_ENTITIES.register("launch_station", () -> BlockEntityType.Builder.of(EntityLaunchStation::new, LAUNCH_STATION.get()).build(null));

    public static final Supplier<Block> ROCKET_ITEM_LOADER = BLOCKS.register("rocket_item_loader", () -> new RocketItemLoader());
    public static final Supplier<BlockEntityType<EntityRocketItemLoader>> ENTITY_ROCKET_ITEM_LOADER = BLOCK_ENTITIES.register("rocket_item_loader", () -> BlockEntityType.Builder.of(EntityRocketItemLoader::new, ROCKET_ITEM_LOADER.get()).build(null));



    public static final Supplier<Block> OBSERVATORY = BLOCKS.register("observatory", () -> new Observatory());
    public static final Supplier<BlockEntityType<EntityObservatory>> ENTITY_OBSERVATORY = BLOCK_ENTITIES.register("observatory", () -> BlockEntityType.Builder.of(EntityObservatory::new, OBSERVATORY.get()).build(null));

    public static final Supplier<Block> OXYGEN_VENT = BLOCKS.register("oxygen_vent", () -> new OxygenVent());
    public static final Supplier<BlockEntityType<EntityOxygenVent>> ENTITY_OXYGEN_VENT = BLOCK_ENTITIES.register("oxygen_vent", () -> BlockEntityType.Builder.of(EntityOxygenVent::new, OXYGEN_VENT.get()).build(null));



    public static final Supplier<Block> SPACE_STATION_ASSEMBLER = BLOCKS.register("space_station_assembler", () -> new SpaceStationAssembler());
    public static final Supplier<BlockEntityType<EntitySpaceStationAssembler>> ENTITY_SPACE_STATION_ASSEMBLER = BLOCK_ENTITIES.register("space_station_assembler", () -> BlockEntityType.Builder.of(EntitySpaceStationAssembler::new, SPACE_STATION_ASSEMBLER.get()).build(null));

    public static final Supplier<Block> ORIENTATION_CONTROLLER = BLOCKS.register("orientation_controller", () -> new OrientationController());
    public static final Supplier<BlockEntityType<EntityOrientationController>> ENTITY_ORIENTATION_CONTROLLER = BLOCK_ENTITIES.register("orientation_controller", () -> BlockEntityType.Builder.of(EntityOrientationController::new, ORIENTATION_CONTROLLER.get()).build(null));

    public static final Supplier<Block> STATION_CONTROLLER = BLOCKS.register("station_controller", () -> new StationController());
    public static final Supplier<BlockEntityType<EntityStationController>> ENTITY_STATION_CONTROLLER = BLOCK_ENTITIES.register("station_controller", () -> BlockEntityType.Builder.of(EntityStationController::new, STATION_CONTROLLER.get()).build(null));

    public static final Supplier<Block> WARP_CONTROLLER = BLOCKS.register("warp_controller", () -> new WarpController());
    public static final Supplier<BlockEntityType<EntityWarpController>> ENTITY_WARP_CONTROLLER = BLOCK_ENTITIES.register("warp_controller", () -> BlockEntityType.Builder.of(EntityWarpController::new, WARP_CONTROLLER.get()).build(null));



    public static final Supplier<Block> MOON_TURF = BLOCKS.register("moon_turf", () -> new Block(BlockBehaviour.Properties.of().strength(0.5f).requiresCorrectToolForDrops()));
    public static final Supplier<Block> MOON_TURF_DARK = BLOCKS.register("moon_turf_dark", () -> new Block(BlockBehaviour.Properties.of().strength(0.5f).requiresCorrectToolForDrops()));



    public static final Supplier<EntityType<EntityRocket>> ENTITY_ROCKET = ENTITIES.register(
            "rocket",
            () -> EntityType.Builder.of(EntityRocket::new, MobCategory.MISC).clientTrackingRange(1000).build(Main.MODID+":rocket")
    );

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOFT_PARTICLE = PARTICLES.register("soft_particle", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST_PARTICLE = PARTICLES.register("dust_particle", () -> new SimpleParticleType(false));


    static {
        registerBlockItem("structure_tower", STRUCTURE_TOWER);
        registerBlockItem("rocket_motor", ROCKET_MOTOR);
        registerBlockItem("fuel_tank", FUEL_TANK);
        registerBlockItem("guidance_computer", GUIDANCE_COMPUTER);
        registerBlockItem("seat", SEAT);
        registerBlockItem("cargo_hold", CARGO_HOLD);
        registerBlockItem("rocket_assembler", ROCKET_ASSEMBLER);
        registerBlockItem("fueling_station", FUELING_STATION);
        registerBlockItem("launch_station", LAUNCH_STATION);
        registerBlockItem("rocket_item_loader", ROCKET_ITEM_LOADER);
        registerBlockItem("observatory", OBSERVATORY);
        registerBlockItem("oxygen_vent", OXYGEN_VENT);
        registerBlockItem("space_station_assembler", SPACE_STATION_ASSEMBLER);
        registerBlockItem("orientation_controller", ORIENTATION_CONTROLLER);
        registerBlockItem("station_controller", STATION_CONTROLLER);
        registerBlockItem("warp_controller", WARP_CONTROLLER);

        registerBlockItem("moon_turf", MOON_TURF);
        registerBlockItem("moon_turf_dark", MOON_TURF_DARK);
    }
}
