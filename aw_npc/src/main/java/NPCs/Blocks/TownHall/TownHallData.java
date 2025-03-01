package NPCs.Blocks.TownHall;

import ARLib.network.SimpleNetworkPacket;
import ARLib.utils.DimensionUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.level.LevelEvent;
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
    private static boolean hasChanges = false;

    public static class TOClientReceiver implements SimpleNetworkPacket.SimpleNetworkDataReceiver {
        public static TOClientReceiver INSTANCE = new TOClientReceiver();

        public void readClient(String data) {
            TownHallData.fromJson(data);
            //System.out.println(data);
        }
    }

    public String name = "";
    public boolean aggressive;
    public BlockPos pos;
    public Set<String> owners = new HashSet<>();

    public TownHallData(BlockPos p) {
        this.pos = p;
    }

    public static void syncDataToPlayer(ServerPlayer player) {
        HashMap<String, List<TownHallData>> byLevelMap = getFromStaticMap();
        for (String dimension : byLevelMap.keySet()) {
            List<TownHallData> entries = byLevelMap.get(dimension);
            entries.removeIf((entry) -> {
                return !entry.owners.contains(player.getName().getString());
            });
        }
        Gson gson = new GsonBuilder().create();
        String data = gson.toJson(byLevelMap);
        PacketDistributor.sendToPlayer(player, new SimpleNetworkPacket("to_sync", data));
    }

    public static void syncData() {
        for (ServerPlayer i : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            syncDataToPlayer(i);
        }
    }

    public static void setChanged() {
        hasChanges = true;
        syncData();
    }

    public static void verifyExist(Level l, BlockPos p) {
        staticData.putIfAbsent(DimensionUtils.getLevelId(l), new HashMap<>());
        if (p != null)
            staticData.get(DimensionUtils.getLevelId(l)).putIfAbsent(p, new TownHallData(p));
    }

    public static Set<String> getOwners(Level level, BlockPos pos) {
        if (pos == null) return new HashSet<>();
        verifyExist(level, pos);
        Set<String> ret = staticData.get(DimensionUtils.getLevelId(level)).get(pos).owners;
        if (ret == null) ret = new HashSet<>();
        return ret;
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
        if (pos == null) return "";
        verifyExist(level, pos);
        return staticData.get(DimensionUtils.getLevelId(level)).get(pos).name;
    }

    public static void setName(Level l, BlockPos p, String name) {
        verifyExist(l, p);
        staticData.get(DimensionUtils.getLevelId(l)).get(p).name = name;
        setChanged();
    }

    public static void removeEntry(Level l, BlockPos p) {
        verifyExist(l, null);
        staticData.get(DimensionUtils.getLevelId(l)).remove(p);
        setChanged();
    }

    public static HashMap<BlockPos, TownHallData> getEntries(Level l) {
        verifyExist(l, null);
        return staticData.get(DimensionUtils.getLevelId(l));
    }

    public static HashMap<String, List<TownHallData>> getFromStaticMap() {
        HashMap<String, List<TownHallData>> map = new HashMap<>();
        for (String s : staticData.keySet()) {
            map.put(s, new ArrayList<>());
            for (BlockPos p : staticData.get(s).keySet()) {
                map.get(s).add(staticData.get(s).get(p));
            }
        }
        return map;
    }

    public static void createStaticMap(HashMap<String, List<TownHallData>> map) {
        staticData = new HashMap<>();
        for (Level l : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            staticData.putIfAbsent(DimensionUtils.getLevelId(l), new HashMap<>());
            if (map.containsKey(DimensionUtils.getLevelId(l))) {
                for (TownHallData i : map.get(DimensionUtils.getLevelId(l))) {
                    if (i != null)
                        staticData.get(DimensionUtils.getLevelId(l)).put(i.pos, i);
                }
            }
        }
    }

    public static String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String s = gson.toJson(getFromStaticMap());
        return s;
    }

    public static void fromJson(String json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type mapType = new TypeToken<HashMap<String, List<TownHallData>>>() {
        }.getType();
        HashMap<String, List<TownHallData>> map = gson.fromJson(json, mapType);
        createStaticMap(map);
    }

    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel().isClientSide()) return;
        if (hasChanges) {
            Path configDir = Paths.get(FMLPaths.GAMEDIR.get().toString()).resolve(event.getLevel().getServer().getWorldPath(LevelResource.ROOT));
            String filename = "townHallData.json";
            Path filePath = configDir.resolve(filename);
            try {
                if (!Files.exists(filePath)) {
                    Files.createFile(filePath);
                }
                Files.writeString(filePath, TownHallData.toJson());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        hasChanges = false;
    }

    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        Path configDir = Paths.get(FMLPaths.GAMEDIR.get().toString()).resolve(event.getLevel().getServer().getWorldPath(LevelResource.ROOT));
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
