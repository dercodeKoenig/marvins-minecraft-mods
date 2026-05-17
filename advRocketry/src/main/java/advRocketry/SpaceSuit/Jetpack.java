package advRocketry.SpaceSuit;

import advRocketry.Items.ItemPortablePressureTank;
import advRocketry.Registry.Fluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Properties;

public class Jetpack extends Item implements ISpaceSuitInventory{
    public Jetpack() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public int getInventorySlots() {
        return 2;
    }

    @Override
    public boolean isItemValid(ItemStack stack, int slot) {
        if (stack.getItem() instanceof ItemPortablePressureTank) {
            IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
            if (fluidHandler.getFluidInTank(0).isEmpty() || fluidHandler.getFluidInTank(0).getFluid().equals(Fluids.HYDROGEN.get())) {
                // only accept empty / hydrogen tanks
                return true;
            }
        }
        return false;
    }


    @Override
    public void addCachedData(CompoundTag tag, IItemHandler inventory, HolderLookup.Provider provider) {
        CompoundTag cachedData = new CompoundTag();
        int pressureTanks = 0;
        int hydrogen = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            // portable pressure tanks are for hydrogen
            if(stack.getItem() instanceof ItemPortablePressureTank){
                pressureTanks++;
                IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
                FluidStack fluidInTank = fluidHandler.getFluidInTank(0);
                if(fluidInTank.getFluid().equals(Fluids.HYDROGEN.get())) {
                    hydrogen += fluidInTank.getAmount();
                }
            }
        }
        cachedData.putInt("pressureTanks", pressureTanks);
        cachedData.putInt("hydrogen", hydrogen);
        tag.put(CACHED_DATA_KEY, cachedData);
    }
}
