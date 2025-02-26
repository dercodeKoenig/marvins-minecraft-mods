package WorkSites.Warehouse;

import net.minecraft.world.item.ItemStack;

public class ComparableItemStack {
    public ItemStack stack;

    public ComparableItemStack(ItemStack stack) {
        this.stack = stack.copyWithCount(1);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            ComparableItemStack that = (ComparableItemStack) obj;
            return ItemStack.isSameItemSameComponents(stack, that.stack);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return ItemStack.hashItemAndComponents(stack);
    }
}
