package advRocketry;

import ARLib.network.SimpleNetworkPacket;
import advRocketry.BlockEntities.EntityGuidanceComputer;
import advRocketry.BlockEntityRenderers.RenderRocketAssembler;
import advRocketry.Dimension.*;
import advRocketry.Particles.RocketFlameParticleProvider;
import advRocketry.Render.PlanetRenderCache;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RendererRocket;
import advRocketry.utils.ClientUtils;
import advRocketry.worldgen.BiomeConfig;

import advRocketry.Render.Fog;
import advRocketry.Render.shaderUtils;
import advRocketry.Render.SkyRenderer;
import advRocketry.worldgen.presets.HOT_DRY;
import advRocketry.worldgen.presets.HOT_VERYDRY;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
        //modEventBus.register(this);
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
        NeoForge.EVENT_BUS.addListener(this::onEntityLeaveWorld);

        modEventBus.addListener(this::loadShaders);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerParticles);


        Registry.BLOCKS.register(modEventBus);
        Registry.ITEMS.register(modEventBus);
        Registry.BLOCK_ENTITIES.register(modEventBus);
        Registry.CREATIVE_TAB.register(modEventBus);
        Registry.ENTITIES.register(modEventBus);
        Registry.PARTICLES.register(modEventBus);

        // register network packets
        SimpleNetworkPacket.registerReceiver(DimensionManager. packetDimensionPropertiesSync, new DimensionManager.SyncDimensionProperties());
        SimpleNetworkPacket.registerReceiver(DimensionManager. packetDimensionListSync, new DimensionManager.SyncDimensionList());
        SimpleNetworkPacket.registerReceiver(GlobalTime.PACKET_ID_SYNCTIME, GlobalTime.INSTANCE);

        Path configDir = FMLPaths.CONFIGDIR.get();
        myConfigDir = Path.of(String.valueOf(configDir), Main.MODID);
        File myConfigDirFile = new File(String.valueOf(myConfigDir));
        if (!myConfigDirFile.exists()) {
            myConfigDirFile.mkdirs();
        }

        BiomeConfig.makePresetIfNotExist(HOT_DRY.name, HOT_DRY.create());
        BiomeConfig.makePresetIfNotExist(HOT_VERYDRY.name, HOT_VERYDRY.create());

    }

    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event){
        if( event.getEntity() instanceof  ServerPlayer p){
            for(Dimension i : DimensionManager.INSTANCE_SERVER.dimensions.values()) {
                DimensionManager.SyncDimensionProperties.syncDimensionPropertiesToPlayer(p, i);
            }
            DimensionManager.SyncDimensionList.syncDimensionListToPlayer(p);
        }
    }

    public void onServerTick(ServerTickEvent.Post event) {
        DimensionManager.INSTANCE_SERVER.tick();
        GlobalTime.tickServer();
    }

    public void onClientTick(ClientTickEvent.Post event) {
        if(ClientUtils.getPlayerLevel() == null)return; // my stuff is only for when playing
        DimensionManager.INSTANCE_CLIENT.tick();
        GlobalTime.tickClient();
        EntityRocket.onClientTickEvent();
        PlanetRenderCache.updatePlanetsToRenderInSky();
    }

    public void onServerStarted(ServerStartedEvent event) {
        Main.worldPath = event.getServer().getWorldPath(LevelResource.ROOT);
        System.out.println("set world path: " + worldPath);
        GlobalTime.load();
        DimensionManager.INSTANCE_SERVER.onServerStart();
    }

    public void onServerStop(ServerStoppingEvent event) {
        GlobalTime.save();
        DimensionManager.INSTANCE_SERVER.onServerStop();
    }

    private void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            //if(true)return;
            Matrix4f proj = event.getProjectionMatrix();
            Matrix4f view = event.getModelViewMatrix();
            SkyRenderer.INSTANCE.renderSky(proj, view, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            // clouds will render next, disable stupid fog
            FogRenderer.setupFog(Minecraft.getInstance().gameRenderer.getMainCamera(), FogRenderer.FogMode.FOG_SKY, 999990, false, 0);
        }
    }

    public void onEntityLeaveWorld(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof EntityRocket rocket) {
            rocket.closeVertexBuffer();
        }
    }

    public void CalculateDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event){
        if(ClientUtils.getSinglePlayer().getVehicle() instanceof EntityRocket rocket) {
            int rocketsize = rocket.size.getY();
            event.setDistance(event.getDistance() + rocketsize*1.3f);
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Registry.ENTITY_GUIDANCE_COMPUTER.get(), (x, y) -> (((EntityGuidanceComputer)x).itemStackHandler));
    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registry.ENTITY_ROCKET.get(), RendererRocket::new);
        event.registerBlockEntityRenderer(Registry.ENTITY_ROCKET_ASSEMBLER.get(), RenderRocketAssembler::new);
    }

    private void loadShaders(RegisterShadersEvent event) {
        // 3. Register the shader and set the static field in the callback
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

    public void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(Registry.STRUCTURE_TOWER.get(), RenderType.cutout());
    }

    public void registerParticles(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(Registry.ROCKET_FLAME.get(), RocketFlameParticleProvider::new);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTab().equals(Registry.CUSTOM_CREATIVE_TAB.get())) {
            e.accept(Registry.LAUNCHPAD.get());
            e.accept(Registry.STRUCTURE_TOWER.get());
            e.accept(Registry.ROCKET_ASSEMBLER.get());
            e.accept(Registry.ROCKET_MOTOR.get());
            e.accept(Registry.FUEL_TANK.get());
            e.accept(Registry.GUIDANCE_COMPUTER.get());
            e.accept(Registry.SEAT.get());
        }
    }
}