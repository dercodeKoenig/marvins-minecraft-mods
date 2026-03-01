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

public class guiModuleFakeItemHandlerSlot extends guiModuleItemHandlerSlot {

    public guiModuleFakeItemHandlerSlot(int id, IItemHandler itemHandler, int targetSlot, int inventoryGroupId, int instantTransferTargetGroup, IGuiHandler guiHandler, int x, int y) {
        super(id, itemHandler, targetSlot, inventoryGroupId, instantTransferTargetGroup, guiHandler, x, y);
    }

    public void server_handleInventoryClick(Player player, int button, boolean isShift) {
        InventoryMenu inventoryMenu = player.inventoryMenu;
        ItemStack carriedStack = inventoryMenu.getCarried();
        ItemStack stack = getStackInSlot(player);

        if (button == 0 && !isShift) {

            if (carriedStack.isEmpty() && !stack.isEmpty()) {
                // Pick up the stack
                int max_pickup = Math.min(stack.getCount(), stack.getMaxStackSize());
                extractItemFromSlot(player, max_pickup);

            } else if (stack.isEmpty() && !carriedStack.isEmpty()) {
                // Place down the carried item
                insertItemIntoSlot(player, carriedStack, carriedStack.getCount());

            } else if (!stack.isEmpty() && !carriedStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, carriedStack)) {
                // Add to stack
                int transferAmount = Math.min(getSlotLimit(player, stack) - stack.getCount(), carriedStack.getCount());
                insertItemIntoSlot(player, carriedStack, transferAmount);

            } else if (!stack.isEmpty() && !carriedStack.isEmpty() && !ItemStack.isSameItemSameComponents(stack, carriedStack)) {
                // swap items
                if (stack.getCount() <= stack.getMaxStackSize() && carriedStack.getCount() <= carriedStack.getMaxStackSize()) {
                    extractItemFromSlot(player,stack.getCount());
                    insertItemIntoSlot(player, carriedStack, carriedStack.getCount());
                }
            }
        }
        if (button == 0 && isShift) {
            extractItemFromSlot(player, stack.getCount());
        }

        if (button == 1 && !isShift) {
            if (carriedStack.isEmpty() && !stack.isEmpty()) {
                // Pick up half of the stack
                int halfCount = stack.getCount() / 2;
                extractItemFromSlot(player, halfCount);

            } else if (stack.getCount() < getSlotLimit(player, stack) && !carriedStack.isEmpty()) {
                // Place one item from carried stack
                ItemStack ret = insertItemIntoSlot(player, carriedStack, 1);
            }
        }
    }
}
