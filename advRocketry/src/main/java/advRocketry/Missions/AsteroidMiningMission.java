package advRocketry.Missions;

import ARLib.utils.ItemUtils;
import ARLib.utils.RecipePartWithProbability;
import advRocketry.BlockEntities.EntityCargoHold;
import advRocketry.Items.ItemAsteroidIdChip;
import advRocketry.Items.ItemSatellite;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsteroidMiningMission extends RocketMission {

    String target;

    public void setTarget(String target) {
        this.target = target;
    }

    public void completeMission() {
        EntityRocket rocket = super.restoreRocket();

        ItemAsteroidIdChip.Asteroid asteroid = ItemAsteroidIdChip.asteroids.get(target);
        if (asteroid != null) {
            List<RecipePartWithProbability> loot = new ArrayList<>(asteroid.loot);
            // shuffle in case we dont have enough storage
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
    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = super.serialize(registries);
        tag.putString("target", target);
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        super.deserialize(tag, registries);
        target = tag.getString("target");
    }
}
