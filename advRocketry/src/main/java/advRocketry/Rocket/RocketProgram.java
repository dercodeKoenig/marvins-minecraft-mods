package advRocketry.Rocket;

import net.minecraft.nbt.CompoundTag;

public interface RocketProgram {

    void run(EntityRocket rocket);

    void readFromNbt(CompoundTag nbt);

    CompoundTag saveToNbt();

}
