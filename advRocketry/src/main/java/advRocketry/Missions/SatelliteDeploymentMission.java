package advRocketry.Missions;

import advRocketry.BlockEntities.EntityCargoHold;
import advRocketry.Items.ItemSatellite;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

public class SatelliteDeploymentMission extends RocketMission {

    ResourceLocation target;

    public void setTarget(ResourceLocation target) {
        this.target = target;
    }

    public void completeMission() {
        EntityRocket rocket = super.restoreRocket();

        // deploy satellites on complete
        // iterate over all cargo holds and find valid satellite builds
        if (target != null) {
            for (BlockEntity i : rocket.blockEntities.values()) {
                if (i instanceof EntityCargoHold cargoHold) {
                    for (int j = 0; j < cargoHold.itemStackHandler.getSlots(); j++) {
                        ItemStack stack = cargoHold.itemStackHandler.getStackInSlot(j);
                        if (stack.getItem() instanceof ItemSatellite) {
                            Satellite satellite = ItemSatellite.createFromItem(stack, ServerLifecycleHooks.getCurrentServer().registryAccess());
                            if (satellite != null && satellite.validateBuild().getFirst()) {
                                SatelliteManager.addTickingSatellite(satellite, target);
                                cargoHold.itemStackHandler.setStackInSlot(j, ItemStack.EMPTY);
                            }
                        }
                    }
                }
            }
            // trigger instant save so even when game crashes, the satellite will not be lost
            SatelliteManager.saveSatellites();
        }
    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = super.serialize(registries);
        tag.putString("target", target.toString());
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        super.deserialize(tag, registries);
        target = ResourceLocation.parse(tag.getString("target"));
    }
}
