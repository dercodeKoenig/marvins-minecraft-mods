package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

public class guiModuleFakeItemHandlerSlot extends guiModuleInventorySlotBase {

    IItemHandler itemHandler;
    int targetSlot;

    public ItemStack stack;
    ItemStack lastStack;

    @Override
    public ItemStack client_getItemStackToRender() {
        return stack;
    }


    @Override
    public void server_writeDataToSyncToClient(CompoundTag tag) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            CompoundTag myTag = new CompoundTag();
            RegistryAccess registryAccess = server.registryAccess();
            myTag.putBoolean("isEmpty", itemHandler.getStackInSlot(targetSlot).isEmpty());
            if (!itemHandler.getStackInSlot(targetSlot).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                myTag.put("ItemStack", itemHandler.getStackInSlot(targetSlot).copyWithCount(1).save(registryAccess, itemTag));
                // as of 1.21.1, itemstacks can only be saved up to 99 items, so make a custom integer to set the count
                myTag.putInt("ItemStackCount", itemHandler.getStackInSlot(targetSlot).getCount());
            }
            tag.put(getMyTagKey(), myTag);
        }

        super.server_writeDataToSyncToClient(tag);

    }

    @Override
    public void client_handleDataSyncedToClient(CompoundTag tag) {
        if (tag.contains(getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(getMyTagKey());
            RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
            if (myTag.contains("ItemStack") && myTag.contains("ItemStackCount")) {
                this.stack = ItemStack.parse(registryAccess, myTag.getCompound("ItemStack")).orElse(ItemStack.EMPTY);
                this.stack.setCount(myTag.getInt("ItemStackCount"));
            } else {
                if (myTag.contains("isEmpty") && myTag.getBoolean("isEmpty"))
                    this.stack = ItemStack.EMPTY;
            }
        }
        super.client_handleDataSyncedToClient(tag);
    }

    @Override
    public void serverTick() {
        if (!ItemStack.isSameItemSameComponents(itemHandler.getStackInSlot(targetSlot), lastStack) || itemHandler.getStackInSlot(targetSlot).getCount() != lastStack.getCount()) {
            broadcastModuleUpdate();
            lastStack = itemHandler.getStackInSlot(targetSlot).copy();
        }
    }

    public guiModuleFakeItemHandlerSlot(int id, IItemHandler itemHandler, int targetSlot, int inventoryGroupId, int instantTransferTargetGroup, IGuiHandler guiHandler, int x, int y) {
        super(id, guiHandler, inventoryGroupId, instantTransferTargetGroup, x, y);
        this.targetSlot = targetSlot;
        this.itemHandler = itemHandler;
        stack = ItemStack.EMPTY;
        lastStack = ItemStack.EMPTY;
    }

    public void server_handleInventoryClick(Player player, int button, boolean isShift) {
        InventoryMenu inventoryMenu = player.inventoryMenu;
        ItemStack carriedStack = inventoryMenu.getCarried();
        ItemStack stack = getStackInSlot();

        if (button == 0 && !isShift) {

            if (carriedStack.isEmpty() && !stack.isEmpty()) {
                // Pick up the stack
                int max_pickup = Math.min(stack.getCount(), stack.getMaxStackSize());
                extractItemFromSlot(max_pickup);

            } else if (stack.isEmpty() && !carriedStack.isEmpty()) {
                // Place down the carried item
                insertItemIntoSlot(carriedStack, carriedStack.getCount());

            } else if (!stack.isEmpty() && !carriedStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, carriedStack)) {
                // Add to stack
                int transferAmount = Math.min(getSlotLimit() - stack.getCount(), carriedStack.getCount());
                insertItemIntoSlot(carriedStack, transferAmount);
            } else if (!stack.isEmpty() && !carriedStack.isEmpty() && !ItemStack.isSameItemSameComponents(stack, carriedStack)) {
                // swap items
                if (stack.getCount() <= stack.getMaxStackSize() && carriedStack.getCount() <= carriedStack.getMaxStackSize()) {
                    extractItemFromSlot(stack.getCount());
                    insertItemIntoSlot(carriedStack, carriedStack.getCount());
                }
            }
        }
        if (button == 0 && isShift) {
            extractItemFromSlot(stack.getCount());
        }

        if (button == 1 && !isShift) {
            if (carriedStack.isEmpty() && !stack.isEmpty()) {
                // Pick up half of the stack
                int halfCount = stack.getCount() / 2;
                extractItemFromSlot(halfCount);

            } else if (stack.getCount() < getSlotLimit() && !carriedStack.isEmpty()) {
                // Place one item from carried stack
                ItemStack ret = insertItemIntoSlot(carriedStack, 1);
            }
        }
    }

    public ItemStack getStackInSlot() {
        return itemHandler.getStackInSlot(targetSlot);
    }

    public ItemStack insertItemIntoSlot(ItemStack stack, int amount) {
        ItemStack toInsert = stack.copyWithCount(amount);
        ItemStack notInserted = itemHandler.insertItem(targetSlot, toInsert, false);
        int inserted = toInsert.getCount() - notInserted.getCount();
        return stack.copyWithCount(stack.getCount() - inserted);
    }

    public ItemStack extractItemFromSlot(int amount) {
        return itemHandler.extractItem(targetSlot, amount, false);
    }

    public int getSlotLimit() {
        return itemHandler.getSlotLimit(targetSlot);
    }
}
