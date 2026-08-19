package advRocketry.Rocket.RocketPrograms;

import advRocketry.GlobalTime;
import advRocketry.Missions.SatelliteRecoverMission;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class ProgramSatelliteRecovery extends ProgramMissionStartBase {
    UUID targetSatellite;
    long missionDuration;

    public ProgramSatelliteRecovery() {

    }

    public ProgramSatelliteRecovery(EntityRocket rocket, UUID targetSatellite, long missionDuration, ResourceLocation returnLevel, BlockPos returnPos, UUID missionId) {
        super(rocket, returnLevel, returnPos, missionId);
        this.targetSatellite = targetSatellite;
        this.missionDuration = missionDuration;
    }

    public void startMission(EntityRocket rocket) {
        SatelliteRecoverMission mission = new SatelliteRecoverMission();
        mission.setTarget(targetSatellite);
        mission.startMission(rocket, GlobalTime.getGlobalTime() + missionDuration, missionId, returnLevel, returnPos);
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        super.readFromNbt(nbt);
        if (nbt.contains("targetSatellite"))
            targetSatellite = nbt.getUUID("targetSatellite");
        missionDuration = nbt.getLong("missionDuration");
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = super.saveToNbt();
        if (targetSatellite != null)
            tag.putUUID("targetSatellite", targetSatellite);
        tag.putLong("missionDuration", missionDuration);
        return tag;
    }
}
