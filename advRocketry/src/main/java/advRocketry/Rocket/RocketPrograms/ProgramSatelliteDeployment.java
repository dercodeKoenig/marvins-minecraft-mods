package advRocketry.Rocket.RocketPrograms;

import advRocketry.GlobalTime;
import advRocketry.Missions.RocketMission;
import advRocketry.Missions.SatelliteDeploymentMission;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class ProgramSatelliteDeployment extends ProgramMissionStartBase {
    ResourceLocation targetPlanet;

    ProgramSatelliteDeployment(EntityRocket rocket, ResourceLocation targetPlanet, ResourceLocation returnLevel, BlockPos returnPos, UUID missionId) {
        super(rocket, returnLevel, returnPos, missionId);
        this.targetPlanet = targetPlanet;
    }

    public void startMission(EntityRocket rocket) {
        RocketMission mission = new SatelliteDeploymentMission();
        mission.startMission(rocket, GlobalTime.getGlobalTime() + 20 * 10, missionId, returnLevel, returnPos);
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        super.readFromNbt(nbt);
        targetPlanet = ResourceLocation.parse(nbt.getString("targetPlanet"));
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = super.saveToNbt();
        tag.putString("targetPlanet", targetPlanet.toString());
        return tag;
    }
}
