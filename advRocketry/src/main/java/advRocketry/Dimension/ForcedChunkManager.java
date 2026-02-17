package advRocketry.Dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;

/// A simple wrapper for the forced chunk system
public class ForcedChunkManager {

    // <LevelId, <ChunkPos, Timeout>>
    public static HashMap<ResourceLocation, HashMap<ChunkPos, Long>> forcedChunks = new HashMap<>();

    public static void keepChunkForceLoaded(Level l, ChunkPos pos) {
        keepChunkForceLoaded(l.dimension().location(), pos);
    }

    /// signals to keep this chunk loaded for the next 10 seconds
    public static void keepChunkForceLoaded(ResourceLocation l, ChunkPos pos) {
        forcedChunks.putIfAbsent(l, new HashMap<>());
        HashMap<ChunkPos, Long> levelForcedChunks = forcedChunks.get(l);

        if (!levelForcedChunks.containsKey(pos)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = DimensionManager.getServerLevel(server, l);

            level.setChunkForced(pos.x, pos.z, true);

            System.out.println("Set chunk force loaded: " + pos + " in " + l);
        }

        levelForcedChunks.put(pos, GlobalTime.getGlobalTime() + 20 * 10);
    }

    /// cleans up force loaded chunks after some time
    public static void tick() {
        if (GlobalTime.getGlobalTime() % 20 == 17) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

            for (ResourceLocation levelId : forcedChunks.keySet()) {
                HashMap<ChunkPos, Long> levelForcedChunks = forcedChunks.get(levelId);
                ServerLevel level = DimensionManager.getServerLevel(server, levelId);

                for (ChunkPos p : new ArrayList<>(levelForcedChunks.keySet())) {
                    if (levelForcedChunks.get(p) < GlobalTime.getGlobalTime()) {
                        // chunk expired
                        levelForcedChunks.remove(p);

                        // Release the chunk
                        level.setChunkForced(p.x, p.z, false);

                        System.out.println("Released forced chunk: " + levelId + ":" + p + " at time " + GlobalTime.getGlobalTime());
                    }
                }
            }
        }
    }
}
