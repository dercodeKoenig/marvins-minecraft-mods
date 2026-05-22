package advRocketry.Missions;

import advRocketry.BlockEntities.EntityCargoHold;
import advRocketry.Items.ItemSatellite;
import advRocketry.Registry.Items;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

public class SatelliteRecoverMission extends RocketMission {

    UUID target;


    public void setTarget(UUID target) {
        this.target = target;
    }

    public void completeMission() {
        EntityRocket rocket = super.restoreRocket();

        // check if there is any free item slot to insert the satellite in
        for (BlockEntity i : rocket.blockEntities.values()) {
            if (i instanceof EntityCargoHold cargoHold) {
                for (int j = 0; j < cargoHold.itemStackHandler.getSlots(); j++) {
                    if (cargoHold.itemStackHandler.getStackInSlot(j).isEmpty()) {
                        Satellite satellite = SatelliteManager.removeSatellite(target);
                        if (satellite != null) {
                            ItemStack satelliteItem = new ItemStack(Items.ITEM_SATELLITE.get(), 1);
                            ItemSatellite.saveToStack(satelliteItem, satellite, ServerLifecycleHooks.getCurrentServer().registryAccess());
                            cargoHold.itemStackHandler.setStackInSlot(j, satelliteItem);
                        }
                        return;
                    }
                }
            }
        }

        SatelliteManager.saveSatellites();
    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = super.serialize(registries);
        tag.putUUID("target", target);
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        super.deserialize(tag, registries);
        target = tag.getUUID("target");
    }
}
