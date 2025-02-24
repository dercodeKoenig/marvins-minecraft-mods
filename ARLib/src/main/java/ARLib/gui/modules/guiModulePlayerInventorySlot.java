package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class guiModulePlayerInventorySlot extends guiModuleInventorySlotBase{

    int targetSlot;

    @Override
    public ItemStack client_getItemStackToRender() {
        return  Minecraft.getInstance().player.getInventory().getItem(targetSlot);
    }

    public ItemStack getStackInSlot(Player player) {
        return player.getInventory().getItem(targetSlot);
    }

    public ItemStack insertItemIntoSlot(Player player, ItemStack stack, int amount) {
        if(!player.getInventory().getItem(targetSlot).isEmpty() && !ItemStack.isSameItemSameComponents(player.getInventory().getItem(targetSlot),stack)){
            //stack does not match item in slot, return
            return stack;
        }
        int max_size = getSlotLimit(player,stack);
        int current_count = player.getInventory().getItem(targetSlot).getCount();
        int to_insert = Math.min(max_size - current_count, amount);
        if (current_count == 0){
            player.getInventory().setItem(targetSlot,stack.copyWithCount(to_insert));
        }else{
            player.getInventory().getItem(targetSlot).grow(to_insert);
        }
        return stack.copyWithCount(stack.getCount()-to_insert);
    }

    public ItemStack extractItemFromSlot(Player player, int amount) {
        int current_count = player.getInventory().getItem(targetSlot).getCount();
        int to_extract = Math.min(current_count,amount);
        ItemStack ret =player.getInventory().getItem(targetSlot).copyWithCount(to_extract);
        player.getInventory().getItem(targetSlot).shrink(to_extract);
        return ret;
    }

    public int getSlotLimit(Player player,ItemStack stack) {
        return player.getInventory().getMaxStackSize(stack);
    }

    public guiModulePlayerInventorySlot(int id, int targetSlot, int inventoryGroupId, int instantTransferTargetGroup, IGuiHandler guiHandler, int x, int y) {
        super(id,guiHandler,inventoryGroupId,instantTransferTargetGroup,x, y);
        this.targetSlot = targetSlot;
    }

    public void server_handleInventoryClick(Player player, int button, boolean isShift) {
        InventoryMenu inventoryMenu = player.inventoryMenu;
        ItemStack carriedStack = inventoryMenu.getCarried();
        ItemStack stack = getStackInSlot(player);

        if (button == 0 && !isShift) {

            if (carriedStack.isEmpty() && !stack.isEmpty()) {
                // Pick up the stack
                int max_pickup = Math.min(stack.getCount(),stack.getMaxStackSize());
                inventoryMenu.setCarried(extractItemFromSlot(player,max_pickup));

            } else if (stack.isEmpty() && !carriedStack.isEmpty()) {
                // Place down the carried item
                inventoryMenu.setCarried(insertItemIntoSlot(player,carriedStack,carriedStack.getCount()));

            } else if (!stack.isEmpty() && !carriedStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, carriedStack)) {
                // Add to stack
                int transferAmount = Math.min(getSlotLimit(player,stack) - stack.getCount(), carriedStack.getCount());
                inventoryMenu.setCarried(insertItemIntoSlot(player,carriedStack,transferAmount));
            } else if (!stack.isEmpty() && !carriedStack.isEmpty() && !ItemStack.isSameItemSameComponents(stack, carriedStack)) {
                // swap items
                if (stack.getCount()<=stack.getMaxStackSize() && carriedStack.getCount()<=carriedStack.getMaxStackSize()){
                    ItemStack stackCopy = stack.copy();
                    extractItemFromSlot(player,stack.getCount());
                    insertItemIntoSlot(player,carriedStack,carriedStack.getCount());
                    inventoryMenu.setCarried(stackCopy);
                }
            }
        }
        if (button == 1 && !isShift) {
            if (carriedStack.isEmpty() && !stack.isEmpty()) {
                // Pick up half of the stack
                int halfCount = stack.getCount() / 2;
                inventoryMenu.setCarried(extractItemFromSlot(player,halfCount));

            } else if (stack.getCount() < getSlotLimit(player,stack) && !carriedStack.isEmpty()) {
                // Place one item from carried stack
                ItemStack ret = insertItemIntoSlot(player,carriedStack,1);
                inventoryMenu.setCarried(ret);
            }
        }
        if (button == 0 && isShift) {
            // move all items in the current slot to slots of the instant transfer target group
            // loop over all modules and try to find a module where the group id matches the transfer target


            List<GuiModuleBase> allModules =new ArrayList<>(this.guiHandler.getModules());
            for (GuiModuleBase i : new ArrayList<>(allModules)) {
                if (i instanceof guiModuleScrollContainer container) {
                    allModules.addAll(container.getAllModulesAndSubModules());
                }
            }

            for (GuiModuleBase i : allModules) {

                if (i instanceof guiModulePlayerInventorySlot j) {
                    if (j.invGroup == instantTransferTarget) {
                        ItemStack notInserted = j.insertItemIntoSlot(player, stack, stack.getCount());
                        int inserted = stack.getCount() - notInserted.getCount();
                        extractItemFromSlot(player,inserted);
                        stack = notInserted;
                    }
                }
                if (i instanceof guiModuleItemHandlerSlot j) {
                    if (j.invGroup == instantTransferTarget) {
                        ItemStack notInserted = j.insertItemIntoSlot(stack, stack.getCount());
                        int inserted = stack.getCount() - notInserted.getCount();
                        extractItemFromSlot(player,inserted);
                        stack = notInserted;
                    }
                }
            }
        }
    }


    public static List<guiModulePlayerInventorySlot> makePlayerHotbarModules(int x, int y, int startingId, int inventoryGroup, int instantTransferTargetGroup, IGuiHandler guiHandler){
        List<guiModulePlayerInventorySlot> modules = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            guiModulePlayerInventorySlot s = new guiModulePlayerInventorySlot(startingId+i, i,inventoryGroup,instantTransferTargetGroup,guiHandler,x+i*18,y);
            modules.add(s);
        }

        return modules;
    }

    public static List<guiModulePlayerInventorySlot> makePlayerInventoryModules(int x, int y, int startingId, int inventoryGroup, int instantTransferTargetGroup,  IGuiHandler guiHandler){
        List<guiModulePlayerInventorySlot> modules = new ArrayList<>();

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 9; i++) {
                guiModulePlayerInventorySlot s = new guiModulePlayerInventorySlot(
                        startingId+i+9*j,
                        9+i+9*j,
                        inventoryGroup,
                        instantTransferTargetGroup,
                        guiHandler,
                        x+i*18,
                        y+j*18
                );
                modules.add(s);
            }
        }


        return modules;
    }
}
