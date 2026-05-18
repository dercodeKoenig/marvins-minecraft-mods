package advRocketry.SpaceSuit;

import advRocketry.Registry.Items;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class Boots extends SpaceSuit{

    public static final int gravity_boots_slot = 0;

    public Boots() {
        super(Type.BOOTS, new Properties().stacksTo(1));
    }

    @Override
    public int getInventorySlots() {
        return 1;
    }

    @Override
    public boolean isItemValid(ItemStack stack, int slot) {
        if(slot == gravity_boots_slot && stack.getItem().equals(Items.ITEM_GRAVITYBOOTS_UPGRADE.get())){
            return true;
        }
        return false;
    }

    @Override
    public void addCachedData(CompoundTag tag, IItemHandler inventory, HolderLookup.Provider provider) {
        CompoundTag cachedData = new CompoundTag();
        boolean gravityBootsUpgrade = false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem().equals(Items.ITEM_GRAVITYBOOTS_UPGRADE)) {
                gravityBootsUpgrade = true;
            }
        }
        cachedData.putBoolean("gravityBootsUpgrade", gravityBootsUpgrade);
        tag.put(CACHED_DATA_KEY, cachedData);
    }
}
