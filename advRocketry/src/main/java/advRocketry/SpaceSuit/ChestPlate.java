package advRocketry.SpaceSuit;

import advRocketry.Items.ItemPortablePressureTank;
import advRocketry.Registry.Fluids;
import advRocketry.Utils.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

public class ChestPlate extends SpaceSuit {
    public ChestPlate() {
        super(Type.CHESTPLATE, new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = ((ISpaceSuitInventory) stack.getItem()).getCachedData(stack, context.registries());
        if (tag.contains("pressureTanks")) {
            int pressureTanks = tag.getInt("pressureTanks");
            tooltipComponents.add(
                    Component.literal("Pressure Tanks: " + pressureTanks).withStyle(ChatFormatting.GRAY)
            );
        }
        if (tag.contains("oxygen")) {
            int oxygen = tag.getInt("oxygen");
            tooltipComponents.add(
                    Component.literal("Oxygen: " + oxygen).withStyle(ChatFormatting.GRAY)
            );
        }
        if (tag.contains("hydrogen")) {
            int oxygen = tag.getInt("hydrogen");
            tooltipComponents.add(
                    Component.literal("Hydrogen: " + oxygen).withStyle(ChatFormatting.GRAY)
            );
        }
    }

    @Override
    public int getInventorySlots() {
        return 2;
    }

    @Override
    public boolean isItemValid(ItemStack stack, int slot) {
        if (stack.getItem() instanceof ItemPortablePressureTank) {
            IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
            if (fluidHandler.getFluidInTank(0).isEmpty() || fluidHandler.getFluidInTank(0).getFluid().equals(Fluids.OXYGEN.get())) {
                // only accept empty / oxygen tanks
                return true;
            }
        }
        if(slot == 0 && stack.getItem() instanceof Jetpack){
            return true;
        }
        return false;
    }

    @Override
    public void addCachedData(CompoundTag tag, IItemHandler inventory, HolderLookup.Provider provider) {
        CompoundTag cachedData = new CompoundTag();
        int pressureTanks = 0;
        int oxygen = 0;
        int hydrogen = 0;
        boolean jetpack = false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            // portable pressure tanks are for oxygen
            if (stack.getItem() instanceof ItemPortablePressureTank) {
                pressureTanks++;
                IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
                FluidStack fluidInTank = fluidHandler.getFluidInTank(0);
                if (fluidInTank.getFluid().equals(Fluids.OXYGEN.get())) {
                    oxygen += fluidInTank.getAmount();
                }
            }
            if (stack.getItem() instanceof Jetpack jetpackItem) {
                jetpack = true;
                CompoundTag jetpackData = jetpackItem.getCachedData(stack, provider);
                if (jetpackData.contains("hydrogen"))
                    hydrogen = jetpackData.getInt("hydrogen");
            }
        }
        cachedData.putInt("pressureTanks", pressureTanks);
        cachedData.putInt("oxygen", oxygen);
        cachedData.putInt("hydrogen", hydrogen);
        cachedData.putBoolean("jetpack", jetpack);

        tag.put(CACHED_DATA_KEY, cachedData);
    }
}
