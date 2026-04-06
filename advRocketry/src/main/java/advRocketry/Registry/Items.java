package advRocketry.Registry;

import advRocketry.Items.*;
import advRocketry.Main;
import advRocketry.SpaceSuit.Boots;
import advRocketry.SpaceSuit.ChestPlate;
import advRocketry.SpaceSuit.Helmet;
import advRocketry.SpaceSuit.Leggings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Items {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Main.MODID);

    public static final Supplier<Item> ITEM_DATA_STORAGE = ITEMS.register("item_data_storage", () -> new ItemDataStorage());
    public static final Supplier<Item> ITEM_SPACE_STATION_CONTAINER = ITEMS.register("space_station_container", () -> new ItemSpaceStationContainer());
    public static final Supplier<Item> ITEM_PLANET_ID_CHIP = ITEMS.register("planet_id_chip", () -> new ItemPlanetIdChip());
    public static final Supplier<Item> ITEM_GALAXY_DATABASE = ITEMS.register("galaxy_database", () -> new ItemGalaxyDatabase());
    public static final Supplier<Item> ITEM_LINKER = ITEMS.register("linker", () -> new ItemLinker());
    public static final Supplier<Item> ITEM_ROCKET_FUEL_BUCKET = ITEMS.register("rocket_fuel_bucket", () -> new BucketItem(Fluids.ROCKET_FUEL.get(), new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    public static final Supplier<Item> ITEM_ASTEROID_ID_CHIP = ITEMS.register("asteroid_id_chip", () -> new ItemAsteroidIdChip());
    public static final Supplier<Item> ITEM_ATM_ANALYZER = ITEMS.register("atm_analyzer", () -> new ItemAtmAnalyzer());

    public static final Supplier<Item> ITEM_OXYGEN_BUCKET = ITEMS.register("oxygen_bucket", () -> new BucketItem(Fluids.OXYGEN.get(), new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    public static final Supplier<Item> ITEM_HYDROGEN_BUCKET = ITEMS.register("hydrogen_bucket", () -> new BucketItem(Fluids.HYDROGEN.get(), new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    public static final Supplier<Item> ITEM_NITROGEN_BUCKET = ITEMS.register("nitrogen_bucket", () -> new BucketItem(Fluids.NITROGEN.get(), new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    public static final Supplier<Item> ITEM_METHANE_BUCKET = ITEMS.register("methane_bucket", () -> new BucketItem(Fluids.METHANE.get(), new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    public static final Supplier<Item> ITEM_CO2_BUCKET = ITEMS.register("co2_bucket", () -> new BucketItem(Fluids.CO2.get(), new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.BUCKET)));

    public static final Supplier<Item> ITEM_LAUNCHPAD = registerBlockItem("launchpad", Blocks.LAUNCHPAD);
    public static final Supplier<Item> ITEM_STRUCTURE_TOWER = registerBlockItem("structure_tower", Blocks.STRUCTURE_TOWER);
    public static final Supplier<Item> ITEM_ROCKET_MOTOR = registerBlockItem("rocket_motor", Blocks.ROCKET_MOTOR);
    public static final Supplier<Item> ITEM_ROCKET_MOTOR_IMPROVED = registerBlockItem("rocket_motor_improved", Blocks.ROCKET_MOTOR_IMPROVED);
    public static final Supplier<Item> ITEM_FUEL_TANK = registerBlockItem("fuel_tank", Blocks.FUEL_TANK);
    public static final Supplier<Item> ITEM_GUIDANCE_COMPUTER = registerBlockItem("guidance_computer", Blocks.GUIDANCE_COMPUTER);
    public static final Supplier<Item> ITEM_SEAT = registerBlockItem("seat", Blocks.SEAT);
    public static final Supplier<Item> ITEM_CARGO_HOLD = registerBlockItem("cargo_hold", Blocks.CARGO_HOLD);
    public static final Supplier<Item> ITEM_DRILL = ITEMS.register("drill", () -> new ItemDrill());
    public static final Supplier<Item> ITEM_GAS_INTAKE = ITEMS.register("gas_intake", () -> new ItemGasIntake());

    public static final Supplier<Item> ITEM_ROCKET_ASSEMBLER = registerBlockItem("rocket_assembler", Blocks.ROCKET_ASSEMBLER);
    public static final Supplier<Item> ITEM_FUELING_STATION = registerBlockItem("fueling_station", Blocks.FUELING_STATION);
    public static final Supplier<Item> ITEM_LAUNCH_STATION = registerBlockItem("launch_station", Blocks.LAUNCH_STATION);
    public static final Supplier<Item> ITEM_ROCKET_ITEM_LOADER = registerBlockItem("rocket_item_loader", Blocks.ROCKET_ITEM_LOADER);
    public static final Supplier<Item> ITEM_ROCKET_FLUID_LOADER = registerBlockItem("rocket_fluid_loader", Blocks.ROCKET_FLUID_LOADER);

    public static final Supplier<Item> ITEM_ASTROBODY_DATA_PROCESSOR = registerBlockItem("astrobody_data_processor", Blocks.ASTROBODY_DATA_PROCESSOR);
    public static final Supplier<Item> ITEM_OBSERVATORY = registerBlockItem("observatory", Blocks.OBSERVATORY);
    public static final Supplier<Item> ITEM_OXYGEN_VENT = registerBlockItem("oxygen_vent", Blocks.OXYGEN_VENT);
    public static final Supplier<Item> ITEM_DATA_STORAGE_BLOCK = registerBlockItem("data_storage_block", Blocks.DATA_STORAGE_BLOCK);
    public static final Supplier<Item> ITEM_SOLAR_PANEL = ITEMS.register("solar_panel", () -> new ItemSolarPanel());
    public static final Supplier<Item> ITEM_WIRELESS_TRANSCEIVER = registerBlockItem("wireless_transceiver", Blocks.WIRELESS_TRANSCEIVER);
    public static final Supplier<Item> ITEM_FLUID_RELEASE = ITEMS.register("fluid_release", () -> new ItemFluidRelease());
    public static final Supplier<Item> ITEM_PRESSURE_TANK = registerBlockItem("pressure_tank", Blocks.PRESSURE_TANK);

    public static final Supplier<Item> ITEM_SPACE_STATION_ASSEMBLER = registerBlockItem("space_station_assembler", Blocks.SPACE_STATION_ASSEMBLER);
    public static final Supplier<Item> ITEM_ORIENTATION_CONTROLLER = registerBlockItem("orientation_controller", Blocks.ORIENTATION_CONTROLLER);
    public static final Supplier<Item> ITEM_STATION_CONTROLLER = registerBlockItem("station_controller", Blocks.STATION_CONTROLLER);
    public static final Supplier<Item> ITEM_WARP_CONTROLLER = registerBlockItem("warp_controller", Blocks.WARP_CONTROLLER);

    public static final Supplier<Item> ITEM_SATELLITE_ASSEMBLER = registerBlockItem("satellite_assembler", Blocks.SATELLITE_ASSEMBLER);
    public static final Supplier<Item> ITEM_SATELLITE_MONITOR = registerBlockItem("satellite_monitor", Blocks.SATELLITE_MONITOR);
    public static final Supplier<Item> ITEM_LAUNCH_STATION_SATELLITE_MISSIONS = registerBlockItem("launch_station_satellite_missions", Blocks.LAUNCH_STATION_SATELLITE_MISSIONS);
    public static final Supplier<Item> ITEM_LAUNCH_STATION_ASTEROID_MISSIONS = registerBlockItem("launch_station_asteroid_missions", Blocks.LAUNCH_STATION_ASTEROID_MISSIONS);
    public static final Supplier<Item> ITEM_LAUNCH_STATION_GAS_MINING_MISSIONS = registerBlockItem("launch_station_gas_mining_missions", Blocks.LAUNCH_STATION_GAS_MINING_MISSIONS);

    public static final Supplier<Item> ITEM_RADIATION_SHIELD = ITEMS.register("radiation_shield", () -> new ItemRadiationShield());
    public static final Supplier<Item> ITEM_LORA_MODULE = ITEMS.register("lora_module", () -> new ItemLoraModule());
    public static final Supplier<Item> ITEM_BATTERY = ITEMS.register("battery", () -> new ItemBattery());
    public static final Supplier<Item> ITEM_SATELLITE = ITEMS.register("satellite", () -> new ItemSatellite());
    public static final Supplier<Item> ITEM_SATELLITE_OPTICAL_TELESCOPE = ITEMS.register("satellite_optical_telescope", () -> new ItemSatelliteOpticalTelescope());
    public static final Supplier<Item> ITEM_SATELLITE_COMPOSITION_SCANNER = ITEMS.register("satellite_composition_scanner", () -> new ItemSatelliteCompositionScanner());
    public static final Supplier<Item> ITEM_SATELLITE_MASS_SCANNER = ITEMS.register("satellite_mass_scanner", () -> new ItemSatelliteMassScanner());
    public static final Supplier<Item> ITEM_SATELLITE_ID_CHIP = ITEMS.register("satellite_id_chip", () -> new ItemSatelliteIdChip());

    public static final Supplier<Item> ITEM_MOON_TURF = registerBlockItem("moon_turf", Blocks.MOON_TURF);
    public static final Supplier<Item> ITEM_MOON_TURF_DARK = registerBlockItem("moon_turf_dark", Blocks.MOON_TURF_DARK);
    public static final Supplier<Item> ITEM_DRY_ICE = registerBlockItem("dry_ice", Blocks.DRY_ICE);

    public static final Supplier<Item> ITEM_SPACE_SUIT_HELMET = ITEMS.register("space_helmet", () -> new Helmet());
    public static final Supplier<Item> ITEM_SPACE_SUIT_CHESTPLATE = ITEMS.register("space_chestplate", () -> new ChestPlate());
    public static final Supplier<Item> ITEM_SPACE_SUIT_LEGGINGS = ITEMS.register("space_leggings", () -> new Leggings());
    public static final Supplier<Item> ITEM_SPACE_SUIT_BOOTS = ITEMS.register("space_boots", () -> new Boots());
    public static final Supplier<Item> ITEM_PORTABLE_PRESSURE_TANK = ITEMS.register("portable_pressure_tank", () -> new ItemPortablePressureTank());


    public static Supplier<Item> registerBlockItem(String name, Supplier<Block> b) {
        return ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
    }
}
