package advRocketry.Satellites;

import advRocketry.Dimension.PlanetDimension;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;

public class Satellite {
    ItemStackHandler inventory;
    ResourceLocation parentDimensionId;
    ArrayList<ItemStack> equipment = new ArrayList<>();
    ArrayList<ItemStack> energyStorages = new ArrayList<>();

    // it is required to have a constructor with no args
    public Satellite() {
        inventory = new ItemStackHandler(7);
    }

    // build the list of equipment and energy storages before starting to tick
    public void onDeploymentStart(){
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if(stack.getItem() instanceof SatelliteEnergyStorage){
                energyStorages.add(stack);
            }
            if(stack.getItem() instanceof SatelliteEquipment){
                equipment.add(stack);
            }
        }
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
