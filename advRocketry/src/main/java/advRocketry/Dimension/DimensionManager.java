package advRocketry.Dimension;

import ARLib.network.SimpleNetworkPacket;
import advRocketry.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import dev.galacticraft.dynamicdimensions.api.PlayerRemover;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DimensionManager implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static final String saveDir = "dimensionProperties";

    public static final String packetSyncId = Main.MODID + "_dimensionManager";

    public static final DimensionManager INSTANCE = new DimensionManager();

    public HashMap<ResourceLocation, Dimension> dimensions = new HashMap<>();

    public DimensionManager() {
        SimpleNetworkPacket.registerReceiver(packetSyncId, this);
    }

    public static Dimension get(ResourceLocation key) {
        if (INSTANCE.dimensions.containsKey(key)) return INSTANCE.dimensions.get(key);
        if (key.equals(RocketTravelDimension.dimId)) return RocketTravelDimension.INSTANCE;
        return null;
    }

    public static void serverTick(ServerTickEvent.Post event) {
        Iterator<Dimension> dimensionIterator = INSTANCE.dimensions.values().iterator();
        while (dimensionIterator.hasNext()) {
            Dimension i = dimensionIterator.next();
            i.serverTick(event);
        }
    }

    public static void clientTick(ClientTickEvent.Post event) {
        Iterator<Dimension> dimensionIterator = INSTANCE.dimensions.values().iterator();
        while (dimensionIterator.hasNext()) {
            Dimension i = dimensionIterator.next();
            i.clientTick();
        }
    }

    public static ServerLevel getServerLevel(MinecraftServer server, ResourceLocation dimensionId) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
    }


    public static void save() {
        System.out.println("[DimensionManager]  saving all dimension properties...");
        Path saveDir = Path.of(String.valueOf(Main.worldPath), DimensionManager.saveDir);
        try {
            Files.createDirectories(saveDir);
            for (Dimension i : INSTANCE.dimensions.values()) {
                Path saveFile = Path.of(String.valueOf(saveDir), i.getDimensionId().getNamespace() + "_" + i.getDimensionId().getPath());
                String s = new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(i.properties);
                Files.writeString(saveFile, s, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[DimensionManager] saved all dimension properties!");
        System.out.println("[DimensionManager] unloading and saving dimensions...");
        for (Dimension i : INSTANCE.dimensions.values()) {
            DynamicDimensionRegistry.from(ServerLifecycleHooks.getCurrentServer()).unloadDynamicDimension(i.getDimensionId(), PlayerRemover.DEFAULT);
        }
        System.out.println("[DimensionManager] saved all dimensions!");

    }

    private static void loadDimensionFromString(String dimensionProperties) {
        Gson gson = new Gson();
        DimensionProperties propsBase = gson.fromJson(dimensionProperties, DimensionProperties.class);
        if (propsBase.type == DimensionProperties.DimensionType.PLANET) {
            PlanetDimensionProperties planetProps = gson.fromJson(dimensionProperties, PlanetDimensionProperties.class);
            if (INSTANCE.dimensions.containsKey(planetProps.dimensionId)) {
                // update
                INSTANCE.dimensions.get(planetProps.dimensionId).properties = planetProps;
            } else {
                // create
                PlanetDimension dimension = new PlanetDimension(planetProps);
                INSTANCE.dimensions.put(dimension.getDimensionId(), dimension);
                System.out.println("[DimensionManager] created PlanetDimension for " + dimension.getDimensionId());
            }
        }
    }

    private static void loadDimensionsFromDirectory(Path directory) {
        if (!Files.exists(directory)) {
            System.out.println("[DimensionManager] Directory does not exist: " + directory);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    try {
                        String content = Files.readString(file);
                        loadDimensionFromString(content);
                        System.out.println("[DimensionManager] Loaded dimension from file: " + file.getFileName());
                    } catch (Exception e) {
                        System.err.println("[DimensionManager] Failed to read file: " + file + " (" + e.getMessage() + ")");
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DimensionManager] Error reading directory: " + directory + " (" + e.getMessage() + ")");
            e.printStackTrace();
        }
    }


    public static void init() {
        INSTANCE.dimensions = new HashMap<>();

        Path worldDir = Path.of(String.valueOf(Main.worldPath), DimensionManager.saveDir);
        Path defaultDir = Path.of(String.valueOf(Main.myConfigDir), DimensionManager.saveDir);

        if (Files.exists(worldDir)) {
            System.out.println("[DimensionManager] Loading dimensions from world path...");
            loadDimensionsFromDirectory(worldDir);
        } else if (Files.exists(defaultDir)) {
            System.out.println("[DimensionManager] Loading dimensions from default config...");
            loadDimensionsFromDirectory(defaultDir);
        } else {
            System.out.println("[DimensionManager] No saved dimensions found, creating default galaxy...");
            List<String> defaultGalaxy = DefaultGalaxy.createDefaultGalaxy();
            for (String s : defaultGalaxy) {
                loadDimensionFromString(s);
            }
        }
    }


    public static void syncDimension(ServerPlayer player, Dimension dimension) {
        PacketDistributor.sendToPlayer(player,
                new SimpleNetworkPacket(
                        packetSyncId,
                        new Gson().toJson(dimension.properties)
                )
        );
    }

    public void readClient(String props) {
        loadDimensionFromString(props);
    }
}
