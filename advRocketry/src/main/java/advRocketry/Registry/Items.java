package advRocketry.Registry;

import advRocketry.Items.*;
import advRocketry.Main;
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
    public static final Supplier<Item> ITEM_LAUNCHPAD = registerBlockItem("launchpad", Blocks.LAUNCHPAD);
    public static final Supplier<Item> ITEM_ROCKET_FUEL_BUCKET = ITEMS.register("rocket_fuel_bucket", () -> new BucketItem(GeneralRegistry.ROCKET_FUEL.get(),new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.BUCKET)));

    public static final Supplier<Item> ITEM_STRUCTURE_TOWER = registerBlockItem("structure_tower", Blocks.STRUCTURE_TOWER);
    public static final Supplier<Item> ITEM_ROCKET_MOTOR = registerBlockItem("rocket_motor", Blocks.ROCKET_MOTOR);
    public static final Supplier<Item> ITEM_FUEL_TANK = registerBlockItem("fuel_tank", Blocks.FUEL_TANK);
    public static final Supplier<Item> ITEM_GUIDANCE_COMPUTER = registerBlockItem("guidance_computer", Blocks.GUIDANCE_COMPUTER);
    public static final Supplier<Item> ITEM_SEAT = registerBlockItem("seat", Blocks.SEAT);
    public static final Supplier<Item> ITEM_CARGO_HOLD = registerBlockItem("cargo_hold", Blocks.CARGO_HOLD);

    public static final Supplier<Item> ITEM_ROCKET_ASSEMBLER = registerBlockItem("rocket_assembler", Blocks.ROCKET_ASSEMBLER);
    public static final Supplier<Item> ITEM_FUELING_STATION = registerBlockItem("fueling_station", Blocks.FUELING_STATION);
    public static final Supplier<Item> ITEM_LAUNCH_STATION = registerBlockItem("launch_station", Blocks.LAUNCH_STATION);
    public static final Supplier<Item> ITEM_ROCKET_ITEM_LOADER = registerBlockItem("rocket_item_loader", Blocks.ROCKET_ITEM_LOADER);

    public static final Supplier<Item> ITEM_OBSERVATORY = registerBlockItem("observatory", Blocks.OBSERVATORY);
    public static final Supplier<Item> ITEM_OXYGEN_VENT = registerBlockItem("oxygen_vent", Blocks.OXYGEN_VENT);
    public static final Supplier<Item> ITEM_DATA_STORAGE_BLOCK = registerBlockItem("data_storage_block", Blocks.DATA_STORAGE_BLOCK);
    public static final Supplier<Item> ITEM_SOLAR_PANEL = registerBlockItem("solar_panel", Blocks.SOLAR_PANEL);

    public static final Supplier<Item> ITEM_SPACE_STATION_ASSEMBLER = registerBlockItem("space_station_assembler", Blocks.SPACE_STATION_ASSEMBLER);
    public static final Supplier<Item> ITEM_ORIENTATION_CONTROLLER = registerBlockItem("orientation_controller", Blocks.ORIENTATION_CONTROLLER);
    public static final Supplier<Item> ITEM_STATION_CONTROLLER = registerBlockItem("station_controller", Blocks.STATION_CONTROLLER);
    public static final Supplier<Item> ITEM_WARP_CONTROLLER = registerBlockItem("warp_controller", Blocks.WARP_CONTROLLER);

    public static final Supplier<Item> ITEM_MOON_TURF = registerBlockItem("moon_turf", Blocks.MOON_TURF);
    public static final Supplier<Item> ITEM_MOON_TURF_DARK = registerBlockItem("moon_turf_dark", Blocks.MOON_TURF_DARK);

    public static Supplier<Item> registerBlockItem(String name, Supplier<Block> b) {
        return ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
    }
}
