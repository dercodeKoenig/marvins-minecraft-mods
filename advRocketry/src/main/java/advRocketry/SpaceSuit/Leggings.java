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

public class Leggings extends SpaceSuit{

    public Leggings() {
        super(Type.LEGGINGS, new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = ((ISpaceSuitInventory) stack.getItem()).getCachedDataUnsafe(stack);
        if (tag.contains("legsUpgrade") && tag.getBoolean("legsUpgrade")) {
            tooltipComponents.add(
                    Component.literal("legs upgrade").withStyle(ChatFormatting.GRAY)
            );
        }
    }

    @Override
    public int getInventorySlots() {
        return 1;
    }

    @Override
    public boolean isItemValid(ItemStack stack, int slot) {
        if(stack.getItem().equals(Items.ITEM_LEGS_UPGRADE.get())){
            return true;
        }
        return false;
    }

    @Override
    public void addCachedData(CompoundTag tag, IItemHandler inventory, HolderLookup.Provider provider) {
        CompoundTag cachedData = new CompoundTag();
        boolean legsUpgrade = false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem().equals(Items.ITEM_LEGS_UPGRADE.get())) {
                legsUpgrade = true;
            }
        }
        cachedData.putBoolean("legsUpgrade", legsUpgrade);
        tag.put(CACHED_DATA_KEY, cachedData);
    }
}
