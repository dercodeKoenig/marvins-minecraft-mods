package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class guiModulePlayerInventorySlot extends guiModuleInventorySlotBase {

    public int targetSlot;

    @Override
    public ItemStack client_getItemStackToRender() {
        return Minecraft.getInstance().player.getInventory().getItem(targetSlot);
    }

    public ItemStack getStackInSlot(Player player) {
        return player.getInventory().getItem(targetSlot);
    }

    public ItemStack insertItemIntoSlot(Player player, ItemStack stack, int amount) {
        if (!player.getInventory().getItem(targetSlot).isEmpty() && !ItemStack.isSameItemSameComponents(player.getInventory().getItem(targetSlot), stack)) {
            //stack does not match item in slot, return
            return stack;
        }
        int max_size = getSlotLimit(player, stack);
        int current_count = player.getInventory().getItem(targetSlot).getCount();
        int to_insert = Math.min(max_size - current_count, amount);
        if (current_count == 0) {
            player.getInventory().setItem(targetSlot, stack.copyWithCount(to_insert));
        } else {
            player.getInventory().getItem(targetSlot).grow(to_insert);
        }
        player.inventoryMenu.broadcastChanges();
        return stack.copyWithCount(stack.getCount() - to_insert);
    }

    public ItemStack extractItemFromSlot(Player player, int amount) {
        int current_count = player.getInventory().getItem(targetSlot).getCount();
        int to_extract = Math.min(current_count, amount);
        ItemStack ret = player.getInventory().getItem(targetSlot).copyWithCount(to_extract);
        player.getInventory().getItem(targetSlot).shrink(to_extract);
        player.inventoryMenu.broadcastChanges();
        return ret;
    }

    public int getSlotLimit(Player player, ItemStack stack) {
        return player.getInventory().getMaxStackSize(stack);
    }

    public guiModulePlayerInventorySlot(int id, int targetSlot, int inventoryGroupId, int instantTransferTargetGroup, IGuiHandler guiHandler, int x, int y) {
        super(id, guiHandler, inventoryGroupId, instantTransferTargetGroup, x, y);
        this.targetSlot = targetSlot;
    }

    public static List<guiModulePlayerInventorySlot> makePlayerHotbarModules(int x, int y, int startingId, int inventoryGroup, int instantTransferTargetGroup, IGuiHandler guiHandler) {
        List<guiModulePlayerInventorySlot> modules = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            guiModulePlayerInventorySlot s = new guiModulePlayerInventorySlot(startingId + i, i, inventoryGroup, instantTransferTargetGroup, guiHandler, x + i * 18, y);
            modules.add(s);
        }

        return modules;
    }

    public static List<guiModulePlayerInventorySlot> makePlayerInventoryModules(int x, int y, int startingId, int inventoryGroup, int instantTransferTargetGroup, IGuiHandler guiHandler) {
        List<guiModulePlayerInventorySlot> modules = new ArrayList<>();

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 9; i++) {
                guiModulePlayerInventorySlot s = new guiModulePlayerInventorySlot(
                        startingId + i + 9 * j,
                        9 + i + 9 * j,
                        inventoryGroup,
                        instantTransferTargetGroup,
                        guiHandler,
                        x + i * 18,
                        y + j * 18
                );
                modules.add(s);
            }
        }


        return modules;
    }
}
