package advRocketry.Utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class ItemUtils {
    public static CompoundTag getStacktagOrEmpty(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA))
            return stack.get(DataComponents.CUSTOM_DATA).copyTag();
        return new CompoundTag();
    }

    public static CompoundTag getStacktagOrEmptyUnsafe(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA))
            return stack.get(DataComponents.CUSTOM_DATA).getUnsafe();
        return new CompoundTag();
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        if (tag == null)
            stack.set(DataComponents.CUSTOM_DATA, null);
        else
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
