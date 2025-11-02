package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.Rocket.EntityRocket;
import advRocketry.worldgen.SpaceDimensionGeneration;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class SpaceTravelManager {
    public static ResourceLocation dimId = ResourceLocation.fromNamespaceAndPath(Main.MODID, "space_travel");

    public static RocketTravelDimension rocketTravelDimension = new RocketTravelDimension(new DimensionProperties());
    ;

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

    public static void update() {
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

    public static void createDimension() {
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
        createDimension();

        usedChunksMap = new HashMap<>();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = DimensionManager.getServerLevel(server, dimId);
        System.out.println("there are " + level.getForcedChunks().size() + " chunk force loaded in space travel dimension");
    }
}
