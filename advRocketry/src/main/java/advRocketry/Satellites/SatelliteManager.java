package advRocketry.Satellites;

import advRocketry.Main;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerLinks;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

// unlike DimensionManager, this should only exist server side
public class SatelliteManager {
    public static String saveFile = Main.MODID + "_satellites.json";
    private static boolean requiresCacheUpdate = true;
    private static HashMap<UUID, Satellite> satellites = new HashMap<>();

    private static ArrayList<Satellite> satellitesArray = new ArrayList<>();

    public static Satellite removeSatellite(UUID satelliteId) {
        Satellite satellite = satellites.remove(satelliteId);
        requiresCacheUpdate = true;
        return satellite;
    }

    public static Satellite getSatellite(UUID satelliteId) {
        return satellites.get(satelliteId);
    }

    public static void addTickingSatellite(Satellite satellite, ResourceLocation target) {
        if (target == null) {
            System.out.println("[SatelliteManager] a satellite had no target specified and will not be added: " + satellite.uuid);
            return;
        }
        System.out.println("[SatelliteManager] satellite deployed: " + satellite.getName() + ":" + satellite.uuid + " at " + target);
        satellite.onDeploymentStart(target);
        satellites.put(satellite.uuid, satellite);
        requiresCacheUpdate = true;
    }

    public static ArrayList<Satellite> getSatellitesArray() {
        if (requiresCacheUpdate) {
            requiresCacheUpdate = false;
            satellitesArray = new ArrayList<>(satellites.values());
        }
        return satellitesArray;
    }

    public static void serverTick() {
        for (Satellite satellite : getSatellitesArray()) {
            satellite.tick();
        }
    }

    public static void onServerStart() {
        satellites.clear();
        try {
            String save = Files.readString(Path.of(Main.worldPath.toString(), saveFile));
            CompoundTag tag = TagParser.parseTag(save);
            for (String key : tag.getAllKeys()) {
                CompoundTag satelliteTag = tag.getCompound(key);
                Satellite satellite = SatelliteRegistry.createFromNbt(satelliteTag, ServerLifecycleHooks.getCurrentServer().registryAccess());
                addTickingSatellite(satellite, satellite.parentDimensionId);
            }
            System.out.println("[SatelliteManager] loaded " + satellites.size() + " satellites");
        } catch (IOException e) {
            // maybe new world...
            System.out.println("[SatelliteManager] could not load satellite save file");
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void onServerStop() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        CompoundTag tag = new CompoundTag();
        for (UUID key : satellites.keySet()) {
            tag.put(key.toString(), SatelliteRegistry.saveToNbt(satellites.get(key), server.registryAccess()));
        }
        try {
            Files.writeString(Path.of(Main.worldPath.toString(), saveFile), tag.toString());
            System.out.println("[SatelliteManager] saved " + satellites.size() + " satellites");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
