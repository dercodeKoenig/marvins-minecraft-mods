package advRocketry.Rocket.RocketPrograms;

import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.GlobalTime;
import advRocketry.Missions.AsteroidMiningMission;
import advRocketry.Missions.SatelliteDeploymentMission;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class ProgramAsteroidMiningMission extends ProgramMissionStartBase {
    String targetAsteroidId;

    public ProgramAsteroidMiningMission(){

    }

    public ProgramAsteroidMiningMission(EntityRocket rocket, String targetAsteroidId, ResourceLocation returnLevel, BlockPos returnPos, UUID missionId) {
        super(rocket, returnLevel, returnPos, missionId);
        this.targetAsteroidId = targetAsteroidId;
    }

    public void startMission(EntityRocket rocket) {
        AsteroidMiningMission mission = new AsteroidMiningMission();
        mission.setTarget(targetAsteroidId);
        long duration = 20 * 30; // base wait
        // TODO: increase wait based on asteroid size / drills on rocket
        mission.startMission(rocket, GlobalTime.getGlobalTime() + duration, missionId, returnLevel, returnPos);
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        super.readFromNbt(nbt);
        targetAsteroidId = nbt.getString("targetPlanet");
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = super.saveToNbt();
        tag.putString("targetPlanet", targetAsteroidId);
        return tag;
    }
}
