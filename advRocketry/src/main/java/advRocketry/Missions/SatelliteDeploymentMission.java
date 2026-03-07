package advRocketry.Missions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.util.UUID;

public class SatelliteDeploymentMission extends RocketMission {

    ResourceLocation target;

    public void completeMission() {

    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = super.serialize(registries);
        tag.putString("target", target.toString());
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        super.deserialize(tag, registries);
        target = ResourceLocation.parse(tag.getString("target"));
    }
}
