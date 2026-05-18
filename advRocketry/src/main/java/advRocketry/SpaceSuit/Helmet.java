package advRocketry.SpaceSuit;

import advRocketry.Registry.Items;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class Helmet extends SpaceSuit{

    public static final int night_vision_upgrade_slot = 0;

    public Helmet() {
        super(Type.HELMET, new Properties().stacksTo(1));
    }

    @Override
    public int getInventorySlots() {
        return 1;
    }

    @Override
    public boolean isItemValid(ItemStack stack, int slot) {
        if(slot == night_vision_upgrade_slot && stack.getItem().equals(Items.ITEM_NIGHTVISION_UPGRADE.get())){
            return true;
        }
        return false;
    }

    @Override
    public void addCachedData(CompoundTag tag, IItemHandler inventory, HolderLookup.Provider provider) {
        CompoundTag cachedData = new CompoundTag();
        boolean nightVisionUpgrade = false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem().equals(Items.ITEM_NIGHTVISION_UPGRADE)) {
                nightVisionUpgrade = true;
            }
        }
        cachedData.putBoolean("nightVisionUpgrade", nightVisionUpgrade);
        tag.put(CACHED_DATA_KEY, cachedData);
    }
}
