package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.Rocket.EntityRocket;
import advRocketry.utils.AxisDirections;
import advRocketry.worldgen.SpaceDimensionGeneration;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.HashMap;

public class RocketTravelDimension extends Dimension {

    public static ResourceLocation dimId = ResourceLocation.fromNamespaceAndPath(Main.MODID, "space_travel");

    public static RocketTravelDimension rocketTravelDimension = new RocketTravelDimension(new DimensionProperties());

    // a rocket should every tick or every few ticks update its chunkpos with the current global time
    // when the travel manager updates, it will remove force loaded chunks where the time was not reset for a few seconds
    static HashMap<ChunkPos, Long> usedChunksMap = new HashMap<>();

    public static void keepChunkLoaded(ChunkPos pos) {
        if (!usedChunksMap.containsKey(pos)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = DimensionManager.getServerLevel(server, dimId);
            level.setChunkForced(pos.x, pos.z, true);
            System.out.println("there are " + level.getForcedChunks().size() + " chunk force loaded in space travel dimension");
        }
        System.out.println("set chunk force loaded:" + pos.x + ":" + pos.z);
        usedChunksMap.put(pos, GlobalTime.getGlobalTime());
    }

    public static ChunkPos getNextFreeChunkPos() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = DimensionManager.getServerLevel(server, dimId);
        int x = 0;
        LongSet forcedChunks = level.getForcedChunks();
        while (true) {
            x += 50;
            ChunkPos p = new ChunkPos(x, 0);
            if (!forcedChunks.contains(p.toLong())) {
                return p;
            }
        }
    }

    public void createDimension() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        System.out.println("creating space travel dimension");
        DynamicDimensionRegistry dynamicDimensionRegistry = DynamicDimensionRegistry.from(server);

        ChunkGenerator generator = SpaceDimensionGeneration.makeChunkGenerator();
        DimensionType type = SpaceDimensionGeneration.makeDimensionType();
        ServerLevel l = dynamicDimensionRegistry.loadDynamicDimension(dimId, generator, type);
        if (l == null) {
            dynamicDimensionRegistry.createDynamicDimension(
                    dimId,
                    generator,
                    type
            );
        }
    }

    public static void init() {
        // create the dimension
       rocketTravelDimension.createDimension();

        usedChunksMap = new HashMap<>();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = DimensionManager.getServerLevel(server, dimId);
        System.out.println("there are " + level.getForcedChunks().size() + " chunk force loaded in space travel dimension");
    }

    public ClientOnly clientOnly;

    public RocketTravelDimension(DimensionProperties properties) {
        super(properties);
        if (FMLEnvironment.dist.isClient()) {
            clientOnly = new ClientOnly();
        }
    }

    @Override
    public ResourceLocation getDimensionId(){
        return dimId;
    }

    @Override
    public float getAtmosphereDensity() {
        return 0;
    }

    @Override
    public double getEarthMassMultiplier() {
        return 0.2;
    }

    @Override
    public float getDayTimePerTick() { return 0; }

        public DimensionProperties.PlanetType getType() {
        return DimensionProperties.PlanetType.DUMMY;
    }

    @Override
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        if(FMLEnvironment.dist.isClient()) return clientOnly.getGlobalAxisDirections();
        return null;
    }

    @Override
    public Vec3 getPosition(float partialTick) {
        if(FMLEnvironment.dist.isClient())
            return clientOnly.getPosition();
        else return null;
    }

    // not sure if this is even used anywhere because we have no terrain, nothing
    @Override
    public double getSurfaceDotToTarget(Dimension target, float partialTick, @Nullable Vec3 myPlanetPosition, @Nullable Vec3 targetPosition) {
        return 1;
    }

    @Override
    public void serverTick(ServerTickEvent event) {
        super.serverTick(event);
        if (GlobalTime.getGlobalTime() % 200 == 59) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = DimensionManager.getServerLevel(server, dimId);
            for (long i : level.getForcedChunks()) {
                ChunkPos pos = new ChunkPos(i);
                long currentTime = GlobalTime.getGlobalTime();
                usedChunksMap.putIfAbsent(pos, currentTime);
                if (usedChunksMap.get(pos) + 20 * 120 < currentTime) {
                    level.setChunkForced(pos.x, pos.z, false);
                    System.out.println("remove forced chunk at " + pos.x + ":" + pos.z);
                    usedChunksMap.remove(pos);
                    break; // prevent  exceptions
                }
            }
        }
    }

    public class ClientOnly {
        public Vec3 getPosition() {
            Player player = Minecraft.getInstance().player;
            if(player != null) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof EntityRocket rocket) {
                    return rocket.universePosition;
                }else{
                    return player.position().scale(0.001); // for debug flying around in creative
                }
            }
            return  new Vec3(0,0,0);
        }
        public AxisDirections getGlobalAxisDirections(){
            Player player = Minecraft.getInstance().player;
            if(player != null) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof EntityRocket rocket) {
                    return new AxisDirections(
                            rocket.universeHeading,
                            rocket.universeFront
                    );
                }
            }
            return new AxisDirections(
                    new Vec3(0, 0, -1),
                    new Vec3(0, 1, 0)
            );
        }
        public void clientTick(){
            RocketTravelDimension.super.clientOnly.clientTick();
        }
    }
}
