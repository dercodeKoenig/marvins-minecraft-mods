package advRocketry.Dimension;

import ARLib.network.SimpleNetworkPacket;
import advRocketry.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class DimensionManager implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static final String SAVE_DIR = "dimensionProperties";

    // this one syncs dimension properties and creates the dimension if not exist
    public static final String packetDimensionPropertiesSync = Main.MODID + "_packetDimensionPropertiesSync";
    // this one syncs the list of dimensions so the client can remove the ones that shouldnt exist
    public static final String packetDimensionListSync = Main.MODID + "_packetDimensionListSync";

    // i split into server instance and client instance because some code might not be thread safe and could break in local world
    public static final DimensionManager INSTANCE_SERVER = new DimensionManager(false);
    public static final DimensionManager INSTANCE_CLIENT = new DimensionManager(true);

    static {
        // register rocket dimension client side - for the server it is registered on server startup
        INSTANCE_CLIENT.dimensions.put(RocketTravelDimension.dimId, new RocketTravelDimension(new DimensionProperties(), INSTANCE_CLIENT));
    }

    public final HashMap<ResourceLocation, Dimension> dimensions = new HashMap<>();

    public boolean isClientSide;

    public DimensionManager(boolean isClientSide) {
        this.isClientSide = isClientSide;
    }

    public static DimensionManager getDimensionManager(boolean isClientSide) {
        if (isClientSide) return INSTANCE_CLIENT;
        else return INSTANCE_SERVER;
    }

    public static ServerLevel getServerLevel(ResourceLocation dimensionId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
    }

    public Dimension get(ResourceLocation key) {
        if (dimensions.containsKey(key)) return dimensions.get(key);
        return null;
    }

    public void tick() {
        Iterator<Dimension> dimensionIterator = dimensions.values().iterator();
        while (dimensionIterator.hasNext()) {
            Dimension i = dimensionIterator.next();
            i.tick();
        }
    }


    public void addDimension(Dimension dimension) {
        dimensions.put(dimension.getDimensionId(), dimension);
        syncDimensionProperties(dimension);
    }

    public void syncDimensionProperties(Dimension dimension, boolean sameLevelOnly) {
        if (isClientSide) return;
        for (ServerPlayer p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (sameLevelOnly && !p.level().dimension().location().equals(dimension.getDimensionId()))
                continue;
            SyncDimensionProperties.syncDimensionPropertiesToPlayer(p, dimension);
        }
    }

    public void syncDimensionProperties(Dimension dimension) {
        syncDimensionProperties(dimension, false);
    }

    private String getSaveFile(ResourceLocation id){
        return id.getNamespace() + "_" + id.getPath() + ".json";
    }

    public void saveDimensionProperties(Path saveDir, List<DimensionProperties> properties) {
        // save current properties and if required, delete old properties to support dynamic deletion of dimensions

        // the save file name is namespace_path.json
        // if a user sets a planet config to planet1 it would still be saved as namespace_planet1 so we need to keep track of the saved filenames to remove invalid ones after save
        HashMap<ResourceLocation, String> savedFiles = new HashMap<>();

        System.out.println("[DimensionManager] saving dimension properties...");
        try {
            Files.createDirectories(saveDir);
            for (DimensionProperties i : properties) {
                Path saveFile = Path.of(String.valueOf(saveDir), getSaveFile(i.dimensionId));
                String s = new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(i);
                Files.writeString(saveFile, s);
                savedFiles.put(i.dimensionId, saveFile.getFileName().toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[DimensionManager] saved all dimension properties!");


        // remove dimension property files for dimensions that no longer exist (idk death star laser or whatever)
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(saveDir)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    String content = Files.readString(file);
                    DimensionProperties props = new Gson().fromJson(content, DimensionProperties.class);
                    // delete if the dimension does not exist
                    if (!savedFiles.containsKey(props.dimensionId)) {
                        Files.delete(file);
                        System.out.println("[DimensionManager] Deleted file for " + props.dimensionId + " because the dimension no longer exists or never existed");
                    }else{
                        if(!savedFiles.get(props.dimensionId).equals(file.getFileName().toString())){
                            Files.delete(file);
                            System.out.println("[DimensionManager] Deleted file " + file.getFileName() + " because the dimension was saved under a different name: "+savedFiles.get(props.dimensionId));
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onServerStop() {

        // unload and remove rocket travel dim before saving the other dimensions
        // this one does not need to be saved
        dimensions.remove(RocketTravelDimension.dimId);
        DynamicDimensionRegistry.from(ServerLifecycleHooks.getCurrentServer()).unloadDynamicDimension(RocketTravelDimension.dimId, (x, y) -> {
        });

        // save dimension properties
        Path saveDir = Path.of(String.valueOf(Main.worldPath), DimensionManager.SAVE_DIR);
        saveDimensionProperties(saveDir, dimensions.values().stream().map(dim -> dim.properties).toList());

        // save dimensions
        System.out.println("[DimensionManager] unloading and saving dimensions...");
        for (Dimension i : dimensions.values()) {
            DynamicDimensionRegistry.from(ServerLifecycleHooks.getCurrentServer()).unloadDynamicDimension(i.getDimensionId(), (x, y) -> {
            });
        }
        System.out.println("[DimensionManager] saved all dimensions!");

        // make sure to release everything to gc and clear for the next run
        dimensions.clear();
    }

    private DimensionProperties createPropertiesFromString(String dimensionProperties){
        Gson gson = new Gson();
        DimensionProperties propsBase = gson.fromJson(dimensionProperties, DimensionProperties.class);
        if (propsBase.type == DimensionProperties.DimensionType.PLANET) {
            return gson.fromJson(dimensionProperties, PlanetDimensionProperties.class);
        }
        if (propsBase.type == DimensionProperties.DimensionType.DUMMY) {
            return gson.fromJson(dimensionProperties, DummyDimensionProperties.class);
        }
        if (propsBase.type == DimensionProperties.DimensionType.SPACE_STATION) {
            return gson.fromJson(dimensionProperties, SpaceStationDimensionProperties.class);
        }
        return propsBase;
    }

    private void loadDimensionFromString(String dimensionProperties) {
        DimensionProperties properties = createPropertiesFromString(dimensionProperties);
        if (properties.type == DimensionProperties.DimensionType.PLANET) {
            if (dimensions.containsKey(properties.dimensionId)) {
                dimensions.get(properties.dimensionId).updateDimensionProperties(properties);
            } else {
                PlanetDimension dimension = new PlanetDimension(properties, this);
                dimensions.put(dimension.getDimensionId(), dimension);
                System.out.println("[DimensionManager] created PlanetDimension for " + dimension.getDimensionId());
            }
        }
        if (properties.type == DimensionProperties.DimensionType.DUMMY) {
            if (dimensions.containsKey(properties.dimensionId)) {
                dimensions.get(properties.dimensionId).updateDimensionProperties(properties);
            } else {
                DummyDimension dummyDimension = new DummyDimension(properties, this);
                dimensions.put(dummyDimension.getDimensionId(), dummyDimension);
                System.out.println("[DimensionManager] created DummyDimension for " + dummyDimension.getDimensionId());
            }
        }
        if (properties.type == DimensionProperties.DimensionType.SPACE_STATION) {
            if (dimensions.containsKey(properties.dimensionId)) {
                dimensions.get(properties.dimensionId).updateDimensionProperties(properties);
            } else {
                SpaceStationDimension spaceStationDimension = new SpaceStationDimension(properties, this);
                dimensions.put(spaceStationDimension.getDimensionId(), spaceStationDimension);
                System.out.println("[DimensionManager] created Space Station for " + spaceStationDimension.getDimensionId() + ":" + spaceStationDimension.getName());
            }
        }
    }

    private void loadDimensionsFromDirectory(Path directory) {
        if (!Files.exists(directory)) {
            System.out.println("[DimensionManager] Error: Directory does not exist: " + directory);
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    String content;
                    try {
                        content = Files.readString(file);
                    } catch (Exception e) {
                        System.err.println("[DimensionManager] Failed to read file: " + file + " (" + e.getMessage() + ")");
                        e.printStackTrace();
                        continue;
                    }
                    loadDimensionFromString(content);
                    System.out.println("[DimensionManager] Loaded dimension from file: " + file.getFileName());
                }
            }
        } catch (Exception e) {
            System.err.println("[DimensionManager] Error reading directory: " + directory + " (" + e.getMessage() + ")");
            e.printStackTrace();
        }
    }

    public void reloadPropertiesFromConfig(){
        System.out.println("[DimensionManager] Reloading dimension properties from main config - this will NOT remove any dimension already defined in your local world folder!");
        Path defaultDir = Path.of(String.valueOf(Main.myConfigDir), DimensionManager.SAVE_DIR);
        loadDimensionsFromDirectory(defaultDir);
        for(Dimension d : dimensions.values()){
            syncDimensionProperties(d);
        }
    }

    public void resetDefaultGalaxy(Path defaultDir){
        System.out.println("[DimensionManager] creating default galaxy...");
        List<String> defaultGalaxy = DefaultGalaxy.createDefaultGalaxy();
        List<DimensionProperties> properties = new LinkedList<>();
        for (String s : defaultGalaxy) {
            properties.add(createPropertiesFromString(s));
        }
        saveDimensionProperties(defaultDir, properties);
    }

    public void onServerStart() {

        if (!dimensions.isEmpty()) throw new AssertionError();

        boolean debug_forceDefaultGalaxy = true;

        Path worldDir = Path.of(String.valueOf(Main.worldPath), DimensionManager.SAVE_DIR);
        Path defaultDir = Path.of(String.valueOf(Main.myConfigDir), DimensionManager.SAVE_DIR);

        // init default galaxy
        if(!Files.exists(defaultDir) || debug_forceDefaultGalaxy){
            resetDefaultGalaxy(defaultDir);
        }

        if (Files.exists(worldDir)) {
            System.out.println("[DimensionManager] Loading dimensions from world path...");
            loadDimensionsFromDirectory(worldDir);
        } else if (Files.exists(defaultDir)) {
            System.out.println("[DimensionManager] Loading dimensions from default config...");
            loadDimensionsFromDirectory(defaultDir);
        }

        // add the rocket travel dimension
        dimensions.put(RocketTravelDimension.dimId, new RocketTravelDimension(new DimensionProperties(), this));

    }


    public static class SyncDimensionProperties implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

        public static void syncDimensionPropertiesToPlayer(ServerPlayer player, Dimension dimension) {
            PacketDistributor.sendToPlayer(player,
                    new SimpleNetworkPacket(
                            packetDimensionPropertiesSync,
                            new Gson().toJson(dimension.properties)
                    )
            );
        }

        public void readClient(String props) {
            //System.out.println(props);
            INSTANCE_CLIENT.loadDimensionFromString(props);
        }
    }

    public static class SyncDimensionList implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

        public static void syncDimensionListToPlayer(ServerPlayer player) {
            DimensionList list = new DimensionList(new ArrayList<>(INSTANCE_SERVER.dimensions.keySet()));
            String s = new Gson().toJson(list);
            PacketDistributor.sendToPlayer(player, new SimpleNetworkPacket(packetDimensionListSync, s));
        }

        public void readClient(String dimensionList) {
            DimensionList list = new Gson().fromJson(dimensionList, DimensionList.class);
            HashSet<ResourceLocation> set = new HashSet<>(list.dimensionIds);
            for (ResourceLocation i : new ArrayList<>(INSTANCE_CLIENT.dimensions.keySet())) {
                if (!set.contains(i)) {
                    INSTANCE_CLIENT.dimensions.remove(i);
                    System.out.println("client removed dimension: " + i);
                }
            }
        }

        static class DimensionList { // wrapped for gson to parse so it has the type of the list
            ArrayList<ResourceLocation> dimensionIds;

            public DimensionList(ArrayList<ResourceLocation> dimensionIds) {
                this.dimensionIds = dimensionIds;
            }
        }
    }
}
