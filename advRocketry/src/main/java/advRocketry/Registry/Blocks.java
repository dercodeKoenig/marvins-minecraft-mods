package advRocketry.Registry;

import advRocketry.Blocks.*;
import advRocketry.Main;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Blocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Main.MODID);

    // rocket building
    public static final Supplier<Block> LAUNCHPAD = BLOCKS.register("launchpad", () -> new LaunchPad());
    public static final Supplier<Block> STRUCTURE_TOWER = BLOCKS.register("structure_tower", () -> new StructureTower());
    public static final Supplier<Block> ROCKET_ASSEMBLER = BLOCKS.register("rocket_assembler", () -> new RocketAssembler());
    public static final Supplier<Block> FUELING_STATION = BLOCKS.register("fueling_station", () -> new FuelingStation());
    public static final Supplier<Block> ROCKET_ITEM_LOADER = BLOCKS.register("rocket_item_loader", () -> new RocketItemLoader());
    public static final Supplier<Block> LAUNCH_STATION = BLOCKS.register("launch_station", () -> new LaunchStation());
    public static final Supplier<Block> CARGO_HOLD = BLOCKS.register("cargo_hold", () -> new CargoHold());

    // rocket parts
    public static final Supplier<Block> ROCKET_MOTOR = BLOCKS.register("rocket_motor", () -> new RocketMotor());
    public static final Supplier<Block> FUEL_TANK = BLOCKS.register("fuel_tank", () -> new FuelTank());
    public static final Supplier<Block> GUIDANCE_COMPUTER = BLOCKS.register("guidance_computer", () -> new GuidanceComputer());
    public static final Supplier<Block> SEAT = BLOCKS.register("seat", () -> new Seat());

    // station parts
    public static final Supplier<Block> WARP_CONTROLLER = BLOCKS.register("warp_controller", () -> new WarpController());
    public static final Supplier<Block> STATION_CONTROLLER = BLOCKS.register("station_controller", () -> new StationController());
    public static final Supplier<Block> ORIENTATION_CONTROLLER = BLOCKS.register("orientation_controller", () -> new OrientationController());
    public static final Supplier<Block> SPACE_STATION_ASSEMBLER = BLOCKS.register("space_station_assembler", () -> new SpaceStationAssembler());

    // other special blocks
    public static final Supplier<Block> SOLAR_PANEL = BLOCKS.register("solar_panel", () -> new SolarPanel());
    public static final Supplier<Block> DATA_STORAGE_BLOCK = BLOCKS.register("data_storage_block", () -> new DataStorageBlock());
    public static final Supplier<Block> OXYGEN_VENT = BLOCKS.register("oxygen_vent", () -> new OxygenVent());
    public static final Supplier<Block> OBSERVATORY = BLOCKS.register("observatory", () -> new Observatory());

    // basic blocks
    public static final Supplier<Block> MOON_TURF_DARK = BLOCKS.register("moon_turf_dark", () -> new Block(BlockBehaviour.Properties.of().strength(0.5f).requiresCorrectToolForDrops()));
    public static final Supplier<Block> MOON_TURF = BLOCKS.register("moon_turf", () -> new Block(BlockBehaviour.Properties.of().strength(0.5f).requiresCorrectToolForDrops()));

    // satellite / missions
    public static final Supplier<Block> SATELLITE_ASSEMBLER = BLOCKS.register("satellite_assembler", () -> new SatelliteAssembler());
    public static final Supplier<Block> LAUNCH_STATION_SATELLITE_MISSIONS = BLOCKS.register("launch_station_satellite_missions", () -> new LaunchStationSatelliteMissions());


}
