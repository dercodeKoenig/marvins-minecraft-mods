package advRocketry;

import advRocketry.Dimension.*;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.LifeSupport.SurvivalSystem;
import advRocketry.Missions.AsteroidManager;
import advRocketry.Missions.MissionManager;
import advRocketry.Render.Particles.RocketParticleEngine;
import advRocketry.Registry.GasRegistry;
import advRocketry.Render.SkyRenderer;
import advRocketry.Render.starmap.SpaceMapPlanetRenderCache;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Satellites.SatelliteManager;
import advRocketry.SpaceSuit.Boots;
import advRocketry.SpaceSuit.SpaceSuit;
import advRocketry.Utils.ChunkUtils;
import advRocketry.Utils.ClientUtils;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Set;

public class WorldEvents {
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            for (Dimension i : DimensionManager.INSTANCE_SERVER.dimensions.values()) {
                DimensionManager.SyncDimensionProperties.syncDimensionPropertiesToPlayer(p, i);
            }
            DimensionManager.SyncDimensionList.syncDimensionListToPlayer(p);
        }
    }

    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        ResourceLocation to = event.getTo().location();
        if (event.getEntity() instanceof ServerPlayer player) {
            Dimension dim = DimensionManager.INSTANCE_SERVER.get(to);
            if (dim != null) {
                DimensionManager.SyncDimensionProperties.syncDimensionPropertiesToPlayer(player, dim);
            }
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        DimensionManager.INSTANCE_SERVER.tick();
        GlobalTime.tickServer();
        ForcedChunkManager.tick();
        LifeSupportSystem.serverTick();
        SatelliteManager.serverTick();
        MissionManager.serverTick();
        SpaceSuit.serverTick();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
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

        SpaceSuit.clientTick();
    }

    public static void onServerStarted(ServerStartedEvent event) {

        // write biome presets (TODO: this is done in main(), remove after testing)
        /*
        BiomeConfig.makePresetIfNotExist(WARM.name, WARM.create());
        BiomeConfig.makePresetIfNotExist(WARM_DRY.name, WARM_DRY.create());
        BiomeConfig.makePresetIfNotExist(MOON.name, MOON.create());
        BiomeConfig.makePresetIfNotExist(DESERT_WASTELAND.name, DESERT_WASTELAND.create());
        BiomeConfig.makePresetIfNotExist(MUSTAFAR.name, MUSTAFAR.create());
        BiomeConfig.makePresetIfNotExist(VENUS.name, VENUS.create());
         */


        Main.worldPath = event.getServer().getWorldPath(LevelResource.ROOT);
        System.out.println("set world path: " + Main.worldPath);
        GlobalTime.load(); // important to load the time first!
        DimensionManager.INSTANCE_SERVER.onServerStart(); // create dimensions next
        ForcedChunkManager.restoreForcedChunks(); // restore forced chunks after dimensions are created
        MissionManager.onServerStart();
        SatelliteManager.onServerStart();
        AsteroidManager.onServerStart();
    }

    public static void onServerStop(ServerStoppingEvent event) {
        AsteroidManager.onServerStop();
        SatelliteManager.onServerStop();
        MissionManager.onServerStop();
        ForcedChunkManager.saveForcedChunks();
        DimensionManager.INSTANCE_SERVER.onServerStop();
        GlobalTime.save();
        LifeSupportSystem.onServerStop();
    }

    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        DimensionManager.INSTANCE_CLIENT.dimensions.clear();
        PlanetRenderCache.INSTANCE.clearCache();
        SpaceMapPlanetRenderCache.INSTANCE.clearCache();
    }

    public static void onRenderStage(RenderLevelStageEvent event) {
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

    public static void CalculateDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (ClientUtils.getSinglePlayer().getVehicle() instanceof EntityRocket rocket) {
            int rocketsize = rocket.size.getY();
            event.setDistance(event.getDistance() + rocketsize * 1.3f);
        }
    }

    public static void onLivingFallEvent(LivingFallEvent event) {
        Level l = event.getEntity().level();
        float g = 1;
        Dimension d = DimensionManager.getDimensionManager(l.isClientSide).get(l.dimension().location());
        if (d != null)
            g = d.getGravitationalMultiplier();
        event.setDamageMultiplier((float) (event.getDamageMultiplier() * Math.pow(g, 1.5)));

        // boots upgrade reduces fall damage
        if(event.getEntity() instanceof Player player){
            ItemStack bootsStack = player.getItemBySlot(EquipmentSlot.FEET);
            if (bootsStack.getItem() instanceof Boots bootsItem) {
                CompoundTag data = bootsItem.getCachedDataUnsafe(bootsStack);
                if (data.contains("gravityBootsUpgrade") && data.getBoolean("gravityBootsUpgrade")) {
                    event.setDamageMultiplier((float) (event.getDamageMultiplier() * 0.1));
                    //System.out.println("reduce fall damage");
                }
            }
        }
    }

    public static void onMobSpawn(MobSpawnEvent.SpawnPlacementCheck event) {
        // prevent mobs to spawn where it is impossible
        if (event.getLevel().isClientSide()) return;
        EntityType<?> type = event.getEntityType();

        Dimension dim = DimensionManager.INSTANCE_SERVER.get(event.getLevel().getLevel().dimension().location());
        if (dim == null) return;

        Set<Dimension.SurvivalProblem> problems = dim.getSurvivalProblems();
        if (!SurvivalSystem.getSurvivalRule(type).allowInMobSpawn(type, event.getLevel().getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), problems)) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    public static void onSourceCreate(CreateFluidSourceEvent e) {
        Level level = e.getLevel();
        if (e.getFluidState().is(Fluids.WATER)) {
            Dimension dim = DimensionManager.getDimensionManager(level.isClientSide).get(level.dimension().location());
            if (dim instanceof SpaceStationDimension) {
                // no sources on space stations
                e.setCanConvert(false);
            }
            if (dim instanceof PlanetDimension planet) {

                // the sea level that this chunk should have
                int maxSeaLevel = planet.getGasProperty(GasRegistry.water).worldGenSeaLevel;

                // if terraforming increases sea level, do not form a source at target sea level
                // until the xz position had its sea level adjusted or there can be leftover sources when sea level goes down again
                // ok, so very important, the event position is the fucking neighbor of the block in question
                // and because this bullshit event doesnt provide a direction, i will check if any block around here is yet to be adjusted

                // because when it makes a source and the chunk stops ticking while increasing sea level,
                // and sea level goes down again, this source is not registered in the chunk tag,
                // and this would cause it to not be considered "ocean" when the sea level is adjusted lower
                for (Direction direction : List.of(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH)) {
                    BlockPos positionPossiblyInQuestion = e.getPos().relative(direction);
                    ChunkAccess chunk = level.getChunk(positionPossiblyInQuestion);
                    CompoundTag chunkEntry = ChunkUtils.getEntryOrNew(chunk, SeaLevelAdjustment.tagKey);
                    int[] seaLevels = SeaLevelAdjustment.getOrInitSeaLevelArray(chunkEntry, GasRegistry.water);
                    int localIndex = SeaLevelAdjustment.getLocalIndex(positionPossiblyInQuestion.getX(), positionPossiblyInQuestion.getZ());
                    int seaLevelExisting = seaLevels[localIndex];

                    maxSeaLevel = Math.min(maxSeaLevel, seaLevelExisting);
                }


                // can only convert if below sea level
                if (e.getPos().getY() > maxSeaLevel ||
                        planet.getCurrentTemp() > GasRegistry.gases.get(GasRegistry.water).getBoilingTemp(planet.getAtmosphereDensity())) {
                    e.setCanConvert(false);
                }
            }
        }
    }
}
