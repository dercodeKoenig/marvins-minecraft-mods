package advRocketry.Utils;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

abstract public class ProxyItemHandler implements IItemHandler {
    public int slots;

    public ProxyItemHandler(int slots){
        this.slots = slots;
    }

    public abstract IItemHandler getItemHandler();
    public abstract void onContentsMaybeChanged();

    @Override
    public int getSlots() {
        return slots;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        IItemHandler itemHandler = getItemHandler();
        if (itemHandler != null)
            return itemHandler.getStackInSlot(i);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int i, ItemStack itemStack, boolean b) {
        IItemHandler itemHandler = getItemHandler();
        if (itemHandler != null) {
            ItemStack res = itemHandler.insertItem(i, itemStack, b);
            onContentsMaybeChanged();
            return res;
        }
        return itemStack;
    }

    @Override
    public ItemStack extractItem(int i, int i1, boolean b) {
        IItemHandler itemHandler = getItemHandler();
        if (itemHandler != null) {
            ItemStack res = itemHandler.extractItem(i, i1, b);
            onContentsMaybeChanged();
            return res;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int i) {
        IItemHandler itemHandler = getItemHandler();
        if (itemHandler != null)
            return itemHandler.getSlotLimit(i);
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        IItemHandler itemHandler = getItemHandler();
        if (itemHandler != null)
            return itemHandler.isItemValid(slot, stack);
        return false;
    }
}
