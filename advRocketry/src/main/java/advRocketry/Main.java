package advRocketry;

import ARLib.network.SimpleNetworkPacket;
import AWGenerators.Registry;
import advRocketry.BlockEntities.EntityAstrobodyDataProcessor;
import advRocketry.BlockEntities.EntityObservatory;
import advRocketry.BlockEntityRenderers.RenderObservatory;
import advRocketry.BlockEntityRenderers.RenderRocketAssembler;
import advRocketry.Dimension.*;
import advRocketry.Items.ItemAsteroidIdChip;
import advRocketry.Items.ItemLinker;
import advRocketry.Missions.MissionManager;
import advRocketry.Oxygen.OxygenSystem;
import advRocketry.Particles.RocketParticleEngine;
import advRocketry.Particles.RocketParticleProvider;
import advRocketry.Registry.*;
import advRocketry.Render.*;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RendererRocket;
import advRocketry.Satellites.SatelliteManager;
import advRocketry.Utils.ClientUtils;
import advRocketry.Worldgen.BiomeConfig;

import advRocketry.Worldgen.presets.HOT;
import advRocketry.Worldgen.presets.HOT_DRY;
import advRocketry.Worldgen.presets.MOON;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Matrix4f;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Mod(Main.MODID)
public class Main {

    public static final String MODID = "adv_rocketry";

    public static Path worldPath;
    public static Path myConfigDir;

    public Main(IEventBus modEventBus, ModContainer modContaine) {
        // game events
        if (FMLLoader.getDist().isClient()) {
            NeoForge.EVENT_BUS.addListener(this::onRenderStage);
            NeoForge.EVENT_BUS.addListener(Fog::renderFogEvent);
            NeoForge.EVENT_BUS.addListener(Fog::computeFogColorEvent);
            NeoForge.EVENT_BUS.addListener(this::onClientTick);
            NeoForge.EVENT_BUS.addListener(this::CalculateDetachedCameraDistance);
        }
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onDimensionChange);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStop);
        NeoForge.EVENT_BUS.addListener(this::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onLivingFallEvent);

        // mod loading
        modEventBus.addListener(this::registerShaders);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerParticles);
        modEventBus.addListener(this::registerClientExtensions);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::registerTickets);

        Blocks.BLOCKS.register(modEventBus);
        Items.ITEMS.register(modEventBus);
        BlockEntities.BLOCK_ENTITIES.register(modEventBus);
        GeneralRegistry.CREATIVE_TAB.register(modEventBus);
        GeneralRegistry.ENTITIES.register(modEventBus);
        GeneralRegistry.PARTICLES.register(modEventBus);
        Fluids.FLUIDS.register(modEventBus);
        Fluids.FLUID_TYPES.register(modEventBus);

        // register network packets
        SimpleNetworkPacket.registerReceiver(DimensionManager.packetDimensionPropertiesSync, new DimensionManager.SyncDimensionProperties());
        SimpleNetworkPacket.registerReceiver(DimensionManager.packetDimensionListSync, new DimensionManager.SyncDimensionList());
        SimpleNetworkPacket.registerReceiver(GlobalTime.PACKET_ID_SYNCTIME, GlobalTime.INSTANCE);

        // setup config directory
        Path configDir = FMLPaths.CONFIGDIR.get();
        myConfigDir = Path.of(String.valueOf(configDir), Main.MODID);
        File myConfigDirFile = new File(String.valueOf(myConfigDir));
        if (!myConfigDirFile.exists()) {
            myConfigDirFile.mkdirs();
        }

        // write biome presets
        BiomeConfig.makePresetIfNotExist(HOT.name, HOT.create());
        BiomeConfig.makePresetIfNotExist(HOT_DRY.name, HOT_DRY.create());
        BiomeConfig.makePresetIfNotExist(MOON.name, MOON.create());

    }

    /// game events ////////////////////////////////

    void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            for (Dimension i : DimensionManager.INSTANCE_SERVER.dimensions.values()) {
                DimensionManager.SyncDimensionProperties.syncDimensionPropertiesToPlayer(p, i);
            }
            DimensionManager.SyncDimensionList.syncDimensionListToPlayer(p);
        }
    }

    void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        ResourceLocation to = event.getTo().location();
        if (event.getEntity() instanceof ServerPlayer player) {
            Dimension dim = DimensionManager.INSTANCE_SERVER.get(to);
            if(dim != null) {
                DimensionManager.SyncDimensionProperties.syncDimensionPropertiesToPlayer(player, dim);
            }
        }
    }

    void onServerTick(ServerTickEvent.Post event) {
        DimensionManager.INSTANCE_SERVER.tick();
        GlobalTime.tickServer();
        ForcedChunkManager.tick();
        OxygenSystem.serverTick();
        SatelliteManager.serverTick();
        MissionManager.serverTick();
    }

    void onClientTick(ClientTickEvent.Post event) {
        if (ClientUtils.getPlayerLevel() == null) return; // my stuff is only for when playing
        DimensionManager.INSTANCE_CLIENT.tick();
        GlobalTime.tickClient();
        EntityRocket.onClientTickEvent();

        Dimension myDimension = ClientUtils.getPlayerDimension();
        if (myDimension != null) {
            Vec3 myPos = myDimension.getPosition(0);
            PlanetRenderCache.INSTANCE.updatePlanetsToRenderInSky(myPos);
        }

        RocketParticleEngine.tick();
    }

    void onServerStarted(ServerStartedEvent event) {
        Main.worldPath = event.getServer().getWorldPath(LevelResource.ROOT);
        System.out.println("set world path: " + worldPath);
        GlobalTime.load(); // important to load the time first!
        DimensionManager.INSTANCE_SERVER.onServerStart(); // create dimensions next
        ForcedChunkManager.restoreForcedChunks(); // restore forced chunks after dimensions are created
        MissionManager.onServerStart();
        SatelliteManager.onServerStart();
        ItemAsteroidIdChip.onServerStart();
    }

    void onServerStop(ServerStoppingEvent event) {
        SatelliteManager.onServerStop();
        MissionManager.onServerStop();
        ForcedChunkManager.saveForcedChunks();
        DimensionManager.INSTANCE_SERVER.onServerStop();
        GlobalTime.save();
    }

    void onRenderStage(RenderLevelStageEvent event) {
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        boolean is_fabulous = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FABULOUS;

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            //if(true)return;
            Matrix4f proj = event.getProjectionMatrix();
            Matrix4f view = event.getModelViewMatrix();
            SkyRenderer.INSTANCE.renderSky(proj, view, partialTick);
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            // clouds will render next, disable stupid fog
            FogRenderer.setupFog(Minecraft.getInstance().gameRenderer.getMainCamera(), FogRenderer.FogMode.FOG_SKY, 999990, false, 0);

            if (is_fabulous)
                RocketParticleEngine.renderAll(event.getFrustum(), event.getCamera(), partialTick);
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            // make it render after clouds then
            if (!is_fabulous)
                RocketParticleEngine.renderAll(event.getFrustum(), event.getCamera(), partialTick);
        }

    }

    void onChunkLoad(ChunkEvent.Load event) {
        //if(event.isNewChunk())
        //System.out.println("new chunk: "+event.getChunk().getPos());
        // TODO: trigger ore replacement
    }

    void CalculateDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (ClientUtils.getSinglePlayer().getVehicle() instanceof EntityRocket rocket) {
            int rocketsize = rocket.size.getY();
            event.setDistance(event.getDistance() + rocketsize * 1.3f);
        }
    }

    void onLivingFallEvent(LivingFallEvent event) {
        Level l = event.getEntity().level();
        float g = 1;
        Dimension d = DimensionManager.getDimensionManager(l.isClientSide).get(l.dimension().location());
        if (d != null)
            g = d.getGravitationalMultiplier();
        event.setDamageMultiplier(event.getDamageMultiplier() * g);
    }

    void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        Player p = event.getEntity();
        Entity target = event.getTarget();
        if (stack.getItem() instanceof ItemLinker) {
            if(ItemLinker.useOnEntity(p, stack, target)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    /// mod load events /////////////////////////////////////

    void registerCapabilities(RegisterCapabilitiesEvent e) {
        //e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntities.ENTITY_GUIDANCE_COMPUTER.get(), (x, y) -> x.itemStackHandler);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_ROCKET_ASSEMBLER.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_SPACE_STATION_ASSEMBLER.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlockEntities.ENTITY_FUELING_STATION.get(), (x, y) -> x.tank);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_FUELING_STATION.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntities.ENTITY_ROCKET_ITEM_LOADER.get(), (x, y) -> x.inventory);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_ROCKET_ITEM_LOADER.get(), (x, y) -> x.battery);
        //e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Registry.ENTITY_OXYGEN_VENT.get(), (x, y) -> x.);
        //e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registry.ENTITY_OXYGEN_VENT.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_SOLAR_PANEL.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntities.ENTITY_SATELLITE_MONITOR.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntities.ENTITY_LAUNCH_STATION.get(), (x, y) -> x.inventory);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntities.ENTITY_LAUNCH_STATION_SATELLITE_MISSIONS.get(), (x, y) -> x.inventory);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntities.ENTITY_LAUNCH_STATION_ASTEROID_MISSIONS.get(), (x, y) -> x.inventory);
    }

    void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GeneralRegistry.ENTITY_ROCKET.get(), RendererRocket::new);
        event.registerBlockEntityRenderer(BlockEntities.ENTITY_ROCKET_ASSEMBLER.get(), RenderRocketAssembler::new);
        event.registerBlockEntityRenderer(BlockEntities.ENTITY_SPACE_STATION_ASSEMBLER.get(), RenderRocketAssembler::new);
        event.registerBlockEntityRenderer(BlockEntities.ENTITY_OBSERVATORY.get(), RenderObservatory::new);
    }

    void registerShaders(RegisterShadersEvent event) {
        try {
            shaderUtils.localAtmosphereShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "atmosphere_shader"), shaderUtils.POSITION_NORMAL);
            event.registerShader(shaderUtils.localAtmosphereShader, x -> {
            });

            shaderUtils.planetShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "planet_shader"), shaderUtils.POSITION_TEXTURE_NORMAL);
            event.registerShader(shaderUtils.planetShader, x -> {
            });

            shaderUtils.blitAddTonemapShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_add_tonemap"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.blitAddTonemapShader, x -> {
            });

            shaderUtils.blitExtractBright = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_extract_bright"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.blitExtractBright, x -> {
            });

            shaderUtils.blitBlur = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_blur"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.blitBlur, x -> {
            });

            shaderUtils.starBackgroundShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "star_background_shader"), shaderUtils.POSITION_COLOR);
            event.registerShader(shaderUtils.starBackgroundShader, x -> {
            });

            shaderUtils.ringSystemShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "ring_system_shader"), shaderUtils.POSITION_NORMAL);
            event.registerShader(shaderUtils.ringSystemShader, x -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(Blocks.STRUCTURE_TOWER.get(), RenderType.cutout());
    }

    void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GeneralRegistry.SOFT_PARTICLE.get(), RocketParticleProvider.SoftParticleProvider::new);
        event.registerSpriteSet(GeneralRegistry.DUST_PARTICLE.get(), RocketParticleProvider.DustParticleProvider::new);
    }

    void registerClientExtensions(RegisterClientExtensionsEvent event) {
        Fluids.registerFluidTypes(event);
    }

    void registerTickets(RegisterTicketControllersEvent event) {
        event.register(ForcedChunkManager.ticketController);
    }

    void loadComplete(FMLLoadCompleteEvent e) {
        ARLib.holoProjector.itemHoloProjector.registerMultiblock("Observatory", EntityObservatory.structure, EntityObservatory.charMapping);
        ARLib.holoProjector.itemHoloProjector.registerMultiblock("Asrobody Data Processor", EntityAstrobodyDataProcessor.structure, EntityAstrobodyDataProcessor.charMapping);
    }

    void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(GeneralRegistry.CUSTOM_CREATIVE_TAB.get())) {
            e.accept(Blocks.LAUNCHPAD.get());
            e.accept(Blocks.STRUCTURE_TOWER.get());

            e.accept(Blocks.ROCKET_MOTOR.get());
            e.accept(Blocks.FUEL_TANK.get());
            e.accept(Blocks.GUIDANCE_COMPUTER.get());
            e.accept(Blocks.CARGO_HOLD.get());
            e.accept(Blocks.SEAT.get());
            e.accept(Blocks.DRILL.get());

            e.accept(Blocks.ROCKET_ASSEMBLER.get());
            e.accept(Blocks.FUELING_STATION.get());
            e.accept(Items.ITEM_ROCKET_FUEL_BUCKET.get());
            e.accept(Blocks.LAUNCH_STATION.get());
            e.accept(Blocks.ROCKET_ITEM_LOADER.get());

            e.accept(Blocks.SPACE_STATION_ASSEMBLER.get());
            e.accept(Blocks.STATION_CONTROLLER.get());
            e.accept(Blocks.ORIENTATION_CONTROLLER.get());
            e.accept(Blocks.WARP_CONTROLLER.get());

            e.accept(Blocks.DATA_STORAGE_BLOCK.get());
            e.accept(Blocks.SOLAR_PANEL.get());
            e.accept(Blocks.OBSERVATORY.get());
            e.accept(Blocks.ASTROBODY_DATA_PROCESSOR.get());
            e.accept(Blocks.OXYGEN_VENT.get());
            e.accept(Blocks.WIRELESS_TRANSCEIVER.get());

            e.accept(Blocks.MOON_TURF.get());
            e.accept(Blocks.MOON_TURF_DARK.get());

            e.accept(Items.ITEM_LINKER.get());
            e.accept(Items.ITEM_GALAXY_DATABASE.get());
            e.accept(Items.ITEM_PLANET_ID_CHIP.get());
            e.accept(Items.ITEM_ASTEROID_ID_CHIP.get());
            e.accept(Items.ITEM_DATA_STORAGE.get());

            e.accept(Blocks.SATELLITE_ASSEMBLER.get());
            e.accept(Blocks.SATELLITE_MONITOR.get());
            e.accept(Blocks.LAUNCH_STATION_SATELLITE_MISSIONS.get());
            e.accept(Blocks.LAUNCH_STATION_ASTEROID_MISSIONS.get());

            e.accept(Items.ITEM_SATELLITE.get());
            e.accept(Items.ITEM_SATELLITE_OPTICAL_TELESCOPE.get());
            e.accept(Items.ITEM_SATELLITE_MASS_SCANNER.get());
            e.accept(Items.ITEM_SATELLITE_COMPOSITION_SCANNER.get());
            e.accept(Items.ITEM_SATELLITE_ID_CHIP.get());
            e.accept(Items.ITEM_LORA_MODULE.get());
            e.accept(Items.ITEM_RADIATION_SHIELD.get());
            e.accept(Items.ITEM_BATTERY.get());

            e.accept(Items.ITEM_OXYGEN_BUCKET.get());
            e.accept(Items.ITEM_HYDROGEN_BUCKET.get());
            e.accept(Items.ITEM_NITROGEN_BUCKET.get());
            e.accept(Items.ITEM_CO2_BUCKET.get());
            e.accept(Items.ITEM_METHANE_BUCKET.get());
        }
    }
}