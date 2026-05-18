package advRocketry;

import advRocketry.BlockEntityRenderers.RenderObservatory;
import advRocketry.BlockEntityRenderers.RenderPressureTank;
import advRocketry.BlockEntityRenderers.RenderRocketAssembler;
import advRocketry.Dimension.*;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.LifeSupport.SurvivalSystem;
import advRocketry.Missions.AsteroidManager;
import advRocketry.Missions.MissionManager;
import advRocketry.Registry.BlockEntities;
import advRocketry.Registry.GasRegistry;
import advRocketry.Registry.GeneralRegistry;
import advRocketry.Render.Particles.RocketParticleEngine;
import advRocketry.Render.Particles.RocketParticleProvider;
import advRocketry.Render.SkyRenderer;
import advRocketry.Render.shaderUtils;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RendererRocket;
import advRocketry.Satellites.SatelliteManager;
import advRocketry.SpaceSuit.BackpackLayer;
import advRocketry.Utils.ChunkUtils;
import advRocketry.Utils.ClientUtils;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ClientSetup {

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GeneralRegistry.ENTITY_ROCKET.get(), RendererRocket::new);
        event.registerBlockEntityRenderer(BlockEntities.ENTITY_ROCKET_ASSEMBLER.get(), RenderRocketAssembler::new);
        event.registerBlockEntityRenderer(BlockEntities.ENTITY_SPACE_STATION_ASSEMBLER.get(), RenderRocketAssembler::new);
        event.registerBlockEntityRenderer(BlockEntities.ENTITY_OBSERVATORY.get(), RenderObservatory::new);
        event.registerBlockEntityRenderer(BlockEntities.ENTITY_PRESSURE_TANK.get(), RenderPressureTank::new);
    }

    public static void registerShaders(RegisterShadersEvent event) {
        try {
            shaderUtils.warpTravelShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "warp_travel_shader"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.warpTravelShader, x -> {});

            shaderUtils.localAtmosphereShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "atmosphere_shader"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.localAtmosphereShader, x -> {});

            shaderUtils.planetShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "planet_shader"), shaderUtils.POSITION_TEXTURE_NORMAL);
            event.registerShader(shaderUtils.planetShader, x -> {});

            shaderUtils.planetAtmShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "planet_atm_shader"), shaderUtils.POSITION_TEXTURE_NORMAL);
            event.registerShader(shaderUtils.planetAtmShader, x -> {});

            shaderUtils.blitPostProcessingShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_post_processing"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.blitPostProcessingShader, x -> {});

            shaderUtils.blitAddShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_add"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.blitAddShader, x -> {});

            shaderUtils.blitExtractBright = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_extract_bright"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.blitExtractBright, x -> {});

            shaderUtils.blitBlur = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "blit_blur"), shaderUtils.POSITION);
            event.registerShader(shaderUtils.blitBlur, x -> {});

            shaderUtils.starBackgroundShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "star_background_shader"), shaderUtils.STAR_BACKGROUND);
            event.registerShader(shaderUtils.starBackgroundShader, x -> {});

            shaderUtils.ringSystemShader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Main.MODID, "ring_system_shader"), shaderUtils.POSITION_NORMAL);
            event.registerShader(shaderUtils.ringSystemShader, x -> {});

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.ROCKET_FUEL.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.OXYGEN.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.HYDROGEN.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.NITROGEN.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.METHANE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.CO2.get(), RenderType.translucent());

        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.ROCKET_FUEL_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.OXYGEN_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.HYDROGEN_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.NITROGEN_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.METHANE_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(advRocketry.Registry.Fluids.CO2_FLOWING.get(), RenderType.translucent());
    }

    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GeneralRegistry.SOFT_PARTICLE.get(), RocketParticleProvider.SoftParticleProvider::new);
        event.registerSpriteSet(GeneralRegistry.DUST_PARTICLE.get(), RocketParticleProvider.DustParticleProvider::new);
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        advRocketry.Registry.Fluids.registerFluidTypes(event);
    }

    public static void addArmorLayers(EntityRenderersEvent.AddLayers event) {
        // Add to all player skins (default and slim)
        for (PlayerSkin.Model skinType : event.getSkins()) {
            LivingEntityRenderer<Player, PlayerModel<Player>> renderer = event.getSkin(skinType);
            if (renderer != null) {
                renderer.addLayer( new BackpackLayer<>(renderer));
            }
        }
    }
}
