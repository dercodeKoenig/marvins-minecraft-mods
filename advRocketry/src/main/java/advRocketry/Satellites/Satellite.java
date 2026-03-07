package advRocketry.Satellites;

import advRocketry.Data.DataStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;

import static advRocketry.Registry.Items.ITEM_SOLAR_PANEL;

public class Satellite {
    ItemStackHandler inventory;
    ResourceLocation parentDimensionId;

    ArrayList<ItemStack> equipment = new ArrayList<>();
    ArrayList<ItemStack> batteries = new ArrayList<>();
    ArrayList<ItemStack> energyProducers = new ArrayList<>();

    // it is required to have a constructor with no args
    public Satellite() {
        inventory = new ItemStackHandler(7) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == 0)
                    return stack.getItem() instanceof SatellitePrimaryFunction;
                if (slot == 1 || slot == 2 || slot == 3)
                    return stack.getItem() instanceof SatelliteEnergyProducer || stack.getItem() instanceof SatelliteBattery;
                if (slot == 4 || slot == 5 || slot == 6)
                    return stack.getItem() instanceof SatelliteEquipment;
                return false;
            }
        };
    }

    // build the list of equipment and energy storages before starting to tick
    public void onDeploymentStart() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof SatelliteEnergyProducer) {
                energyProducers.add(stack);
            }
            if (stack.getItem() instanceof SatelliteBattery) {
                batteries.add(stack);
            }
            if (stack.getItem() instanceof SatelliteEquipment) {
                equipment.add(stack);
            }
        }
    }

    public int getEnergyStored() {
        int total = 0;
        for (ItemStack stack : batteries) {
            total += ((SatelliteBattery) stack.getItem()).getEnergyStored(stack);
        }
        return total;
    }

    public void tick() {

    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putString("parentDimensionId", parentDimensionId.toString());
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        parentDimensionId = ResourceLocation.parse(tag.getString("parentDimensionId"));
    }
}
