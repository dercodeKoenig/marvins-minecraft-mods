package advRocketry.Data;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface IItemDataStorage {
    ///  return data amount inserted
    int insertData(ItemStack stack, @Nullable DataStack dataStack, boolean simulate);

    /// return extracted DataStack
    @Nullable
    DataStack extractData(ItemStack stack, int amount, boolean simulate);

    @Nullable
    DataStack getDataStack(ItemStack stack);

    int getDataCapacity(ItemStack stack);

    int getRemainingCapacity(ItemStack stack);

    boolean canExtract(ItemStack stack);

    boolean canReceive(ItemStack stack);
}
