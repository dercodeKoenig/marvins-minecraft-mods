package advRocketry.Satellites;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// basically copied from rocket program registry
// allows to save & load a subclass of the base satellite class
public class SatelliteRegistry {
    static final Map<String, Class<? extends Satellite>> satellites = new HashMap<>();
    static final Map<Class<? extends Satellite>, String> satellitesI = new HashMap<>();

    static {
        // register known Satellites here
        registerSatellite(Satellite.class, "SatelliteBaseClass");
    }

    static void registerSatellite(Class<? extends Satellite> satelliteClass, String id) {
        Objects.requireNonNull(satelliteClass, "satelliteClass");
        Objects.requireNonNull(id, "id");
        if (satellites.containsKey(id)) {
            throw new IllegalArgumentException("A program with id '" + id + "' is already registered");
        }
        satellites.put(id, satelliteClass);
        satellitesI.put(satelliteClass, id);
    }

    // create from NBT
    public static Satellite createFromNbt(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag == null) return null;
        if (!tag.contains("name") || !tag.contains("data")) return null;
        String name = tag.getString("name");
        Class<? extends Satellite> clazz = satellites.get(name);
        if (clazz == null) return null;
        try {
            Satellite satellite = clazz.getDeclaredConstructor().newInstance();
            CompoundTag data = tag.getCompound("data");
            satellite.deserialize(data, registries);
            return satellite;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Failed to instantiate satellite: " + name, e);
        }
    }

    public static CompoundTag saveToNbt(Satellite satellite, HolderLookup.Provider registries) {
        CompoundTag data = new CompoundTag();
        String name = satellitesI.get(satellite.getClass());
        data.putString("name", name);
        data.put("data", satellite.serialize(registries));
        return data;
    }
}