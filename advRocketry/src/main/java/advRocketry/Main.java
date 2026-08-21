package advRocketry;

import ARLib.network.SimpleNetworkPacket;
import advRocketry.BlockEntities.EntityAstrobodyDataProcessor;
import advRocketry.BlockEntities.EntityObservatory;
import advRocketry.Dimension.*;
import advRocketry.Items.ItemLinker;
import advRocketry.Items.ItemPortablePressureTank;
import advRocketry.Registry.*;
import advRocketry.Render.*;
import advRocketry.SpaceSuit.ChestPlate;
import advRocketry.Worldgen.BiomeConfig;

import advRocketry.Worldgen.Presets.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.File;
import java.nio.file.Path;

@Mod(Main.MODID)
public class Main {

    public static final String MODID = "adv_rocketry";

    public static Path worldPath;
    public static Path myConfigDir;

    public Main(IEventBus modEventBus, ModContainer modContaine) {
        // game events
        if (FMLLoader.getDist().isClient()) {
            NeoForge.EVENT_BUS.addListener(WorldEvents::onRenderStage);
            NeoForge.EVENT_BUS.addListener(Fog::renderFogEvent);
            NeoForge.EVENT_BUS.addListener(Fog::computeFogColorEvent);
            NeoForge.EVENT_BUS.addListener(WorldEvents::onClientTick);
            NeoForge.EVENT_BUS.addListener(WorldEvents::CalculateDetachedCameraDistance);
        }
        NeoForge.EVENT_BUS.addListener(WorldEvents::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(WorldEvents::onDimensionChange);
        NeoForge.EVENT_BUS.addListener(WorldEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(WorldEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(WorldEvents::onServerStop);
        NeoForge.EVENT_BUS.addListener(WorldEvents::onClientLogout);
        NeoForge.EVENT_BUS.addListener(PlanetEvents::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(ItemLinker::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(WorldEvents::onLivingFallEvent);
        NeoForge.EVENT_BUS.addListener(WorldEvents::onSourceCreate);
        NeoForge.EVENT_BUS.addListener(WorldEvents::onMobSpawn);

        // mod loading
        if (FMLLoader.getDist().isClient()) {
            modEventBus.addListener(ClientSetup::registerShaders);
            modEventBus.addListener(ClientSetup::onClientSetup);
            modEventBus.addListener(ClientSetup::registerEntityRenderers);
            modEventBus.addListener(ClientSetup::registerParticles);
            modEventBus.addListener(ClientSetup::registerClientExtensions);
            modEventBus.addListener(ClientSetup::addArmorLayers);
        }
        modEventBus.addListener(Main::addCreative);
        modEventBus.addListener(Main::registerCapabilities);
        modEventBus.addListener(Main::loadComplete);
        modEventBus.addListener(Main::registerTickets);
        NeoForge.EVENT_BUS.addListener(Main::registerCommands); // uses the other bus, but for me it belongs to mod loading and not game events

        Blocks.BLOCKS.register(modEventBus);
        Items.ITEMS.register(modEventBus);
        BlockEntities.BLOCK_ENTITIES.register(modEventBus);
        Features.FEATURES.register(modEventBus);
        GeneralRegistry.CREATIVE_TAB.register(modEventBus);
        GeneralRegistry.ENTITIES.register(modEventBus);
        GeneralRegistry.PARTICLES.register(modEventBus);
        Fluids.FLUIDS.register(modEventBus);
        Fluids.FLUID_TYPES.register(modEventBus);
        GeneralRegistry.ATTACHMENT_TYPES.register(modEventBus);
        GeneralRegistry.ARMOR_MATERIALS.register(modEventBus);
        GeneralRegistry.COMPONENTS.register(modEventBus);

        // setup config directory
        Path configDir = FMLPaths.CONFIGDIR.get();
        myConfigDir = Path.of(String.valueOf(configDir), Main.MODID);
        File myConfigDirFile = new File(String.valueOf(myConfigDir));
        if (!myConfigDirFile.exists()) {
            myConfigDirFile.mkdirs();
        }

        // register network packets
        SimpleNetworkPacket.registerReceiver(DimensionManager.packetDimensionPropertiesSync, new DimensionManager.SyncDimensionProperties());
        SimpleNetworkPacket.registerReceiver(DimensionManager.packetDimensionListSync, new DimensionManager.SyncDimensionList());
        SimpleNetworkPacket.registerReceiver(GlobalTime.PACKET_ID_SYNCTIME, GlobalTime.INSTANCE);
        SimpleNetworkPacket.registerReceiver(ChestPlate.ActivateJetpack.id, new ChestPlate.ActivateJetpack());
        SimpleNetworkPacket.registerReceiver(Config.PACKET_ID_SYNC, Config.INSTANCE);

        // write biome presets
        BiomeConfig.makePresetIfNotExist(WARM.name, WARM.create());
        BiomeConfig.makePresetIfNotExist(WARM_DRY.name, WARM_DRY.create());
        BiomeConfig.makePresetIfNotExist(MOON.name, MOON.create());
        BiomeConfig.makePresetIfNotExist(DESERT_WASTELAND.name, DESERT_WASTELAND.create());
        BiomeConfig.makePresetIfNotExist(MUSTAFAR.name, MUSTAFAR.create());
        BiomeConfig.makePresetIfNotExist(VENUS.name, VENUS.create());
        BiomeConfig.makePresetIfNotExist(OVERWORLD.name, OVERWORLD.create());
        BiomeConfig.makePresetIfNotExist(ICEWORLD.name, ICEWORLD.create());
    }

    /// mod load events /////////////////////////////////////

    public static void registerCapabilities(RegisterCapabilitiesEvent e) {
        //e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntities.ENTITY_GUIDANCE_COMPUTER.get(), (x, y) -> x.itemStackHandler);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_ROCKET_ASSEMBLER.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_SPACE_STATION_ASSEMBLER.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlockEntities.ENTITY_FUELING_STATION.get(), (x, y) -> x.tank);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_FUELING_STATION.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntities.ENTITY_ROCKET_ITEM_LOADER.get(), (x, y) -> x.inventory);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_ROCKET_ITEM_LOADER.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlockEntities.ENTITY_ROCKET_FLUID_LOADER.get(), (x, y) -> x.tank);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_ROCKET_FLUID_LOADER.get(), (x, y) -> x.battery);


        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlockEntities.ENTITY_OXYGEN_VENT.get(), (x, y) -> x.tank);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_CO2_SCRUBBER.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_SOLAR_PANEL.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_SATELLITE_MONITOR.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlockEntities.ENTITY_FLUID_RELEASE.get(), (x, y) -> y == x.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) ? null : x.tank);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlockEntities.ENTITY_PRESSURE_TANK.get(), (x, y) -> x.tank);


        ItemPortablePressureTank portablePressuretankAluminum = Items.ITEM_PORTABLE_PRESSURE_TANK_ALUMINUM.get();
        e.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) ->
                        new FluidHandlerItemStack(
                                GeneralRegistry.FLUID_CONTAINER_DATA,
                                stack,
                                portablePressuretankAluminum.capacity
                        ),
                portablePressuretankAluminum

        );

        ItemPortablePressureTank portablePressuretankSteel = Items.ITEM_PORTABLE_PRESSURE_TANK_STEEL.get();
        e.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) ->
                        new FluidHandlerItemStack(
                                GeneralRegistry.FLUID_CONTAINER_DATA,
                                stack,
                                portablePressuretankSteel.capacity
                        ),
                portablePressuretankSteel

        );
    }

    public static void registerTickets(RegisterTicketControllersEvent event) {
        event.register(ForcedChunkManager.ticketController);
    }

    public static void loadComplete(FMLLoadCompleteEvent e) {
        ARLib.holoProjector.itemHoloProjector.registerMultiblock("Observatory", EntityObservatory.structure, EntityObservatory.charMapping);
        ARLib.holoProjector.itemHoloProjector.registerMultiblock("Asrobody Data Processor", EntityAstrobodyDataProcessor.structure, EntityAstrobodyDataProcessor.charMapping);
        TerraformingSystem.setup();
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("adv_rocketry_reset_galaxy")
                        .requires(source -> source.hasPermission(2))
                        .executes((context) -> {
                            context.getSource().getPlayer().sendSystemMessage(Component.literal("Reloading properties now..."));
                            context.getSource().getPlayer().sendSystemMessage(Component.literal("This will NOT remove dimensions already loaded or saved in your world folder!"));
                            DimensionManager.INSTANCE_SERVER.reloadPropertiesFromConfig();
                            context.getSource().getPlayer().sendSystemMessage(Component.literal("Properties reloaded!"));
                            return 1;
                        })
        );

        event.getDispatcher().register(
                Commands.literal("adv_rocketry_reload_config")
                        .requires(source -> source.hasPermission(2))
                        .executes((context) -> {
                            context.getSource().getPlayer().sendSystemMessage(Component.literal("Reloading configs now..."));
                            Config.INSTANCE = Config.loadConfig();
                            ClientConfig.INSTANCE = ClientConfig.loadConfig();
                            for (ServerPlayer p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                                Config.SyncConfig(p);
                            }
                            return 1;
                        })
        );


        event.getDispatcher().register(
                Commands.literal("adv_rocketry_debug")
                        .executes((context) -> {
                            //TerraformingSystem.setup();
                            return 1;
                        })
        );
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(GeneralRegistry.CUSTOM_CREATIVE_TAB.get())) {
            e.accept(Blocks.LAUNCHPAD.get());
            e.accept(Blocks.STRUCTURE_TOWER.get());

            e.accept(Blocks.ROCKET_MOTOR.get());
            e.accept(Blocks.ROCKET_MOTOR_IMPROVED.get());
            e.accept(Blocks.FUEL_TANK.get());
            e.accept(Blocks.GUIDANCE_COMPUTER.get());
            e.accept(Blocks.CARGO_HOLD.get());
            e.accept(Blocks.SEAT.get());
            e.accept(Blocks.DRILL.get());
            e.accept(Blocks.GAS_INTAKE.get());

            e.accept(Blocks.ROCKET_ASSEMBLER.get());
            e.accept(Blocks.FUELING_STATION.get());
            e.accept(Items.ITEM_ROCKET_FUEL_BUCKET.get());
            e.accept(Blocks.LAUNCH_STATION.get());
            e.accept(Blocks.ROCKET_ITEM_LOADER.get());
            e.accept(Blocks.ROCKET_FLUID_LOADER.get());

            e.accept(Blocks.SPACE_STATION_ASSEMBLER.get());
            e.accept(Blocks.STATION_CONTROLLER.get());
            e.accept(Blocks.ORIENTATION_CONTROLLER.get());
            e.accept(Blocks.WARP_CONTROLLER.get());

            e.accept(Blocks.DATA_STORAGE_BLOCK.get());
            e.accept(Blocks.SOLAR_PANEL.get());
            e.accept(Blocks.OBSERVATORY.get());
            e.accept(Blocks.ASTROBODY_DATA_PROCESSOR.get());
            e.accept(Blocks.OXYGEN_VENT.get());
            e.accept(Blocks.CO2_SCRUBBER.get());
            e.accept(Blocks.WIRELESS_TRANSCEIVER.get());
            e.accept(Blocks.FLUID_RELEASE.get());
            e.accept(Blocks.PRESSURE_TANK.get());
            e.accept(Blocks.SUIT_WORKSTATION.get());

            e.accept(Blocks.MOON_TURF.get());
            e.accept(Blocks.MOON_TURF_DARK.get());
            e.accept(Blocks.DRY_ICE.get());

            e.accept(Blocks.CRYSTAL_RED.get());
            e.accept(Blocks.CRYSTAL_ORANGE.get());
            e.accept(Blocks.CRYSTAL_YELLOW.get());
            e.accept(Blocks.CRYSTAL_GREEN.get());
            e.accept(Blocks.CRYSTAL_BLUE.get());
            e.accept(Blocks.CRYSTAL_PURPLE.get());

            e.accept(Items.ITEM_LINKER.get());
            e.accept(Items.ITEM_GALAXY_DATABASE.get());
            e.accept(Items.ITEM_PLANET_ID_CHIP.get());
            e.accept(Items.ITEM_ASTEROID_ID_CHIP.get());
            e.accept(Items.ITEM_DATA_STORAGE.get());
            e.accept(Items.ITEM_ATM_ANALYZER.get());
            e.accept(Items.ITEM_PORTABLE_PRESSURE_TANK_ALUMINUM.get());
            e.accept(Items.ITEM_PORTABLE_PRESSURE_TANK_STEEL.get());

            e.accept(Blocks.SATELLITE_ASSEMBLER.get());
            e.accept(Blocks.SATELLITE_MONITOR.get());
            e.accept(Blocks.LAUNCH_STATION_SATELLITE_MISSIONS.get());
            e.accept(Blocks.LAUNCH_STATION_ASTEROID_MISSIONS.get());
            e.accept(Blocks.LAUNCH_STATION_GAS_MINING_MISSIONS.get());

            e.accept(Items.ITEM_SATELLITE.get());
            e.accept(Items.ITEM_SATELLITE_OPTICAL_TELESCOPE.get());
            e.accept(Items.ITEM_SATELLITE_MASS_SCANNER.get());
            e.accept(Items.ITEM_SATELLITE_COMPOSITION_SCANNER.get());
            e.accept(Items.ITEM_SATELLITE_ID_CHIP.get());
            e.accept(Items.ITEM_SATELLITE_BIOME_CHANGER.get());
            e.accept(Items.ITEM_SATELLITE_BIOME_CHANGER_REMOTE.get());
            e.accept(Items.ITEM_LORA_MODULE.get());
            e.accept(Items.ITEM_RADIATION_SHIELD.get());
            e.accept(Items.ITEM_BATTERY.get());

            e.accept(Items.ITEM_OXYGEN_BUCKET.get());
            e.accept(Items.ITEM_HYDROGEN_BUCKET.get());
            e.accept(Items.ITEM_NITROGEN_BUCKET.get());
            e.accept(Items.ITEM_CO2_BUCKET.get());
            e.accept(Items.ITEM_METHANE_BUCKET.get());

            e.accept(Items.ITEM_SPACE_SUIT_HELMET.get());
            e.accept(Items.ITEM_SPACE_SUIT_CHESTPLATE.get());
            e.accept(Items.ITEM_SPACE_SUIT_LEGGINGS.get());
            e.accept(Items.ITEM_SPACE_SUIT_BOOTS.get());
            e.accept(Items.ITEM_JETPACK.get());
            e.accept(Items.ITEM_NIGHT_VISION_UPGRADE.get());
            e.accept(Items.ITEM_LEGS_UPGRADE.get());
            e.accept(Items.ITEM_GRAVITY_BOOTS_UPGRADE.get());
            e.accept(Items.ITEM_FLIGHT_SPEED_UPGRADE.get());

        }
    }
}