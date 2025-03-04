package Vehicles;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class Utils {

    public static CompoundTag getStackTagOrEmpty(ItemStack stack) {
        try {
            return stack.getTag().copy();
        } catch (Exception e) {
            CompoundTag itemTag = new CompoundTag();
            return itemTag;
        }
    }

    public static void setStackTag(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag);
    }
}
