package advRocketry.Items;

import advRocketry.Data.DataStack;
import advRocketry.Data.DataStorage;
import advRocketry.Data.IItemDataStorage;
import advRocketry.Satellites.SatelliteEquipment;
import advRocketry.utils.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

public class ItemDataStorage extends Item implements IItemDataStorage, SatelliteEquipment {

    int maxData = 1000;

    public ItemDataStorage() {
        super(new Properties());
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        DataStack stack1 = getDataStack(stack);
        if (stack1 != null) {
            tooltipComponents.add(
                    Component.literal("Data: " + stack1.type)
            );
            tooltipComponents.add(
                    Component.literal("Amount: " + stack1.amount+" / "+maxData)
            );
        }
    }

    public void setDataStack(ItemStack stack, DataStack dataStack) {
        if (dataStack == null)
            ItemUtils.setTag(stack, new CompoundTag());
        else {
            CompoundTag tag = dataStack.saveToNbt();
            ItemUtils.setTag(stack, tag);
        }
    }

    @Override
    public DataStack getDataStack(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        return DataStack.createFromNbt(tag);
    }

    @Override
    public int insertData(ItemStack stack, DataStack dataStack, boolean simulate) {
        DataStorage dataStorage = new DataStorage(maxData);
        dataStorage.setStackDirect(getDataStack(stack));
        int inserted = dataStorage.insertData(dataStack, simulate);
        if (!simulate) {
            setDataStack(stack, dataStorage.getDataStack());
        }
        return inserted;
    }

    @Nullable
    @Override
    public DataStack extractData(ItemStack stack, int amount, boolean simulate) {
        DataStorage dataStorage = new DataStorage(maxData);
        dataStorage.setStackDirect(getDataStack(stack));
        DataStack extracted = dataStorage.extractData(amount, simulate);
        if (!simulate) {
            setDataStack(stack, dataStorage.getDataStack());
        }
        return extracted;
    }

    @Override
    public int getDataCapacity(ItemStack stack) {
        return maxData;
    }

    @Override
    public int getRemainingCapacity(ItemStack stack) {
        int remaining = maxData;
        DataStack dataStack = getDataStack(stack);
        if (dataStack != null)
            remaining -= dataStack.amount;
        return remaining;
    }

    @Override
    public boolean canExtract(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canReceive(ItemStack stack) {
        return true;
    }
}
