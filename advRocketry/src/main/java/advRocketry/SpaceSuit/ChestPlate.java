package advRocketry.SpaceSuit;

import net.minecraft.world.item.ItemStack;

public class ChestPlate extends SpaceSuit{
    public ChestPlate() {
        super(Type.CHESTPLATE, new Properties().stacksTo(1));
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
