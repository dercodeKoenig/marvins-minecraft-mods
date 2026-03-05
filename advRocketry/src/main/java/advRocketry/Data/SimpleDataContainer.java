package advRocketry.Data;

import ARLib.gui.IGuiHandler;
import ARLib.gui.modules.GuiModuleBase;
import ARLib.gui.modules.guiModuleFluidTankDisplay;
import ARLib.gui.modules.guiModuleImage;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/// handles the data transfer between items / dataStorage with 2 inventory slots for gui
public class SimpleDataContainer implements IItemHandler {
    public IDataStorage dataStorage;
    public IItemHandler inventory;

    public SimpleDataContainer(IDataStorage dataStorage, IItemHandler itemStackHandler) {
        this.dataStorage = dataStorage;
        this.inventory = itemStackHandler;
    }


    public int getSlots() {
        return 2;
    }

    public ItemStack getStackInSlot(int slot) {
        return this.inventory.getStackInSlot(slot);
    }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return this.isItemValid(slot, stack) ? this.inventory.insertItem(slot, stack, simulate) : stack;
    }

    public ItemStack insertItemIgnoreFilter(int slot, ItemStack stack, boolean simulate) {
        return this.inventory.insertItem(slot, stack, simulate);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return this.inventory.extractItem(slot, amount, simulate);
    }

    public int getSlotLimit(int slot) {
        return this.inventory.getSlotLimit(slot);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemDataStorage;
    }

    public void performPossibleDataTransfer() {
        ItemStack stack = this.extractItem(0, 1, true);
        if (!stack.isEmpty()) {
            if (stack.getItem() instanceof IItemDataStorage iItemDataStorage) {
                // try extract data into item
                DataStack availableData = this.dataStorage.extractData(dataStorage.getDataCapacity(), true);
                ItemStack stackCopy = stack.copyWithCount(1);
                int insertableData = iItemDataStorage.insertData(stackCopy, availableData, false); // no simulation to write the result item in stackCopy
                ItemStack notInsertableResultItem = insertItemIgnoreFilter(1, stackCopy, true);
                if(availableData != null)
                    System.out.println(availableData.amount+":"+insertableData+":"+notInsertableResultItem);
                if (insertableData != 0 && notInsertableResultItem.isEmpty()) {
                    // commit the transactions
                    this.dataStorage.extractData(insertableData, false);
                    this.insertItemIgnoreFilter(1, stackCopy, false); // stackCopy already holds the inserted data
                    this.extractItem(0, 1, false);
                } else {
                    // try to fill from item if drain was not possible
                    stackCopy = stack.copyWithCount(1);
                    int maxFill = this.dataStorage.getRemainingCapacity();
                    availableData = iItemDataStorage.extractData(stackCopy, maxFill, false);
                    notInsertableResultItem = insertItemIgnoreFilter(1, stackCopy, false);
                    if (notInsertableResultItem.isEmpty()) {
                        this.dataStorage.insertData(availableData, false);
                        this.extractItem(0, 1, false);
                    }
                }
            }
        }
    }
}
