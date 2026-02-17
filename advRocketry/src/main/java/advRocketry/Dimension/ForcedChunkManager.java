package advRocketry.Dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class ForcedChunkManager {
    public static ForcedChunkManager INSTANCE = new ForcedChunkManager();

    // every level has a map of chunkPos, expire time
    public HashMap<ResourceLocation, HashMap<ChunkPos, Long>> forcedChunks = new HashMap<>();


    public static void keepChunkForceLoaded(Level l, ChunkPos pos) {
        keepChunkForceLoaded(l.dimension().location(), pos);
    }

    /// signals to keep this chunk loaded for the next 10 seconds
    public static void keepChunkForceLoaded(ResourceLocation l, ChunkPos pos) {
        INSTANCE.forcedChunks.putIfAbsent(l, new HashMap<>());
        HashMap<ChunkPos, Long> levelForcedChunks = INSTANCE.forcedChunks.get(l);

        if (!levelForcedChunks.containsKey(pos)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = DimensionManager.getServerLevel(server, l);

            // Use the vanilla method. This satisfies NeoForge's hasForcedChunks() check.
            // It automatically adds a Level 31 (Entity Ticking) ticket.
            level.setChunkForced(pos.x, pos.z, true);

            System.out.println("Set chunk force loaded via setChunkForced: " + pos + " in " + l);
        }

        // Keep your internal timer for expiration logic
        levelForcedChunks.put(pos, GlobalTime.getGlobalTime() + 20 * 10);
    }

    public static void tick() {
        if (GlobalTime.getGlobalTime() % 20 == 17) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

            for (ResourceLocation levelId : INSTANCE.forcedChunks.keySet()) {
                HashMap<ChunkPos, Long> levelForcedChunks = INSTANCE.forcedChunks.get(levelId);
                ServerLevel level = DimensionManager.getServerLevel(server, levelId);

                for (ChunkPos p : new ArrayList<>(levelForcedChunks.keySet())) {
                    if (levelForcedChunks.get(p) < GlobalTime.getGlobalTime()) {
                        levelForcedChunks.remove(p);

                        // Release the chunk
                        level.setChunkForced(p.x, p.z, false);

                        System.out.println("Released forced chunk: " + p + " at time " + GlobalTime.getGlobalTime());
                    }
                }
            }
        }
    }
}
