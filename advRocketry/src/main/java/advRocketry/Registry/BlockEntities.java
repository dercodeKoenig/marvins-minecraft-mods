package advRocketry.Registry;

import advRocketry.BlockEntities.*;
import advRocketry.Main;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Main.MODID);

    public static final Supplier<BlockEntityType<EntityGuidanceComputer>> ENTITY_GUIDANCE_COMPUTER = BLOCK_ENTITIES.register("guidance_computer", () -> BlockEntityType.Builder.of(EntityGuidanceComputer::new, Blocks.GUIDANCE_COMPUTER.get()).build(null));
    public static final Supplier<BlockEntityType<EntityRocketAssembler>> ENTITY_ROCKET_ASSEMBLER = BLOCK_ENTITIES.register("rocket_assembler", () -> BlockEntityType.Builder.of(EntityRocketAssembler::new, Blocks.ROCKET_ASSEMBLER.get()).build(null));
    public static final Supplier<BlockEntityType<EntityFuelingStation>> ENTITY_FUELING_STATION = BLOCK_ENTITIES.register("fueling_station", () -> BlockEntityType.Builder.of(EntityFuelingStation::new, Blocks.FUELING_STATION.get()).build(null));
    public static final Supplier<BlockEntityType<EntityCargoHold>> ENTITY_CARGO_HOLD = BLOCK_ENTITIES.register("cargo_hold", () -> BlockEntityType.Builder.of(EntityCargoHold::new, Blocks.CARGO_HOLD.get()).build(null));
    public static final Supplier<BlockEntityType<EntityLaunchStation>> ENTITY_LAUNCH_STATION = BLOCK_ENTITIES.register("launch_station", () -> BlockEntityType.Builder.of(EntityLaunchStation::new, Blocks.LAUNCH_STATION.get()).build(null));
    public static final Supplier<BlockEntityType<EntityRocketItemLoader>> ENTITY_ROCKET_ITEM_LOADER = BLOCK_ENTITIES.register("rocket_item_loader", () -> BlockEntityType.Builder.of(EntityRocketItemLoader::new, Blocks.ROCKET_ITEM_LOADER.get()).build(null));

    public static final Supplier<BlockEntityType<EntityObservatory>> ENTITY_OBSERVATORY = BLOCK_ENTITIES.register("observatory", () -> BlockEntityType.Builder.of(EntityObservatory::new, Blocks.OBSERVATORY.get()).build(null));
    public static final Supplier<BlockEntityType<EntityOxygenVent>> ENTITY_OXYGEN_VENT = BLOCK_ENTITIES.register("oxygen_vent", () -> BlockEntityType.Builder.of(EntityOxygenVent::new, Blocks.OXYGEN_VENT.get()).build(null));
    public static final Supplier<BlockEntityType<EntityDataStorageBlock>> ENTITY_DATA_STORAGE_BLOCK = BLOCK_ENTITIES.register("data_storage_block", () -> BlockEntityType.Builder.of(EntityDataStorageBlock::new, Blocks.DATA_STORAGE_BLOCK.get()).build(null));
    public static final Supplier<BlockEntityType<EntitySolarPanel>> ENTITY_SOLAR_PANEL = BLOCK_ENTITIES.register("solar_panel", () -> BlockEntityType.Builder.of(EntitySolarPanel::new, Blocks.SOLAR_PANEL.get()).build(null));

    public static final Supplier<BlockEntityType<EntitySpaceStationAssembler>> ENTITY_SPACE_STATION_ASSEMBLER = BLOCK_ENTITIES.register("space_station_assembler", () -> BlockEntityType.Builder.of(EntitySpaceStationAssembler::new, Blocks.SPACE_STATION_ASSEMBLER.get()).build(null));
    public static final Supplier<BlockEntityType<EntityOrientationController>> ENTITY_ORIENTATION_CONTROLLER = BLOCK_ENTITIES.register("orientation_controller", () -> BlockEntityType.Builder.of(EntityOrientationController::new, Blocks.ORIENTATION_CONTROLLER.get()).build(null));
    public static final Supplier<BlockEntityType<EntityStationController>> ENTITY_STATION_CONTROLLER = BLOCK_ENTITIES.register("station_controller", () -> BlockEntityType.Builder.of(EntityStationController::new, Blocks.STATION_CONTROLLER.get()).build(null));
    public static final Supplier<BlockEntityType<EntityWarpController>> ENTITY_WARP_CONTROLLER = BLOCK_ENTITIES.register("warp_controller", () -> BlockEntityType.Builder.of(EntityWarpController::new, Blocks.WARP_CONTROLLER.get()).build(null));

    public static final Supplier<BlockEntityType<EntitySatelliteAssembler>> ENTITY_SATELLITE_ASSEMBLER = BLOCK_ENTITIES.register("satellite_assembler", () -> BlockEntityType.Builder.of(EntitySatelliteAssembler::new, Blocks.SATELLITE_ASSEMBLER.get()).build(null));
    public static final Supplier<BlockEntityType<EntityLaunchStationSatelliteMissions>> ENTITY_LAUNCH_STATION_SATELLITE_MISSIONS = BLOCK_ENTITIES.register("launch_station_satellite_missions", () -> BlockEntityType.Builder.of(EntityLaunchStationSatelliteMissions::new, Blocks.LAUNCH_STATION_SATELLITE_MISSIONS.get()).build(null));
}
