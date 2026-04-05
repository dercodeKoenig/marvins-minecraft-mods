package advRocketry.SpaceSuit;

import net.minecraft.world.item.ItemStack;

public class Leggings extends SpaceSuit{
    public Leggings() {
        super(Type.LEGGINGS, new Properties().stacksTo(1));
    }

    @Override
    int getInventorySlots() {
        return 0;
    }

    @Override
    boolean isItemValid(ItemStack stack, int slot) {
        return false;
    }
}
