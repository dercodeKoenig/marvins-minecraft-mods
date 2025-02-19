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

public class TownHallNames {
    private static HashMap<String, HashMap<BlockPos, String>> townhallNamesStatic = new HashMap<>();
    private static boolean hasChanges = false;

    public static String getName(Level level, BlockPos pos) {
      verifyExist(level,null);
        return townhallNamesStatic.get(DimensionUtils.getLevelId(level)).get(pos);
    }

    public static void setChanged(){hasChanges = true;}

    public static void verifyExist(Level l , BlockPos p){
        if (townhallNamesStatic.get(DimensionUtils.getLevelId(l)) == null) {
            townhallNamesStatic.put(DimensionUtils.getLevelId(l), new HashMap<>());
        }
        if(p != null){
            if (townhallNamesStatic.get(DimensionUtils.getLevelId(l)).get(p) == null) {
                townhallNamesStatic.get(DimensionUtils.getLevelId(l)).put(p, "");
            }
        }
    }

    public static void setName(Level l, BlockPos p, String name) {
        verifyExist(l,null);
        townhallNamesStatic.get(DimensionUtils.getLevelId(l)).put(p, name);
        setChanged();
    }

    public static void removeEntry(Level l, BlockPos p) {
        verifyExist(l,null);
        townhallNamesStatic.get(DimensionUtils.getLevelId(l)).remove(p);
        setChanged();
    }

    public BlockPos pos;
    public String name;

    public static HashMap<String, List<TownHallNames>> getFromStaticMap() {
        HashMap<String, List<TownHallNames>> map = new HashMap<>();
        for (String s : townhallNamesStatic.keySet()) {
            map.put(s, new ArrayList<>());
            for (BlockPos p : townhallNamesStatic.get(s).keySet()) {
                TownHallNames i = new TownHallNames();
                i.pos = p;
                i.name = townhallNamesStatic.get(s).get(p);
                map.get(s).add(i);
            }
        }
        return map;
    }

    public static void createStaticMap(HashMap<String, List<TownHallNames>> map) {
        townhallNamesStatic = new HashMap<>();
        for (Level l : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            verifyExist(l,null);
            if(map.containsKey(DimensionUtils.getLevelId(l))){
                for(TownHallNames i : map.get(DimensionUtils.getLevelId(l))){
                    if(i!=null)
                        townhallNamesStatic.get(DimensionUtils.getLevelId(l)).put(i.pos,i.name);
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
        Type mapType = new TypeToken<HashMap<String, List<TownHallNames>>>() {
        }.getType();
        HashMap<String, List<TownHallNames>> map = gson.fromJson(json, mapType);
        createStaticMap(map);
    }

    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel().isClientSide()) return;
        if(hasChanges) {
            Path configDir = Paths.get(FMLPaths.GAMEDIR.get().toString()).resolve(event.getLevel().getServer().getWorldPath(LevelResource.ROOT));
            String filename = "townHallNames";
            Path filePath = configDir.resolve(filename);
            try {
                if (!Files.exists(filePath)) {
                    Files.createFile(filePath);
                }
                Files.writeString(filePath, TownHallNames.toJson());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        hasChanges = false;
    }

    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        Path configDir = Paths.get(FMLPaths.GAMEDIR.get().toString()).resolve(event.getLevel().getServer().getWorldPath(LevelResource.ROOT));
        String filename = "townHallNames";
        Path filePath = configDir.resolve(filename);
        if (Files.exists(filePath)) {
            try {
                String s = Files.readString(filePath);
                TownHallNames.fromJson(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JsonSyntaxException j) {
                System.err.println(j);
            }
        }
    }
}
