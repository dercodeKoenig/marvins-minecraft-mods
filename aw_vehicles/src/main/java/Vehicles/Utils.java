package Vehicles;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class Utils {

    public static CompoundTag getStackTagOrEmpty(ItemStack stack) {
        try {
            return stack.get(DataComponents.CUSTOM_DATA).copyTag();
        } catch (Exception e) {
            CompoundTag itemTag = new CompoundTag();
            return itemTag;
        }
    }

    public static void setStackTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
