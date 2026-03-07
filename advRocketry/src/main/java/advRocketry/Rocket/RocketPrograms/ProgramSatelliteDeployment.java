package advRocketry.Rocket.RocketPrograms;

import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
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

    public ProgramSatelliteDeployment(){

    }

    public ProgramSatelliteDeployment(EntityRocket rocket, ResourceLocation targetPlanet, ResourceLocation returnLevel, BlockPos returnPos, UUID missionId) {
        super(rocket, returnLevel, returnPos, missionId);
        this.targetPlanet = targetPlanet;
    }

    public void startMission(EntityRocket rocket) {
        SatelliteDeploymentMission mission = new SatelliteDeploymentMission();
        mission.setTarget(targetPlanet);
        long duration = 20 * 30; // base wait
        if(DimensionManager.INSTANCE_SERVER.get(targetPlanet) instanceof PlanetDimension planetDimension){
            if(DimensionManager.INSTANCE_SERVER.get(super.returnLevel) instanceof Dimension origin){
                double distanceAU = planetDimension.getPosition(0).distanceTo(origin.getPosition(0));
                double extraSecond = distanceAU * Config.INSTANCE.rocket_SpaceTravel_AU_Per_Second;
                duration += (long) (20 * extraSecond * 3); // extra time for moving to long distance planets
            }
        }
        mission.startMission(rocket, GlobalTime.getGlobalTime() + duration, missionId, returnLevel, returnPos);
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
