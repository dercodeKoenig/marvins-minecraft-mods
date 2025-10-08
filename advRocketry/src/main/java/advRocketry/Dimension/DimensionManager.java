package advRocketry.Dimension;

import advRocketry.Main;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DimensionManager {
    public static final String saveFile = "galaxy.json";

    public static DimensionManager INSTANCE = new DimensionManager();

    public HashMap<ResourceLocation, Dimension> dimensions = new HashMap<>();
    public HashMap<ResourceLocation, Dimension> spaceStations = new HashMap<>();
    public SpaceDimension spaceDim;

    public DimensionManager() {

    }

    public static IAdvRocketryDimension get(ResourceLocation key) {
        if (INSTANCE.dimensions.containsKey(key)) return INSTANCE.dimensions.get(key);
        if (INSTANCE.spaceStations.containsKey(key)) return INSTANCE.dimensions.get(key);
        if(key.equals(SpaceDimension.spaceDimId)) return INSTANCE.spaceDim;
        return null;
    }

    public static void serverTick(ServerTickEvent.Post event) {
        //if(true)return;
        for (Dimension i : INSTANCE.dimensions.values()) {
            i.serverTick(event);
        }
    }

    public static void clientTick(ClientTickEvent.Post event) {
        //if(true)return;
        for (Dimension i : INSTANCE.dimensions.values()) {
            i.clientOnly.clientTick();
        }
    }

    public static ServerLevel getServerLevel(MinecraftServer server, ResourceLocation dimensionId) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
    }

    public static void save() {
        System.out.println("saving all dimension properties...");
        ArrayList<DimensionProperties> allProperties = new ArrayList<>();
        for (Dimension i : INSTANCE.dimensions.values()) {
            allProperties.add(i.properties);
        }
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(allProperties);
        Path saveFile = Path.of(String.valueOf(Main.worldPath), DimensionManager.saveFile);
        try {
            Files.writeString(saveFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("saved all dimension properties!");
    }

    private static void loadFromString(String galaxyString) {
        INSTANCE.dimensions.clear();
        Type listType = new TypeToken<List<DimensionProperties>>() {
        }.getType();
        List<DimensionProperties> galaxy = new Gson().fromJson(galaxyString, listType);
        for (DimensionProperties i : galaxy) {
            Dimension dimension = new Dimension(i);
            INSTANCE.dimensions.put(i.dimensionId, dimension);
        }
        System.out.println("galaxy loaded with " + galaxy.size() + " objects");
    }

    public static void init() {
        Path saveFile = Path.of(String.valueOf(Main.worldPath), DimensionManager.saveFile);

        Path defaultPlanetDef = Path.of(String.valueOf(Main.myConfigDir), DimensionManager.saveFile);
/*

        try {
            loadFromString(Files.readString(saveFile));
            return;
        } catch (IOException e) {
            System.out.println("no galaxy definition found in world path");
        }

        try {
            loadFromString(Files.readString(defaultPlanetDef));
            return;
        } catch (IOException e) {
            System.out.println("no galaxy definition found in default config");
        }
 */

        String defaultGalaxy = DefaultGalaxy.createDefaultGalaxy();
        System.out.println("default galaxy created");
        try {
            Files.writeString(defaultPlanetDef, defaultGalaxy, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("default galaxy saved in config dir");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loadFromString(defaultGalaxy);


        // create space dimension
        INSTANCE.spaceDim = new SpaceDimension();
    }
}
