package advRocketry.Missions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// basically copied from rocket program registry
public class MissionRegistry {
    static final Map<String, Class<? extends RocketMission>> missions = new HashMap<>();
    static final Map<Class<? extends RocketMission>, String> missionsI = new HashMap<>();

    static {
        // register known Missions here
        registerMission(RocketMission.class, "MissionBaseClass");
        registerMission(SatelliteDeploymentMission.class, "SatelliteDeploymentMission");
        registerMission(SatelliteRecoverMission.class, "SatelliteRecoverMission");
    }

    public static void registerMission(Class<? extends RocketMission> clazz, String id) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(id, "id");
        if (missions.containsKey(id)) {
            throw new IllegalArgumentException("A program with id '" + id + "' is already registered");
        }
        missions.put(id, clazz);
        missionsI.put(clazz, id);
    }

    // create from NBT
    public static RocketMission createFromNbt(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag == null) return null;
        if (!tag.contains("name") || !tag.contains("data")) return null;
        String name = tag.getString("name");
        Class<? extends RocketMission> clazz = missions.get(name);
        if (clazz == null) return null;
        try {
            RocketMission mission = clazz.getDeclaredConstructor().newInstance();
            CompoundTag data = tag.getCompound("data");
            mission.deserialize(data, registries);
            return mission;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Failed to instantiate mission: " + name, e);
        }
    }

    public static CompoundTag saveToNbt(RocketMission mission, HolderLookup.Provider registries) {
        CompoundTag data = new CompoundTag();
        String name = missionsI.get(mission.getClass());
        data.putString("name", name);
        data.put("data", mission.serialize(registries));
        return data;
    }
}