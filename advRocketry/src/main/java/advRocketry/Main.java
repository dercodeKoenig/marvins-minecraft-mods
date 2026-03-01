package advRocketry;

import ARLib.network.SimpleNetworkPacket;
import advRocketry.BlockEntities.EntityGuidanceComputer;
import advRocketry.BlockEntities.EntityObservatory;
import advRocketry.BlockEntityRenderers.RenderObservatory;
import advRocketry.BlockEntityRenderers.RenderRocketAssembler;
import advRocketry.Dimension.*;
import advRocketry.Items.ItemLinker;
import advRocketry.Oxygen.OxygenSystem;
import advRocketry.Particles.RocketParticleEngine;
import advRocketry.Particles.RocketParticleProvider;
import advRocketry.Render.*;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RendererRocket;
import advRocketry.utils.ClientUtils;
import advRocketry.worldgen.BiomeConfig;

import advRocketry.worldgen.presets.HOT;
import advRocketry.worldgen.presets.HOT_DRY;
import advRocketry.worldgen.presets.MOON;
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

        Registry.BLOCKS.register(modEventBus);
        Registry.ITEMS.register(modEventBus);
        Registry.BLOCK_ENTITIES.register(modEventBus);
        Registry.CREATIVE_TAB.register(modEventBus);
        Registry.ENTITIES.register(modEventBus);
        Registry.PARTICLES.register(modEventBus);
        Registry.FLUIDS.register(modEventBus);
        Registry.FLUID_TYPES.register(modEventBus);

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

    void onServerTick(ServerTickEvent.Post event) {
        DimensionManager.INSTANCE_SERVER.tick();
        GlobalTime.tickServer();
        ForcedChunkManager.tick();
        OxygenSystem.serverTick();
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
        DimensionManager.INSTANCE_SERVER.onServerStart();
        ForcedChunkManager.restoreForcedChunks(); // restore forced chunks after dimensions are created
    }

    void onServerStop(ServerStoppingEvent event) {
        GlobalTime.save();
        ForcedChunkManager.saveForcedChunks();
        DimensionManager.INSTANCE_SERVER.onServerStop();
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
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Registry.ENTITY_GUIDANCE_COMPUTER.get(), (x, y) -> (((EntityGuidanceComputer) x).itemStackHandler));
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registry.ENTITY_ROCKET_ASSEMBLER.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registry.ENTITY_SPACE_STATION_ASSEMBLER.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Registry.ENTITY_FUELING_STATION.get(), (x, y) -> x.tank);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registry.ENTITY_FUELING_STATION.get(), (x, y) -> x.battery);
        //e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Registry.ENTITY_OXYGEN_VENT.get(), (x, y) -> x.);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registry.ENTITY_OXYGEN_VENT.get(), (x, y) -> x.battery);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Registry.ENTITY_ROCKET_ITEM_LOADER.get(), (x, y) -> x.inventory);
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Registry.ENTITY_ROCKET_ITEM_LOADER.get(), (x, y) -> x.battery);
    }

    void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registry.ENTITY_ROCKET.get(), RendererRocket::new);
        event.registerBlockEntityRenderer(Registry.ENTITY_ROCKET_ASSEMBLER.get(), RenderRocketAssembler::new);
        event.registerBlockEntityRenderer(Registry.ENTITY_SPACE_STATION_ASSEMBLER.get(), RenderRocketAssembler::new);
        event.registerBlockEntityRenderer(Registry.ENTITY_OBSERVATORY.get(), RenderObservatory::new);
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
        ItemBlockRenderTypes.setRenderLayer(Registry.STRUCTURE_TOWER.get(), RenderType.cutout());
    }

    void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Registry.SOFT_PARTICLE.get(), RocketParticleProvider::new);
    }

    void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    @Override
                    public int getTintColor() {
                        return 0xffffffff;
                    }

                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fuel_still");
                    }

                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "block/fuel_flow");
                    }
                }, Registry.ROCKET_FUEL_TYPE.get()
        );
    }

    void registerTickets(RegisterTicketControllersEvent event) {
        event.register(ForcedChunkManager.ticketController);
    }

    void loadComplete(FMLLoadCompleteEvent e) {
        ARLib.holoProjector.itemHoloProjector.registerMultiblock("Observatory", EntityObservatory.structure, EntityObservatory.charMapping);
    }

    void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(Registry.CUSTOM_CREATIVE_TAB.get())) {
            e.accept(Registry.LAUNCHPAD.get());
            e.accept(Registry.STRUCTURE_TOWER.get());
            e.accept(Registry.ROCKET_ASSEMBLER.get());
            e.accept(Registry.ROCKET_MOTOR.get());
            e.accept(Registry.FUEL_TANK.get());
            e.accept(Registry.GUIDANCE_COMPUTER.get());
            e.accept(Registry.CARGO_HOLD.get());
            e.accept(Registry.SEAT.get());
            e.accept(Registry.ROCKET_FUEL_BUCKET.get());
            e.accept(Registry.FUELING_STATION.get());
            e.accept(Registry.ITEM_LINKER.get());
            e.accept(Registry.ITEM_GALAXY_STORAGE_DISK.get());
            e.accept(Registry.ITEM_PLANET_ID_CHIP.get());
            e.accept(Registry.OBSERVATORY.get());
            e.accept(Registry.ROCKET_ITEM_LOADER.get());
            e.accept(Registry.OXYGEN_VENT.get());
            e.accept(Registry.SPACE_STATION_ASSEMBLER.get());
            e.accept(Registry.MOON_TURF.get());
            e.accept(Registry.MOON_TURF_DARK.get());
        }
    }
}