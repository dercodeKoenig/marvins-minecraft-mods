package advRocketry.Rocket.RocketPrograms;

import ARLib.utils.RecipePartWithProbability;
import advRocketry.Blocks.Drill;
import advRocketry.GlobalTime;
import advRocketry.Missions.AsteroidManager;
import advRocketry.Missions.AsteroidMiningMission;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class ProgramAsteroidMiningMission extends ProgramMissionStartBase {
    String targetDiscoveredAsteroidKey;

    public ProgramAsteroidMiningMission() {

    }

    public ProgramAsteroidMiningMission(EntityRocket rocket, String targetAsteroidKey, ResourceLocation returnLevel, BlockPos returnPos, UUID missionId) {
        super(rocket, returnLevel, returnPos, missionId);
        this.targetDiscoveredAsteroidKey = targetAsteroidKey;
    }

    public void startMission(EntityRocket rocket) {
        AsteroidMiningMission mission = new AsteroidMiningMission();
        mission.setTarget(targetDiscoveredAsteroidKey);
        long duration = 20 * 10; // base wait

        int totalPossibleLoot = 0;
        AsteroidManager.DiscoveredAsteroid discoveredAsteroid = AsteroidManager.getDiscoveredAsteroid(targetDiscoveredAsteroidKey);
        AsteroidManager.Asteroid asteroid = AsteroidManager.getAsteroid(discoveredAsteroid);
        if (asteroid != null) {
            for (RecipePartWithProbability p : asteroid.loot) {
                totalPossibleLoot += p.amount;
            }
        }
        int drillBlocks = 0;
        for (BlockState state : rocket.blocks.values()) {
            if (state.getBlock() instanceof Drill) {
                drillBlocks++;
            }
        }
        // 1 second for every block
        if (drillBlocks > 0) {
            duration += (long) ((double) totalPossibleLoot / drillBlocks * 20);
        }
        // if no drill the mission will not make any loot

        mission.startMission(rocket, GlobalTime.getGlobalTime() + duration, missionId, returnLevel, returnPos);
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        super.readFromNbt(nbt);
        if (nbt.contains("targetAsteroidId"))
            targetDiscoveredAsteroidKey = nbt.getString("targetAsteroidId");
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = super.saveToNbt();
        if (targetDiscoveredAsteroidKey != null)
            tag.putString("targetAsteroidId", targetDiscoveredAsteroidKey);
        return tag;
    }
}
