package NPCs.Blocks.TownHall;

import ARLib.network.SimpleNetworkPacket;
import ARLib.utils.DimensionUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TownHallData {
    private static HashMap<String, HashMap<BlockPos, TownHallData>> staticData = new HashMap<>();

    public static class TOClientReceiver implements SimpleNetworkPacket.SimpleNetworkDataReceiver {
        public static TOClientReceiver INSTANCE = new TOClientReceiver();

        public void readClient(String data) {
            TownHallData.fromJson(data);
        }
    }

    public String name = "";
    public boolean aggressive;
    public BlockPos pos;
    public String levelId;
    public Set<String> owners = new HashSet<>();

    public TownHallData() {
    }

    public static void syncDataToPlayer(ServerPlayer player) {
        String data = toJson();
        PacketDistributor.sendToPlayer(player, new SimpleNetworkPacket("to_sync", data));
    }

    public static void syncData() {
        for (ServerPlayer i : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            syncDataToPlayer(i);
        }
    }

    public static void setChanged() {
        Path worldDir = ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT);
        String filename = "townHallData.json";
        Path filePath = worldDir.resolve(filename);
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            Files.writeString(filePath, TownHallData.toJson());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        syncData();
    }

    public static void verifyExist(Level l) {
        staticData.putIfAbsent(DimensionUtils.getLevelId(l), new HashMap<>());
    }

    public static void verifyExist(Level l, BlockPos p) {
        verifyExist(l);
        staticData.get(DimensionUtils.getLevelId(l)).putIfAbsent(p, new TownHallData());
    }

    public static Set<String> getOwners(Level level, BlockPos pos) {
        verifyExist(level, pos);
        return staticData.get(DimensionUtils.getLevelId(level)).get(pos).owners;
    }

    public static void addOwner(Level l, BlockPos p, String owner) {
        verifyExist(l, p);
        staticData.get(DimensionUtils.getLevelId(l)).get(p).owners.add(owner);
        setChanged();
    }

    public static void removeOwner(Level l, BlockPos p, String owner) {
        verifyExist(l, p);
        staticData.get(DimensionUtils.getLevelId(l)).get(p).owners.remove(owner);
        setChanged();
    }

    public static String getName(Level level, BlockPos pos) {
        verifyExist(level, pos);
        return staticData.get(DimensionUtils.getLevelId(level)).get(pos).name;
    }

    public static void setName(Level l, BlockPos p, String name) {
        verifyExist(l, p);
        staticData.get(DimensionUtils.getLevelId(l)).get(p).name = name;
        setChanged();
    }

    public static void removeEntry(Level l, BlockPos p) {
        verifyExist(l);
        staticData.get(DimensionUtils.getLevelId(l)).remove(p);
        setChanged();
    }

    public static HashMap<BlockPos, TownHallData> getEntries(Level l) {
        verifyExist(l);
        return staticData.get(DimensionUtils.getLevelId(l));
    }

    public static List<TownHallData> toList() {
        List<TownHallData> list = new ArrayList<>();
        for (String s : staticData.keySet()) {
            if(s==null)continue;
            for (BlockPos p : staticData.get(s).keySet()) {
                if(p==null)continue;
                TownHallData c = staticData.get(s).get(p);
                c.levelId = s;
                c.pos = p;
                list.add(c);
            }
        }
        return list;
    }

    public static void createStaticMap(List<TownHallData> list) {
        staticData = new HashMap<>();
        for (TownHallData i : list) {
            String levelId = i.levelId;
            BlockPos pos = i.pos;
            staticData.putIfAbsent(levelId, new HashMap<>());
            staticData.get(levelId).put(pos, i);
        }
    }

    public static String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String s = gson.toJson(toList());
        return s;
    }

    public static void fromJson(String json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type mapType = new TypeToken<List<TownHallData>>() {
        }.getType();
        List<TownHallData> list = gson.fromJson(json, mapType);
        createStaticMap(list);
    }

    public static void onServerStarting(ServerStartingEvent event) {
        Path configDir = ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT);
        String filename = "townHallData.json";
        Path filePath = configDir.resolve(filename);
        if (Files.exists(filePath)) {
            try {
                String s = Files.readString(filePath);
                TownHallData.fromJson(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JsonSyntaxException j) {
                System.err.println(j);
            }
        }
    }
}
