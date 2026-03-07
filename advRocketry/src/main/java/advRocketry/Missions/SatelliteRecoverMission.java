package advRocketry.Missions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class SatelliteRecoverMission extends RocketMission {

    UUID target;

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = super.serialize(registries);
        tag.putUUID("target", target);
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        super.deserialize(tag, registries);
        target = tag.getUUID("target");
    }
}
