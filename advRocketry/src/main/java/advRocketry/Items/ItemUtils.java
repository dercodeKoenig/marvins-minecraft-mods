package advRocketry.Items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class ItemUtils {
    public static CompoundTag getStacktagOrEmpty(ItemStack stack) {
        try {
            return ((CustomData) stack.get(DataComponents.CUSTOM_DATA)).copyTag();
        } catch (Exception var3) {
            return new CompoundTag();
        }
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
