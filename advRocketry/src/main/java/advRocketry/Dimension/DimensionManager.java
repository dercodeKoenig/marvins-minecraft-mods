package advRocketry.Dimension;

import ARLib.network.SimpleNetworkPacket;
import advRocketry.Main;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.api.PlayerRemover;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DimensionManager {
    public static final String saveFile = "galaxy.json";

    public static DimensionManager INSTANCE = new DimensionManager();

    public HashMap<ResourceLocation, Dimension> dimensions = new HashMap<>();

    public DimensionManager() {
        new DimensionClientSync();
    }

    public static Dimension get(ResourceLocation key) {
        if (INSTANCE.dimensions.containsKey(key)) return INSTANCE.dimensions.get(key);
        if(key.equals(SpaceTravelManager.dimId))return SpaceTravelManager.rocketTravelDimension;
        return null;
    }

    public static void serverTick(ServerTickEvent.Post event) {
        Iterator<Dimension> dimensionIterator = INSTANCE.dimensions.values().iterator();
        while (dimensionIterator.hasNext()){
            Dimension i = dimensionIterator.next();
            i.serverTick(event);
        }
        if (GlobalTime.getGlobalTime() % (20 * 60) == 0) {
            DimensionClientSync.syncDimensions();
        }
    }

    public static void clientTick(ClientTickEvent.Post event) {
        //if(true)return;
        Iterator<Dimension> dimensionIterator = INSTANCE.dimensions.values().iterator();
        while (dimensionIterator.hasNext()){
            Dimension i = dimensionIterator.next();
            i.clientOnly.clientTick();
        }
    }

    public static ServerLevel getServerLevel(MinecraftServer server, ResourceLocation dimensionId) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
    }

    public static String savePropertiesToString() {
        ArrayList<DimensionProperties> allProperties = new ArrayList<>();
        for (Dimension i : INSTANCE.dimensions.values()) {
            allProperties.add(i.properties);
        }
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(allProperties);
        return json;
    }

    public static void save() {
        System.out.println("saving all dimension properties...");
        Path saveFile = Path.of(String.valueOf(Main.worldPath), DimensionManager.saveFile);
        try {
            Files.writeString(saveFile, savePropertiesToString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("saved all dimension properties!");
        System.out.println("unloading and saving dimensions...");
        for (Dimension i : INSTANCE.dimensions.values()) {
            DynamicDimensionRegistry.from(ServerLifecycleHooks.getCurrentServer()).unloadDynamicDimension(i.getDimensionId(), PlayerRemover.DEFAULT);
        }
        System.out.println("saved all dimensions!");

    }

    private static void createDimensionsFromString(String dimensionProperties) {
        INSTANCE.dimensions.clear();
        Type listType = new TypeToken<List<DimensionProperties>>() {
        }.getType();
        List<DimensionProperties> galaxy = new Gson().fromJson(dimensionProperties, listType);
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
        createDimensionsFromString(defaultGalaxy);

    }


    static class DimensionClientSync implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

        public static String packetSyncId = Main.MODID + "_dimensionManager";

        public DimensionClientSync() {
            SimpleNetworkPacket.registerReceiver(packetSyncId, this);
        }

        public static void syncDimensions(){
            PacketDistributor.sendToAllPlayers(
                    new SimpleNetworkPacket(
                            DimensionClientSync.packetSyncId,
                            savePropertiesToString()
                    )
            );
        }

        public void readClient(String data) {
            Type listType = new TypeToken<List<DimensionProperties>>() {
            }.getType();
            List<DimensionProperties> newGalaxyConfig = new Gson().fromJson(data, listType);
            for (DimensionProperties i : newGalaxyConfig) {
                if (INSTANCE.dimensions.containsKey(i.dimensionId)) {
                    INSTANCE.dimensions.get(i.dimensionId).properties = i;
                } else {
                    Dimension dimension = new Dimension(i);
                    INSTANCE.dimensions.put(i.dimensionId, dimension);
                    System.out.println("client created new dimension for "+i.dimensionId);
                }
            }
            List<ResourceLocation> toDelete = new ArrayList<>();
            for (Dimension d : INSTANCE.dimensions.values()) {
                boolean stillExists = false;
                for (DimensionProperties i : newGalaxyConfig) {
                    if (i.dimensionId.equals(d.getDimensionId())) {
                        stillExists = true;
                        break;
                    }
                }
                if (!stillExists) {
                    toDelete.add(d.getDimensionId());
                }
            }
            for (ResourceLocation i : toDelete) {
                INSTANCE.dimensions.remove(i);
                System.out.println("client removed dimension for "+i);
            }
        }
    }
}
