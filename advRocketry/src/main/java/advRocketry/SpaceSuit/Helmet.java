package advRocketry.SpaceSuit;

import advRocketry.Registry.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

public class Helmet extends SpaceSuit {

    public Helmet() {
        super(Type.HELMET, new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = ((ISpaceSuitInventory) stack.getItem()).getCachedDataUnsafe(stack);
        if (tag.contains("nightVisionUpgrade") && tag.getBoolean("nightVisionUpgrade")) {
            tooltipComponents.add(
                    Component.literal("night vision upgrade").withStyle(ChatFormatting.GRAY)
            );
        }
        if (tag.contains("flightSpeedUpgrades")) {
            tooltipComponents.add(
                    Component.literal("flight speed upgrades: " + tag.getInt("flightSpeedUpgrades")).withStyle(ChatFormatting.GRAY)
            );
        }
    }

    @Override
    public int getInventorySlots() {
        return 2;
    }

    @Override
    public boolean isItemValid(ItemStack stack, int slot) {
        if (stack.getItem().equals(Items.ITEM_NIGHT_VISION_UPGRADE.get())) {
            return true;
        }
        if (stack.getItem().equals(Items.ITEM_FLIGHT_SPEED_UPGRADE.get())) {
            return true;
        }
        return false;
    }

    @Override
    public void addCachedData(CompoundTag tag, IItemHandler inventory, HolderLookup.Provider provider) {
        CompoundTag cachedData = new CompoundTag();
        boolean nightVisionUpgrade = false;
        int flightSpeedUpgrades = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem().equals(Items.ITEM_NIGHT_VISION_UPGRADE.get())) {
                nightVisionUpgrade = true;
            }
            if (stack.getItem().equals(Items.ITEM_FLIGHT_SPEED_UPGRADE)) {
                flightSpeedUpgrades++;
            }
        }
        cachedData.putBoolean("nightVisionUpgrade", nightVisionUpgrade);
        cachedData.putInt("flightSpeedUpgrades", flightSpeedUpgrades);
        tag.put(CACHED_DATA_KEY, cachedData);
    }
}
