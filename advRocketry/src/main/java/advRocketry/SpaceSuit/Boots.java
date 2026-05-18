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

public class Boots extends SpaceSuit{

    public Boots() {
        super(Type.BOOTS, new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = ((ISpaceSuitInventory) stack.getItem()).getCachedDataUnsafe(stack);
        if (tag.contains("gravityBootsUpgrade") && tag.getBoolean("gravityBootsUpgrade")) {
            tooltipComponents.add(
                    Component.literal("gravity boots upgrade").withStyle(ChatFormatting.GRAY)
            );
        }
    }

    @Override
    public int getInventorySlots() {
        return 1;
    }

    @Override
    public boolean isItemValid(ItemStack stack, int slot) {
        if(stack.getItem().equals(Items.ITEM_GRAVITY_BOOTS_UPGRADE.get())){
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
            if (stack.getItem().equals(Items.ITEM_GRAVITY_BOOTS_UPGRADE.get())) {
                gravityBootsUpgrade = true;
            }
        }
        cachedData.putBoolean("gravityBootsUpgrade", gravityBootsUpgrade);
        tag.put(CACHED_DATA_KEY, cachedData);
    }
}
