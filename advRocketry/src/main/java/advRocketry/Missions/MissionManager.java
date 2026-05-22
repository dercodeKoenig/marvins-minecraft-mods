package advRocketry.Missions;

import advRocketry.GlobalTime;
import advRocketry.Main;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class MissionManager {
    public static String saveFile = Main.MODID + "_missions.json";

    private static HashMap<UUID, RocketMission> missions = new HashMap<>();

    public static void addMission(UUID id, RocketMission mission) {
        missions.put(id, mission);
        saveMissions();
    }

    public static RocketMission getMission(UUID id) {
        return missions.get(id);
    }

    public static void serverTick() {
        // this loop might not be most efficient but let's be real, will you have 50+ missions running at once?
        // i know i could sort them by complete time and so on... but i don't care
        for (UUID missionId : new ArrayList<>(missions.keySet())) {
            if (GlobalTime.getGlobalTime() > missions.get(missionId).completeTime) {
                // mission is complete!
                missions.get(missionId).completeMission();
                missions.remove(missionId);
                saveMissions();
            }
        }
    }


    public static void onServerStart() {
        if (!missions.isEmpty()) throw new AssertionError();
        try {
            String save = Files.readString(Path.of(Main.worldPath.toString(), saveFile));
            CompoundTag tag = TagParser.parseTag(save);
            for (String key : tag.getAllKeys()) {
                CompoundTag satelliteTag = tag.getCompound(key);
                RocketMission mission = MissionRegistry.createFromNbt(satelliteTag, ServerLifecycleHooks.getCurrentServer().registryAccess());
                missions.put(mission.missionID, mission);
            }
            System.out.println("[MissionManager] loaded " + missions.size() + " missions");
        } catch (IOException e) {
            // maybe new world...
            System.out.println("[MissionManager] could not load satellite save file");
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveMissions() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        CompoundTag tag = new CompoundTag();
        for (UUID key : missions.keySet()) {
            tag.put(key.toString(), MissionRegistry.saveToNbt(missions.get(key), server.registryAccess()));
        }
        try {
            Files.writeString(Path.of(Main.worldPath.toString(), saveFile), tag.toString());
            System.out.println("[MissionManager] saved " + missions.size() + " missions");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void onServerStop() {
        saveMissions();
        missions.clear();
    }
}
