package advRocketry.Satellites;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.UUID;

public class Satellite {
    public ItemStackHandler inventory;
    public ResourceLocation parentDimensionId;

    public UUID uuid;

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
                    return stack.getItem() instanceof SatelliteEquipment || stack.getItem() instanceof SatelliteBattery;
                return false;
            }
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    // build the list of equipment and energy storages before starting to tick
    public void onDeploymentStart(ResourceLocation parentDimensionId) {
        this.parentDimensionId = parentDimensionId;
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

    /// returns energy extracted, will extract from all batteries until amount is satisfied
    public double extractEnergy(double amount){
        double extracted = 0;
        for (ItemStack stack : batteries) {
            double remaining = amount - extracted;
            extracted += ((SatelliteBattery) stack.getItem()).extractEnergy(stack, remaining);
        }
        return extracted;
    }

    /// returns energy stored in all batteries combined
    public double getEnergyStored() {
        double total = 0;
        for (ItemStack stack : batteries) {
            total += ((SatelliteBattery) stack.getItem()).getEnergyStored(stack);
        }
        return total;
    }

    public void tick() {
        this.generateEnergyAndFillBatteries();
    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", inventory.serializeNBT(registries));
        if(parentDimensionId != null)
            tag.putString("parentDimensionId", parentDimensionId.toString());
        if(uuid != null)
            tag.putUUID("uuid", uuid);
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        if(tag.contains("parentDimensionId"))
            parentDimensionId = ResourceLocation.parse(tag.getString("parentDimensionId"));
        if(tag.contains("uuid"))
            uuid = tag.getUUID("uuid");
    }

    // generated energy from the energy producers and puts it in the batteries if space is available
    private void generateEnergyAndFillBatteries() {
        double energyProduced = 0;
        // generate energy
        for (ItemStack stack : energyProducers) {
            energyProduced += ((SatelliteEnergyProducer) stack.getItem()).produceEnergy(this);
        }
        // move generated energy into batteries
        for (ItemStack stack : batteries) {
            double received = ((SatelliteBattery) stack.getItem()).receiveEnergy(stack, energyProduced);
            energyProduced -= received;
        }
    }
}
