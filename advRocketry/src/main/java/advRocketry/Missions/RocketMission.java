package advRocketry.Missions;

import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

import static advRocketry.Registry.GeneralRegistry.ENTITY_ROCKET;

public class RocketMission {
    public long completeTime;
    CompoundTag rocketTag;
    UUID missionID;
    ResourceLocation homeLevelId;
    // last launch pos is in rocket save data

    public void completeMission() {

    }

    public void startMission(EntityRocket rocket, long completeTime, UUID missionID) {
        this.homeLevelId = rocket.level().dimension().location();
        rocket
        this.missionID = missionID;
        this.completeTime = completeTime;
        rocketTag = new CompoundTag();
        rocket.addAdditionalSaveData(rocketTag);
        rocket.discard();
        MissionManager.missions.put(this.missionID, this);
    }

    public EntityRocket restoreRocket(Level level, BlockPos pos) {
        EntityRocket rocket = ENTITY_ROCKET.get().create(level);
        rocket.readAdditionalSaveData(rocketTag);
        rocket.setPos(pos.getCenter());
        rocket.setDeltaMovement(0,0,0);
        level.addFreshEntity(rocket);
        return rocket;
    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("rocket", rocketTag);
        tag.putLong("completeTime", completeTime);
        tag.putUUID("missionID", missionID);
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        rocketTag = tag.getCompound("rocket");
        completeTime = tag.getLong("completeTime");
        missionID = tag.getUUID("missionID");
    }
}
