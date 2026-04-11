package advRocketry.SpaceSuit;

import advRocketry.Items.ItemPortablePressureTank;
import net.minecraft.world.item.ItemStack;

public class ChestPlate extends SpaceSuit{
    public ChestPlate() {
        super(Type.CHESTPLATE, new Properties().stacksTo(1));
    }

    @Override
    public int getInventorySlots() {
        return 2;
    }

    @Override
    public boolean isItemValid(ItemStack stack, int slot) {
        if(stack.getItem() instanceof ItemPortablePressureTank)
            return true;
        return false;
    }
}
