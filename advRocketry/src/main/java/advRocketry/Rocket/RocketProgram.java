package advRocketry.Rocket;

import advRocketry.Rocket.RocketUtils.ProgramNavigateToPlanetPosition;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public interface RocketProgram {
    class programList {
        static HashMap<String, Class<?>> programs = new HashMap<>();
        static {
            programs.put(ProgramNavigateToPlanetPosition.id, ProgramNavigateToPlanetPosition.class);
        }
    }

    void run(EntityRocket rocket);

    void readFromNbt(CompoundTag nbt);

    CompoundTag saveToNbt();

    static RocketProgram createFromNbt(CompoundTag tag) {
        if(tag.contains("noProgram")){
            return null;
        }
        String name = tag.getString("name");
        RocketProgram program = null;
        try {
            program = (RocketProgram) programList.programs.get(name).getDeclaredConstructor().newInstance();
            program.readFromNbt(tag.getCompound("data"));
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return program;
    }

    static CompoundTag saveToNbt(RocketProgram program){
        CompoundTag data = new CompoundTag();
        if(program == null) data.putInt("noProgram", 0);
        else {
            String name = null;
            for (String entry : programList.programs.keySet()) {
                Class<?> c = programList.programs.get(entry);
                if (c.isInstance(program)) {
                    name = entry;
                    break;
                }
            }
            data.putString("name", name);
            data.put("data", program.saveToNbt());
        }
        return data;
    }
}
