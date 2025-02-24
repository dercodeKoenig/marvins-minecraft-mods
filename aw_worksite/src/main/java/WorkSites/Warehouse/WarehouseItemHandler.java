package WorkSites.Warehouse;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.LinkedHashSet;
import java.util.Map;

public class WarehouseItemHandler implements IItemHandler {
    EntityWarehouse wareHouse;

    public WarehouseItemHandler(EntityWarehouse wareHouse){
        this.wareHouse = wareHouse;
    }

    @Override
    public int getSlots() {
        return wareHouse.allItemStacksWithCount.size()+1;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if(i == wareHouse.allItemStacksWithCount.size()) return ItemStack.EMPTY;

        EntityWarehouse.ComparableItemStack key = wareHouse.allItemStacksWithCount.keySet().stream().toList().get(i);
        return key.stack.copyWithCount(wareHouse.allItemStacksWithCount.get(key));
    }

    @Override
    public ItemStack insertItem(int __i, ItemStack itemStack, boolean b) {
        EntityWarehouse.ComparableItemStack key = new EntityWarehouse.ComparableItemStack(itemStack);
        LinkedHashSet<BlockEntity> whereItemsAreFound = wareHouse.whereItemStacksComeFrom.getOrDefault(key, new LinkedHashSet<>());

        ItemStack remaining = itemStack.copy();

        for (BlockEntity e : new LinkedHashSet<>(whereItemsAreFound)) {
            if (e.isRemoved()) {
                whereItemsAreFound.remove(e);
                continue;
            }
            IItemHandler itemHandler = wareHouse.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, e.getBlockPos(), e.getBlockState(), e, Direction.UP);
            if (itemHandler == null) {
                whereItemsAreFound.remove(e);
                continue;
            }
            for (int j = 0; j < itemHandler.getSlots(); j++) {
                EntityWarehouse.ComparableItemStack sc = new EntityWarehouse.ComparableItemStack(itemHandler.getStackInSlot(j));
                if (sc.equals(key)) {
                    remaining = itemHandler.insertItem(j, remaining, b);
                }
            }
            if (!b) {
                wareHouse.scanInventory(e);
            }
            if (remaining.isEmpty())
                break;
        }

        if (!remaining.isEmpty()) {
            // now it needs to scan it ALL to find if there is anywhere to insert the item
            for (BlockEntity e : wareHouse.knownInventoriesList) {
                if (e.isRemoved()) {
                    wareHouse.scanInventory(e);
                    continue;
                }
                IItemHandler itemHandler = wareHouse.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, e.getBlockPos(), e.getBlockState(), e, Direction.UP);
                if (itemHandler == null) {
                    wareHouse.scanInventory(e);
                    continue;
                }
                for (int j = 0; j < itemHandler.getSlots(); j++) {
                    // assign the remaining to be the itemStack
                    remaining = itemHandler.insertItem(j, remaining, b);
                }

                wareHouse.scanInventory(e);

                if (remaining.isEmpty())
                    break;
            }
        }

        return remaining;
    }

    @Override
    public ItemStack extractItem(int slot, int count, boolean b) {

        if(wareHouse.allItemStacksWithCount.size() == slot) return ItemStack.EMPTY;

        EntityWarehouse.ComparableItemStack key = wareHouse.allItemStacksWithCount.keySet().stream().toList().get(slot);

        if(key.stack.isEmpty()) return ItemStack.EMPTY;

        LinkedHashSet<BlockEntity> whereItemsAreFound = wareHouse.whereItemStacksComeFrom.getOrDefault(key, new LinkedHashSet<>());

        ItemStack extracted = key.stack.copyWithCount(0);

        for (BlockEntity e : new LinkedHashSet<>(whereItemsAreFound)) {
            if (e.isRemoved()) {
                whereItemsAreFound.remove(e);
                continue;
            }
            IItemHandler itemHandler = wareHouse.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, e.getBlockPos(), e.getBlockState(), e, Direction.UP);
            if (itemHandler == null) {
                whereItemsAreFound.remove(e);
                continue;
            }
            for (int j = 0; j < itemHandler.getSlots(); j++) {
                EntityWarehouse.ComparableItemStack sc = new EntityWarehouse.ComparableItemStack(itemHandler.getStackInSlot(j));
                if (sc.equals(key)) {
                    ItemStack newExtracted = itemHandler.extractItem(j, count - extracted.getCount(), b);
                    extracted.grow(newExtracted.getCount());
                }
            }

            if (!b) {
                wareHouse.scanInventory(e);
            }

            if (extracted.getCount() == count)
                break;
        }

        if (extracted.getCount() != count) {
            for (BlockEntity e : wareHouse.knownInventoriesList) {
                if (e.isRemoved()) {
                    wareHouse.scanInventory(e);
                    continue;
                }
                IItemHandler itemHandler = wareHouse.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, e.getBlockPos(), e.getBlockState(), e, Direction.UP);
                if (itemHandler == null) {
                    wareHouse.scanInventory(e);
                    continue;
                }
                for (int j = 0; j < itemHandler.getSlots(); j++) {
                    EntityWarehouse.ComparableItemStack sc = new EntityWarehouse.ComparableItemStack(itemHandler.getStackInSlot(j));
                    if (sc.equals(key)) {
                        ItemStack newExtracted = itemHandler.extractItem(j, count - extracted.getCount(), b);
                        extracted.grow(newExtracted.getCount());
                    }
                }

                wareHouse.scanInventory(e);

                if (extracted.getCount() == count)
                    break;
            }
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int i) {
        return 99;
    }

    @Override
    public boolean isItemValid(int i, ItemStack itemStack) {
        return true;
    }
}
