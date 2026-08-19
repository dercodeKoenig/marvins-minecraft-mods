package advRocketry.Rocket.RocketPrograms;

import advRocketry.GlobalTime;
import advRocketry.Missions.SatelliteDeploymentMission;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class ProgramSatelliteDeployment extends ProgramMissionStartBase {
    ResourceLocation targetPlanet;
    long missionDuration;

    public ProgramSatelliteDeployment(){

    }

    public ProgramSatelliteDeployment(EntityRocket rocket, ResourceLocation targetPlanet, long missionDuration, ResourceLocation returnLevel, BlockPos returnPos, UUID missionId) {
        super(rocket, returnLevel, returnPos, missionId);
        this.targetPlanet = targetPlanet;
        this.missionDuration = missionDuration;
    }

    public void startMission(EntityRocket rocket) {
        SatelliteDeploymentMission mission = new SatelliteDeploymentMission();
        mission.setTarget(targetPlanet);
        mission.startMission(rocket, GlobalTime.getGlobalTime() + missionDuration, missionId, returnLevel, returnPos);
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        super.readFromNbt(nbt);
        if(nbt.contains("targetPlanet"))
            targetPlanet = ResourceLocation.parse(nbt.getString("targetPlanet"));
        missionDuration = nbt.getLong("missionDuration");
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = super.saveToNbt();
        if(targetPlanet != null)
            tag.putString("targetPlanet", targetPlanet.toString());
        tag.putLong("missionDuration", missionDuration);
        return tag;
    }
}
