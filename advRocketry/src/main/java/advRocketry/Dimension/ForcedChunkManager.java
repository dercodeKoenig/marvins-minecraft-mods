package advRocketry.Dimension;

import advRocketry.Main;
import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/// A simple wrapper for the forced chunk system
/// Handles release of chunks if keepChunkForceLoaded is not called for some time
public class ForcedChunkManager {

    // for gson to serialize it, we don't save the timeout, we simply reset it on load
    public static class SaveData {
        public ArrayList<SaveEntry> entries = new ArrayList<>();
        public static class SaveEntry {
            public String levelId;
            public Long chunkPos;
        }
    }

    // <LevelId, <Position, Timeout>>
    public static HashMap<ResourceLocation, HashMap<ChunkPos, Long>> forcedChunks = new HashMap<>();

    // save file path
    public static String saveFile = Main.MODID + "_forced_chunks.json";

    public static ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Main.MODID, "forced_chunk_manager");
    public static UUID ticketOwner = UUID.fromString("848fb0e9-7630-4939-8af4-3a8c3b4dc835"); // i randomly generated this one
    public static TicketController ticketController = new TicketController(
            ID,
            (serverLevel, ticketHelper) -> {
                // dynamic dimensions are created later - this will not trigger for dynamic levels
                // we release whatever exists and later reload it
                ticketHelper.removeAllTickets(ticketOwner);
            }
    );

    // read and restore forced chunks
    public static void restoreForcedChunks() {
        forcedChunks.clear(); // clear from old sessions

        Path savePath = Path.of(Main.worldPath.toString(), saveFile);
        SaveData saveData = new SaveData();
        try {
            String data = Files.readString(savePath);
            saveData = new Gson().fromJson(data, SaveData.class);
        } catch (IOException e) {
            System.out.println("no forced chunk data found - no chunks will be restored");
        }
        for (SaveData.SaveEntry entry : saveData.entries) {
            System.out.println("restore forced chunk: " + entry.levelId + ":" + new ChunkPos(entry.chunkPos));

            ResourceLocation levelId = ResourceLocation.parse(entry.levelId);
            ChunkPos pos = new ChunkPos(entry.chunkPos);
            ServerLevel level = DimensionManager.getServerLevel(ServerLifecycleHooks.getCurrentServer(), levelId);

            // The callback to release tickets does not trigger for dynamic dimensions because they are created after the server is started.
            // If we just forceChunk (... true ...) it will not force load the chunk, idk why exactly this happens.
            // But it works if we unforce and re-force the chunk.

            ticketController.forceChunk(level, ticketOwner, pos.x, pos.z, false, true); // unforce

            keepChunkForceLoaded(ResourceLocation.parse(entry.levelId), new ChunkPos(entry.chunkPos)); // normal force load and adding to tracking map
        }
    }

    // save forces chunks
    public static void saveForcedChunks() {
        Path savePath = Path.of(Main.worldPath.toString(), saveFile);
        SaveData saveData = new SaveData();
        for (ResourceLocation levelId : forcedChunks.keySet()) {
            for (ChunkPos pos : forcedChunks.get(levelId).keySet()) {
                SaveData.SaveEntry entry = new SaveData.SaveEntry();
                entry.chunkPos = pos.toLong();
                entry.levelId = levelId.toString();
                saveData.entries.add(entry);
            }
        }
        try {
            Files.writeString(savePath, new Gson().toJson(saveData));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /// signals to keep this chunk loaded for a few seconds
    public static void keepChunkForceLoaded(Level l, ChunkPos pos) {
        keepChunkForceLoaded(l.dimension().location(), pos);
    }

    /// signals to keep this chunk loaded for a few seconds
    public static void keepChunkForceLoaded(ResourceLocation l, ChunkPos pos) {
        forcedChunks.putIfAbsent(l, new HashMap<>());
        HashMap<ChunkPos, Long> levelForcedChunks = forcedChunks.get(l);

        if (!levelForcedChunks.containsKey(pos)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = DimensionManager.getServerLevel(server, l);
            if (level != null) {
                ticketController.forceChunk(level, ticketOwner, pos.x, pos.z, true, true);
                System.out.println("Set chunk force loaded: " + pos + " in " + l);
            } else {
                System.out.println("error force loading chunk, level is null: " + l);
            }
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
                if (level == null) {
                    forcedChunks.remove(levelId);
                    System.out.println("Level for " + levelId + " is null. If a dynamic dimension was removed, this is normal.");
                    break; // prevent modification exception
                }

                for (ChunkPos pos : new ArrayList<>(levelForcedChunks.keySet())) {
                    if (levelForcedChunks.get(pos) < GlobalTime.getGlobalTime()) {
                        // chunk expired
                        levelForcedChunks.remove(pos);

                        // Release the chunk
                        ticketController.forceChunk(level, ticketOwner, pos.x, pos.z, false, true);

                        System.out.println("Released forced chunk: " + levelId + ":" + pos + " at time " + GlobalTime.getGlobalTime());
                    }
                }
            }
        }
    }
}
