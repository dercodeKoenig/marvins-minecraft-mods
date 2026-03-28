package advRocketry.Rocket;

import advRocketry.Rocket.RocketPrograms.*;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProgramRegistry {
    static final Map<String, Class<? extends RocketProgram>> programs = new HashMap<>();
    static final Map<Class<? extends RocketProgram>, String> programsI = new HashMap<>();

    static {
        // register known programs here
        registerProgram(ProgramNavigateToPlanetPosition.class, "ProgramNavigateToPlanetPosition");
        registerProgram(ProgramNavigateToSpaceStation.class, "ProgramNavigateToSpaceStation");
        registerProgram(ProgramMissionStartBase.class, "ProgramMissionStartBase");
        registerProgram(ProgramSatelliteDeployment.class, "ProgramSatelliteDeployment");
        registerProgram(ProgramSatelliteRecovery.class, "ProgramSatelliteRecovery");
        registerProgram(ProgramTestFlight.class, "ProgramTestFlight");
        registerProgram(ProgramAsteroidMiningMission.class, "ProgramAsteroidMiningMission");
        registerProgram(ProgramGasMiningMission.class, "ProgramGasMiningMission");
    }

    public static void registerProgram(Class<? extends RocketProgram> programClass, String id) {
        Objects.requireNonNull(programClass, "programClass");
        Objects.requireNonNull(id, "id");
        if (programs.containsKey(id)) {
            throw new IllegalArgumentException("A program with id '" + id + "' is already registered");
        }
        programs.put(id, programClass);
        programsI.put(programClass, id);
    }

    public static RocketProgram createFromNbt(CompoundTag tag) {
        if (tag == null) return null;

        if (tag.contains("noProgram")) {
            return null;
        }

        String name = tag.getString("name");
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Program tag missing 'name' field");
        }

        Class<? extends RocketProgram> clazz = programs.get(name);
        if (clazz == null) {
            throw new IllegalStateException("No registered program with id: " + name);
        }

        try {
            RocketProgram program = clazz.getDeclaredConstructor().newInstance();
            CompoundTag data = tag.contains("data") ? tag.getCompound("data") : new CompoundTag();
            program.readFromNbt(data);
            return program;
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate program: " + name, e);
        }
    }

    public static CompoundTag saveToNbt(RocketProgram program) {
        CompoundTag data = new CompoundTag();
        if (program == null) {
            data.putInt("noProgram", 1);
            return data;
        }

        String name = programsI.get(program.getClass());
        if (name == null) {
            throw new IllegalStateException("Program class not registered: " + program.getClass().getName());
        }

        data.putString("name", name);
        data.put("data", program.saveToNbt());
        return data;
    }
}
