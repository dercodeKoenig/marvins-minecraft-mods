package advRocketry.Rocket.RocketPrograms;

import ARLib.utils.ItemUtils;
import ARLib.utils.RecipePartWithProbability;
import advRocketry.BlockEntities.EntityCargoHold;
import advRocketry.Blocks.Drill;
import advRocketry.GlobalTime;
import advRocketry.Missions.AsteroidManager;
import advRocketry.Missions.RocketMission;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        AsteroidManager.DiscoveredAsteroid discoveredAsteroid = AsteroidManager.getDiscoveredAsteroid(targetDiscoveredAsteroidKey);
        AsteroidManager.Asteroid asteroid = AsteroidManager.getAsteroid(discoveredAsteroid);

        // find total possible loot to calculate duration
        int totalPossibleLoot = 0;
        if (asteroid != null) {
            for (RecipePartWithProbability p : asteroid.loot) {
                totalPossibleLoot += p.amount;
            }
        }
        // find total drills to calculate duration and if mining is possible
        int drillBlocks = 0;
        for (BlockState state : rocket.blocks.values()) {
            if (state.getBlock() instanceof Drill) {
                drillBlocks++;
            }
        }

        if (asteroid != null && drillBlocks > 0) {
            // this asteroid is mined, remove it from the list of discovered asteroids
            AsteroidManager.invalidateDiscoveredAsteroid(discoveredAsteroid);

            // transfer the loot into the rocket
            List<RecipePartWithProbability> loot = new ArrayList<>(asteroid.loot);
            Collections.shuffle(loot);
            for (RecipePartWithProbability item : loot) {
                item.computeRandomAmount();
                ItemStack resultStack = ItemUtils.getItemStackFromIdOrTag(item.id, item.getRandomAmount(), rocket.level().registryAccess());
                for (BlockEntity be : rocket.blockEntities.values()) {
                    if (be instanceof EntityCargoHold cargoHold) {
                        for (int i = 0; i < cargoHold.itemStackHandler.getSlots(); i++) {
                            resultStack = cargoHold.itemStackHandler.insertItem(i, resultStack, false);
                            if (resultStack.isEmpty())
                                break;
                        }
                    }
                    if (resultStack.isEmpty())
                        break;
                }
            }
        }

        // 1 second for every block
        long duration = 20 * 10; // base wait
        if (drillBlocks > 0) {
            duration += (long) ((double) totalPossibleLoot / drillBlocks * 20);
        }

        // start the mission (it will just hold the rocket for some time and spawn it back)
        RocketMission mission = new RocketMission();
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
