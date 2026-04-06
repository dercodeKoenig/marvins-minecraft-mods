package ARLib.utils;

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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.level.block.Block.popResource;

// a simple class to help with the movement of fluid between tank and fluid handler items like buckets
// performs the fill / drain, has helper to pop inventory and to load and save
// uses hard coded inventory group ids / save names
public class SimpleFluidContainer implements IItemHandler, IFluidHandler {

    public FluidTank myTank;
    public ItemStackHandler inventory;

    public SimpleFluidContainer(FluidTank tank, ItemStackHandler itemStackHandler) {
        this.myTank = tank;
        this.inventory = itemStackHandler;
    }

    public List<GuiModuleBase> makeGuiModules(int startId, int x, int y, IGuiHandler guiHandler) {
        List<GuiModuleBase> modules = new ArrayList<>();

        modules.add(new guiModuleFluidTankDisplay(startId, this, 0, guiHandler, x, y));

        guiModuleItemHandlerSlot s1 = new guiModuleItemHandlerSlot(startId + 1, this, 0, 1, 0, guiHandler, x + 20, y);
        s1.setSlotBackground(ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_item_slot_background_bucket.png"), 18, 18);
        modules.add(s1);

        modules.add(new guiModuleItemHandlerSlot(startId + 2, this, 1, 1, 0, guiHandler, x + 20, y + 35));

        ResourceLocation arrow = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_down.png");
        modules.add(new guiModuleImage(guiHandler, 32, 28, 16, 16, arrow, 12, 16));

        return modules;
    }

    public void popItems(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            ItemStack stack1 = inventory.getStackInSlot(0).copy();
            popResource(level, pos, stack1);
            inventory.setStackInSlot(0, ItemStack.EMPTY);

            ItemStack stack2 = inventory.getStackInSlot(1).copy();
            popResource(level, pos, stack2);
            inventory.setStackInSlot(1, ItemStack.EMPTY);
        }
    }


    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if(tag.contains("simple_fluid_container_tank"))
            myTank.readFromNBT(registries, tag.getCompound("simple_fluid_container_tank"));
        if(tag.contains("simple_fluid_container_inventory"))
            inventory.deserializeNBT(registries, tag.getCompound("simple_fluid_container_inventory"));
    }

    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag tankNBT = new CompoundTag();
        myTank.writeToNBT(registries, tankNBT);
        tag.put("simple_fluid_container_tank", tankNBT);
        CompoundTag inventoryTag = inventory.serializeNBT(registries);
        tag.put("simple_fluid_container_inventory", inventoryTag);
    }


    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return myTank.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return myTank.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return myTank.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return myTank.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return myTank.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return myTank.drain(maxDrain, action);
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (isItemValid(slot, stack)) // inventory has all valid because we need to allow insert into slot 2 from internal logic
            return inventory.insertItem(slot, stack, simulate);
        else return stack;
    }

    public ItemStack insertItemIgnoreFilter(int slot, ItemStack stack, boolean simulate) {
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && stack.getCapability(Capabilities.FluidHandler.ITEM) != null; // slot 1 is output only
    }

    public void performPossibleFluidTransfer() {
        ItemStack stack = getStackInSlot(0);
        if (!stack.isEmpty()) {

            // Make a single-item copy to operate on
            ItemStack stackCopy = stack.copyWithCount(1);
            IFluidHandlerItem fluidHandlerCopy = stackCopy.getCapability(Capabilities.FluidHandler.ITEM);

            if (fluidHandlerCopy != null) {
                FluidStack fluidInTank = getFluidInTank(0);
                int tankCapacity = getTankCapacity(0);

                // 1. Try to drain fluid from the item into the tank
                int maxFill = tankCapacity - fluidInTank.getAmount();
                FluidStack drained = fluidHandlerCopy.drain(maxFill, FluidAction.EXECUTE);
                int canFill = fill(drained, FluidAction.SIMULATE);
                ItemStack resultItem = fluidHandlerCopy.getContainer();

                // If all drained can fit into the tank, and we can insert result item into slot 1, commit!
                if (!drained.isEmpty() && canFill == drained.getAmount() && insertItemIgnoreFilter(1, resultItem, true).isEmpty()) {
                    // Commit the drain, fluid transfer, and item movement
                    fill(drained, FluidAction.EXECUTE);
                    extractItem(0, 1, false);
                    insertItemIgnoreFilter(1, resultItem, false);
                }

                // 2. If draining did not work, try filling the item instead
                else {
                    //make new copy because it may have been modified in tee code above
                    stackCopy = stack.copyWithCount(1);
                    fluidHandlerCopy = stackCopy.getCapability(Capabilities.FluidHandler.ITEM);

                    // Execute the fill operation and get the transformed container item
                    // make a copy of the fluidStack so that the original tank is not modified
                    int filled = fluidHandlerCopy.fill(fluidInTank.copy(), FluidAction.EXECUTE);
                    resultItem = fluidHandlerCopy.getContainer();
                    // Try inserting result item into slot 1
                    if (insertItemIgnoreFilter(1, resultItem, true).isEmpty()) {
                        // Commit the fill, fluid transfer, and item movement
                        drain(fluidInTank.copyWithAmount(filled), FluidAction.EXECUTE);
                        extractItem(0, 1, false);
                        insertItemIgnoreFilter(1, resultItem, false);
                    }
                }
            }
        }
    }
}
