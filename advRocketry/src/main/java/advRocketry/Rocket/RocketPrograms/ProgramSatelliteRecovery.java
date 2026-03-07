package advRocketry.Rocket.RocketPrograms;

import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.GlobalTime;
import advRocketry.Missions.SatelliteDeploymentMission;
import advRocketry.Missions.SatelliteRecoverMission;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class ProgramSatelliteRecovery extends ProgramMissionStartBase {
    UUID targetSatellite;

    public ProgramSatelliteRecovery(){

    }

    public ProgramSatelliteRecovery(EntityRocket rocket, UUID targetSatellite, ResourceLocation returnLevel, BlockPos returnPos, UUID missionId) {
        super(rocket, returnLevel, returnPos, missionId);
        this.targetSatellite = targetSatellite;
    }

    public void startMission(EntityRocket rocket) {
        SatelliteRecoverMission mission = new SatelliteRecoverMission();
        mission.setTarget(targetSatellite);
        long duration = 20 * 30; // base wait
        Satellite toReturn = SatelliteManager.getSatellite(targetSatellite);
        if(toReturn != null) {
            ResourceLocation satelliteParent = toReturn.parentDimensionId;
            if (DimensionManager.INSTANCE_SERVER.get(satelliteParent) instanceof PlanetDimension planetDimension) {
                if (DimensionManager.INSTANCE_SERVER.get(super.returnLevel) instanceof Dimension origin) {
                    double distanceAU = planetDimension.getPosition(0).distanceTo(origin.getPosition(0));
                    double extraSecond = distanceAU * Config.INSTANCE.rocket_SpaceTravel_AU_Per_Second;
                    duration += (long) (20 * extraSecond * 3); // extra time for moving to long distance planets
                }
            }
        }
        mission.startMission(rocket, GlobalTime.getGlobalTime() + duration, missionId, returnLevel, returnPos);
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        super.readFromNbt(nbt);
        if(nbt.contains("targetSatellite"))
            targetSatellite = nbt.getUUID("targetSatellite");
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = super.saveToNbt();
        if(targetSatellite != null)
            tag.putUUID("targetSatellite", targetSatellite);
        return tag;
    }
}
