package advRocketry.SpaceSuit;

import advRocketry.Utils.ItemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public interface ISpaceSuitInventory {

    String CACHED_DATA_KEY = "C";

    default CompoundTag getCachedData(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains(CACHED_DATA_KEY))
            return tag.getCompound(CACHED_DATA_KEY);
        return new CompoundTag();
    }

    default void addCachedData(CompoundTag tag, IItemHandler inventory, HolderLookup.Provider provider) {
        // add some cached data for fast read without need to deserialize inventory all the time
        CompoundTag cachedData = new CompoundTag();
        tag.put(CACHED_DATA_KEY, cachedData);
    }

    int getInventorySlots();

    boolean isItemValid(ItemStack stack, int slot);

    static ItemStackHandler loadInventory(ItemStack stack, HolderLookup.Provider provider) {
        if(stack.isEmpty()) return null;
        ISpaceSuitInventory iSpaceSuitInventory = ((ISpaceSuitInventory)stack.getItem());
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        ItemStackHandler inventory = new ItemStackHandler(iSpaceSuitInventory.getInventorySlots()){
            public int getSlotLimit(int slot) {
                return 1;
            }
            public boolean isItemValid(int slot, ItemStack stack) {
                return iSpaceSuitInventory.isItemValid(stack, slot);
            }
        };
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(provider, tag.getCompound("inventory"));
        }
        return inventory;
    }

    static void saveInventory(ItemStackHandler inventory, ItemStack stack, HolderLookup.Provider provider) {
        if (stack.getItem() instanceof ISpaceSuitInventory spaceSuitItem) {
            if (inventory == null)
                inventory = new ItemStackHandler(spaceSuitItem.getInventorySlots());
            CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
            tag.put("inventory", inventory.serializeNBT(provider));
            spaceSuitItem.addCachedData(tag, inventory, provider);
            ItemUtils.setTag(stack, tag);
        }
    }
}
