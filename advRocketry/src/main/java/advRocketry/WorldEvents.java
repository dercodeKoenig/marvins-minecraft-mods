package advRocketry;

import advRocketry.Blocks.DryIceBlock;
import advRocketry.Dimension.*;
import advRocketry.Items.ItemAsteroidIdChip;
import advRocketry.Items.ItemLinker;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.Missions.MissionManager;
import advRocketry.Particles.RocketParticleEngine;
import advRocketry.Registry.GasRegistry;
import advRocketry.Render.SkyRenderer;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Satellites.SatelliteManager;
import advRocketry.Utils.ClientUtils;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Matrix4f;

public class WorldEvents {
    static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            for (Dimension i : DimensionManager.INSTANCE_SERVER.dimensions.values()) {
                DimensionManager.SyncDimensionProperties.syncDimensionPropertiesToPlayer(p, i);
            }
            DimensionManager.SyncDimensionList.syncDimensionListToPlayer(p);
        }
    }

    static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        ResourceLocation to = event.getTo().location();
        if (event.getEntity() instanceof ServerPlayer player) {
            Dimension dim = DimensionManager.INSTANCE_SERVER.get(to);
            if (dim != null) {
                DimensionManager.SyncDimensionProperties.syncDimensionPropertiesToPlayer(player, dim);
            }
        }
    }

    static void onServerTick(ServerTickEvent.Post event) {
        long t0 = System.nanoTime();
        DimensionManager.INSTANCE_SERVER.tick();
        GlobalTime.tickServer();
        ForcedChunkManager.tick();
        LifeSupportSystem.serverTick();
        SatelliteManager.serverTick();
        MissionManager.serverTick();
        //System.out.println((double)(System.nanoTime() - t0) / 1000 / 1000);
    }

    static void onClientTick(ClientTickEvent.Post event) {
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

    static void onServerStarted(ServerStartedEvent event) {
        Main.worldPath = event.getServer().getWorldPath(LevelResource.ROOT);
        System.out.println("set world path: " + Main.worldPath);
        GlobalTime.load(); // important to load the time first!
        DimensionManager.INSTANCE_SERVER.onServerStart(); // create dimensions next
        ForcedChunkManager.restoreForcedChunks(); // restore forced chunks after dimensions are created
        MissionManager.onServerStart();
        SatelliteManager.onServerStart();
        ItemAsteroidIdChip.onServerStart();
    }

    static void onServerStop(ServerStoppingEvent event) {
        SatelliteManager.onServerStop();
        MissionManager.onServerStop();
        ForcedChunkManager.saveForcedChunks();
        DimensionManager.INSTANCE_SERVER.onServerStop();
        GlobalTime.save();
    }

    static void onRenderStage(RenderLevelStageEvent event) {
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

    static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && DimensionManager.INSTANCE_SERVER.get(serverLevel.dimension().location()) instanceof PlanetDimension planet) {
            // perform some terraforming checks and replacement rules on new chunks

            // placement flags:
            // 2  -> sync to player
            // 16 -> no neighbor update (if i read it correctly)

            double planetTemp = planet.getCurrentTemp();
            double atmLevel = planet.getAtmosphereDensity();
            boolean shouldFreezeWater = planetTemp < GasRegistry.gases.get(GasRegistry.water).getFreezeTemp(atmLevel) - 1;

            if (event.isNewChunk()) {
                long t0 = System.nanoTime();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int xB = event.getChunk().getPos().getBlockX(x);
                        int zB = event.getChunk().getPos().getBlockZ(z);

                        for (int y = serverLevel.getMinBuildHeight(); y < serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, xB, zB); y++) {
                            BlockPos pos = new BlockPos(xB, y, zB);
                            BlockState state = serverLevel.getBlockState(pos);

                            // freeze water if possible
                            if (state.getBlock().equals(net.minecraft.world.level.block.Blocks.WATER) && shouldFreezeWater) {
                                serverLevel.setBlock(pos, net.minecraft.world.level.block.Blocks.ICE.defaultBlockState(), 2 | 16);
                            }
                        }

                        SeaLevelAdjustment.saveInitialWaterLevelOnChunkGeneration(serverLevel, event.getChunk(), xB, zB);
                        for (GasRegistry.Gas gas : GasRegistry.gases.values()) {
                            while (SeaLevelAdjustment.adjustSeaLevelIfRequired(planet, gas, xB, zB, 2 | 16)) {
                                continue; // nothing to do, all the action happens above
                            }
                        }

                        while (DryIceBlock.placeDryIceIfPossible(planet, xB, zB, 2 | 16)) {
                            continue; // nothing to do, all the action happens above
                        }
                    }
                }
                //System.out.println("block replacement on chunk load: " + (double) (System.nanoTime() - t0) / 1000 / 1000);
            }
        }
    }

    static void CalculateDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (ClientUtils.getSinglePlayer().getVehicle() instanceof EntityRocket rocket) {
            int rocketsize = rocket.size.getY();
            event.setDistance(event.getDistance() + rocketsize * 1.3f);
        }
    }

    static void onLivingFallEvent(LivingFallEvent event) {
        Level l = event.getEntity().level();
        float g = 1;
        Dimension d = DimensionManager.getDimensionManager(l.isClientSide).get(l.dimension().location());
        if (d != null)
            g = d.getGravitationalMultiplier();
        event.setDamageMultiplier(event.getDamageMultiplier() * g);
    }

    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        Player p = event.getEntity();
        Entity target = event.getTarget();
        if (stack.getItem() instanceof ItemLinker) {
            if (ItemLinker.useOnEntity(p, stack, target)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    static void onSourceCreate(CreateFluidSourceEvent e) {
        Level level = e.getLevel();
        if (e.getFluidState().getType().isSame(net.minecraft.world.level.material.Fluids.WATER)) {
            Dimension dim = DimensionManager.getDimensionManager(level.isClientSide).get(level.dimension().location());
            if (dim instanceof SpaceStationDimension) {
                // no sources on space stations
                e.setCanConvert(false);
            }
            if (dim instanceof PlanetDimension planet) {
                // can only convert if below sea level
                int seaLevel = planet.getGasProperty(GasRegistry.water).worldGenSeaLevel;
                if (e.getPos().getY() > seaLevel ||
                        planet.getCurrentTemp() > GasRegistry.gases.get(GasRegistry.water).getBoilingTemp(planet.getAtmosphereDensity())) {
                    e.setCanConvert(false);
                }
            }
        }
    }

    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        // prevent living entities to spawn where it is impossible
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Player) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        if (!LifeSupportSystem.canSurviveAt(event.getLevel(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }
}
