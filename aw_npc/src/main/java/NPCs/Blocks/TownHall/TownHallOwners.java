package NPCs.Blocks.TownHall;

import ARLib.utils.DimensionUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TownHallOwners {
    private static HashMap<String, HashMap<BlockPos, Set<String>>> ownerNamesStatic = new HashMap<>();
    private static boolean hasChanges = false;

    public static Set<String> getOwners(Level level, BlockPos pos) {
        verifyExist(level, null);
        Set<String> ret = ownerNamesStatic.get(DimensionUtils.getLevelId(level)).get(pos);
        if (ret == null) ret = new HashSet<>();
        return ret;
    }

    public static void setChanged(){hasChanges = true;}

    public static void verifyExist(Level l , BlockPos p){
        if (ownerNamesStatic.get(DimensionUtils.getLevelId(l)) == null) {
            ownerNamesStatic.put(DimensionUtils.getLevelId(l), new HashMap<>());
        }
        if(p != null){
            if (ownerNamesStatic.get(DimensionUtils.getLevelId(l)).get(p) == null) {
                ownerNamesStatic.get(DimensionUtils.getLevelId(l)).put(p, new HashSet<>());
            }
        }
    }

    public static void setOwners(Level l, BlockPos p, Set<String> owners) {
        verifyExist(l,null);
        ownerNamesStatic.get(DimensionUtils.getLevelId(l)).put(p, owners);
        setChanged();
    }
    public static void addOwner(Level l, BlockPos p, String owner) {
        verifyExist(l,p);
        ownerNamesStatic.get(DimensionUtils.getLevelId(l)).get(p).add(owner);
        setChanged();
    }
    public static void removeOwner(Level l, BlockPos p, String owner) {
        verifyExist(l,p);
        ownerNamesStatic.get(DimensionUtils.getLevelId(l)).get(p).remove(owner);
        setChanged();
    }

    public static void removeEntry(Level l, BlockPos p) {
        verifyExist(l,null);
        ownerNamesStatic.get(DimensionUtils.getLevelId(l)).remove(p);
        setChanged();
    }
    public static HashMap<BlockPos,Set<String>> getEntries(Level l) {
        verifyExist(l,null);
        return ownerNamesStatic.get(DimensionUtils.getLevelId(l));
    }

    public BlockPos pos;
    public Set<String> owners;

    public static HashMap<String, List<TownHallOwners>> getFromStaticMap() {
        HashMap<String, List<TownHallOwners>> map = new HashMap<>();
        for (String s : ownerNamesStatic.keySet()) {
            map.put(s, new ArrayList<>());
            for (BlockPos p : ownerNamesStatic.get(s).keySet()) {
                TownHallOwners i = new TownHallOwners();
                i.pos = p;
                i.owners = ownerNamesStatic.get(s).get(p);
                map.get(s).add(i);
            }
        }
        return map;
    }

    public static void createStaticMap(HashMap<String, List<TownHallOwners>> map) {
        ownerNamesStatic = new HashMap<>();
        for (Level l : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            if (!ownerNamesStatic.containsKey(DimensionUtils.getLevelId(l))) {
                ownerNamesStatic.put(DimensionUtils.getLevelId(l), new HashMap<>());
            }
            if(map.containsKey(DimensionUtils.getLevelId(l))){
                for(TownHallOwners i : map.get(DimensionUtils.getLevelId(l))){
                    if(i!=null)
                        ownerNamesStatic.get(DimensionUtils.getLevelId(l)).put(i.pos,i.owners);
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
        Type mapType = new TypeToken<HashMap<String, List<TownHallOwners>>>() {
        }.getType();
        HashMap<String, List<TownHallOwners>> map = gson.fromJson(json, mapType);
        createStaticMap(map);
    }

    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel().isClientSide()) return;
        if(hasChanges) {
            Path configDir = Paths.get(FMLPaths.GAMEDIR.get().toString()).resolve(event.getLevel().getServer().getWorldPath(LevelResource.ROOT));
            String filename = "townHallOwners";
            Path filePath = configDir.resolve(filename);
            try {
                if (!Files.exists(filePath)) {
                    Files.createFile(filePath);
                }
                Files.writeString(filePath, TownHallOwners.toJson());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        hasChanges = false;
    }

    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        Path configDir = Paths.get(FMLPaths.GAMEDIR.get().toString()).resolve(event.getLevel().getServer().getWorldPath(LevelResource.ROOT));
        String filename = "townHallOwners";
        Path filePath = configDir.resolve(filename);
        if (Files.exists(filePath)) {
            try {
                String s = Files.readString(filePath);
                TownHallOwners.fromJson(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JsonSyntaxException j) {
                System.err.println(j);
            }
        }
    }
}
