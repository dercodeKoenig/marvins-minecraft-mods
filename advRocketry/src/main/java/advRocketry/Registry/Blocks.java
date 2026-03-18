package advRocketry.Registry;

import advRocketry.Blocks.*;
import advRocketry.Main;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
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

    // rocket part
    public static final Supplier<Block> ROCKET_MOTOR = BLOCKS.register("rocket_motor", () -> new RocketMotor());
    public static final Supplier<Block> FUEL_TANK = BLOCKS.register("fuel_tank", () -> new FuelTank());
    public static final Supplier<Block> GUIDANCE_COMPUTER = BLOCKS.register("guidance_computer", () -> new GuidanceComputer());
    public static final Supplier<Block> CARGO_HOLD = BLOCKS.register("cargo_hold", () -> new CargoHold());
    public static final Supplier<Block> SEAT = BLOCKS.register("seat", () -> new Seat());
    public static final Supplier<Block> DRILL = BLOCKS.register("drill", () -> new Drill());

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
    public static final Supplier<Block> ASTROBODY_DATA_PROCESSOR = BLOCKS.register("astrobody_data_processor", () -> new AstrobodyDataProcessor());
    public static final Supplier<Block> WIRELESS_TRANSCEIVER = BLOCKS.register("wireless_transceiver", () -> new WirelessTransceiver());
    public static final Supplier<Block> FLUID_RELEASE = BLOCKS.register("fluid_release", () -> new FluidRelease());

    // basic blocks
    public static final Supplier<Block> MOON_TURF_DARK = BLOCKS.register("moon_turf_dark", () -> new Block(BlockBehaviour.Properties.of().strength(0.5f).requiresCorrectToolForDrops()));
    public static final Supplier<Block> MOON_TURF = BLOCKS.register("moon_turf", () -> new Block(BlockBehaviour.Properties.of().strength(0.5f).requiresCorrectToolForDrops()));
    public static final Supplier<Block> DRY_ICE = BLOCKS.register("dry_ice", () -> new DryIceBlock(BlockBehaviour.Properties.of().strength(0.1f).requiresCorrectToolForDrops()));

    // satellite / missions
    public static final Supplier<Block> SATELLITE_ASSEMBLER = BLOCKS.register("satellite_assembler", () -> new SatelliteAssembler());
    public static final Supplier<Block> SATELLITE_MONITOR = BLOCKS.register("satellite_monitor", () -> new SatelliteMonitor());
    public static final Supplier<Block> LAUNCH_STATION_SATELLITE_MISSIONS = BLOCKS.register("launch_station_satellite_missions", () -> new LaunchStationSatelliteMissions());
    public static final Supplier<Block> LAUNCH_STATION_ASTEROID_MISSIONS = BLOCKS.register("launch_station_asteroid_missions", () -> new LaunchStationAsteroidMissions());



    public static final DeferredHolder<Block, LiquidBlock> METHANE_BLOCK = BLOCKS.register("methane_block", () -> new CompositionFluidLiquidBlock(
            Fluids.METHANE.get(),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER).noLootTable(),
            GasRegistry.methane
    ));
    public static final DeferredHolder<Block, LiquidBlock> CO2_BLOCK = BLOCKS.register("co2_block", () -> new CompositionFluidLiquidBlock(
            Fluids.CO2.get(),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER).noLootTable(),
            GasRegistry.co2
    ));
    public static final DeferredHolder<Block, LiquidBlock> OXYGEN_BLOCK = BLOCKS.register("oxygen_block", () -> new CompositionFluidLiquidBlock(
            Fluids.OXYGEN.get(),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER).noLootTable(),
            GasRegistry.oxygen
    ));
    public static final DeferredHolder<Block, LiquidBlock> HYDROGEN_BLOCK = BLOCKS.register("hydrogen_block", () -> new CompositionFluidLiquidBlock(
            Fluids.HYDROGEN.get(),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER).noLootTable(),
            GasRegistry.hydrogen
    ));
    public static final DeferredHolder<Block, LiquidBlock> NITROGEN_BLOCK = BLOCKS.register("nitrogen_block", () -> new CompositionFluidLiquidBlock(
            Fluids.NITROGEN.get(),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER).noLootTable(),
            GasRegistry.nitrogen
    ));
    public static final DeferredHolder<Block, LiquidBlock> ROCKET_FUEL_BLOCK = BLOCKS.register("rocket_fuel_block", () -> new LiquidBlock(
            Fluids.ROCKET_FUEL.get(),
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER).noLootTable()
    ));

}
