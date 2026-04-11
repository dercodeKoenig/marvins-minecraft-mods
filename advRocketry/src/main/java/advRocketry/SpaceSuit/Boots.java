package advRocketry.SpaceSuit;

import net.minecraft.world.item.ItemStack;

public class Boots extends SpaceSuit{
    public Boots() {
        super(Type.BOOTS, new Properties().stacksTo(1));
    }

    @Override
    public int getInventorySlots() {
        return 0;
    }

    @Override
    public boolean isItemValid(ItemStack stack, int slot) {
        return false;
    }
}
