package advRocketry.Rocket.RocketPrograms;

import ARLib.utils.RecipePartWithProbability;
import advRocketry.Blocks.Drill;
import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemAsteroidIdChip;
import advRocketry.Missions.AsteroidMiningMission;
import advRocketry.Missions.SatelliteDeploymentMission;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

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
        long duration = 5; // base wait


        ItemAsteroidIdChip.Asteroid asteroid = ItemAsteroidIdChip.asteroids.get(targetAsteroidId);
        int totalPossibleLoot = 0;
        if (asteroid != null) {
            for(RecipePartWithProbability p : asteroid.loot){
                totalPossibleLoot += p.amount;
            }
        }
        int drillBlocks = 0;
        for(BlockState state : rocket.blocks.values()){
            if(state.getBlock() instanceof Drill){
                drillBlocks++;
            }
        }
        // 1 second for every block
        if(drillBlocks > 0) {
            duration += (long) ((double) totalPossibleLoot / drillBlocks * 20);
        }
        // if no drill the mission will not make any loot

        mission.startMission(rocket, GlobalTime.getGlobalTime() + duration, missionId, returnLevel, returnPos);
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        super.readFromNbt(nbt);
        if(nbt.contains("targetAsteroidId"))
            targetAsteroidId = nbt.getString("targetAsteroidId");
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = super.saveToNbt();
        if(targetAsteroidId != null)
            tag.putString("targetAsteroidId", targetAsteroidId);
        return tag;
    }
}
