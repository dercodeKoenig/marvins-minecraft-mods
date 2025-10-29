package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.Rocket.EntityRocket;
import advRocketry.worldgen.SpaceDimensionGeneration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

public class SpaceTravelManager {
    public static String saveFile = "spaceTravelChunks.json";
    public static ResourceLocation dimId = ResourceLocation.fromNamespaceAndPath(Main.MODID, "space_travel");
    public static HashSet<Integer> usedChunks;

    public static ChunkPos idToChunkPos(int id) {
        return new ChunkPos(0, id * 50);
    }

    public static void update(){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = DimensionManager.getServerLevel(server, dimId);
        HashSet<ChunkPos> chunksWithRockets = new HashSet<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof EntityRocket) {
               chunksWithRockets.add(e.chunkPosition());
            }
        }
        
        for (int i : usedChunks) {
            ChunkPos pos = idToChunkPos(i);
            if(!chunksWithRockets.contains(pos)){
                // this chunk has no rocket (it was removed or teleported away or whatever)
                level.setChunkForced(pos.x,pos.z,false);
                usedChunks.remove(i);
                break; // prevent concurrent modification problem, this method will run every few seconds so no problem
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

    public static void load() {
        // create the dimension
        createDimension();

        // read what chunks currently are in use
        usedChunks = new HashSet<>();
        Path saveFile = Path.of(String.valueOf(Main.worldPath), SpaceTravelManager.saveFile);
        try {
            String json = Files.readString(saveFile);
            usedChunks = new Gson().fromJson(json, HashSet.class);
        } catch (IOException e) {
            System.out.println("No space travel chunk data found. This is normal for a new world.");
        }
        System.out.println("space travel chunks in use:" + usedChunks.size());

        // force load the chunks so the rocket can tick and travel even if no player there
        for (int i : usedChunks) {
            ChunkPos pos = idToChunkPos(i);
            ServerLevel level = DimensionManager.getServerLevel(ServerLifecycleHooks.getCurrentServer(), dimId);
            level.setChunkForced(pos.x,pos.z,true);
        }
    }

    public static void save() {
        Path saveFile = Path.of(String.valueOf(Main.worldPath), SpaceTravelManager.saveFile);
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(usedChunks);
        try {
            Files.writeString(saveFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("saved space travel chunk ids");
    }
}
