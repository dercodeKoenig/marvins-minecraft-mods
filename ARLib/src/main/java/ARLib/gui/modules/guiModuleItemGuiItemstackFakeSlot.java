package ARLib.gui.modules;


import ARLib.gui.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

public class guiModuleItemGuiItemstackFakeSlot extends guiModuleInventorySlotBase {
    public  interface StackBasedItemHandler{
        ItemStack getStackInSlot(ItemStack stack, int slot, RegistryAccess registry);
        ItemStack insertItem(ItemStack stack, int slot, ItemStack stackToInsert, boolean simulate, RegistryAccess registry);
        ItemStack extractItem(ItemStack stack, int slot, int amount, boolean simulate, RegistryAccess registry);
        int getSlotLimit(ItemStack stack, int slot, RegistryAccess registry);
    }
    StackBasedItemHandler itemHandler;
    int targetSlot;

    public guiModuleItemGuiItemstackFakeSlot(StackBasedItemHandler itemHandler, int slot, int id, IGuiHandler guiHandler, int inventoryGroup, int instantTransferTargetGroup, int x, int y) {
        super(id, guiHandler, inventoryGroup, instantTransferTargetGroup, x, y);
        this.itemHandler = itemHandler;
        this.targetSlot = slot;
    }

    @Override
    public ItemStack client_getItemStackToRender() {
        return itemHandler.getStackInSlot(Minecraft.getInstance().player.getMainHandItem(),targetSlot, Minecraft.getInstance().level.registryAccess());
    }
    public void server_writeDataToSyncToClient(CompoundTag tag) {}

    public void client_handleDataSyncedToClient(CompoundTag tag) {}

    public void server_handleInventoryClick(Player player, int button, boolean isShift) {
        InventoryMenu inventoryMenu = player.inventoryMenu;
        ItemStack carriedStack = inventoryMenu.getCarried();
        ItemStack stack = this.getStackInSlot(player);
        if (button == 0 && !isShift) {
            if (carriedStack.isEmpty() && !stack.isEmpty()) {
                int max_pickup = Math.min(stack.getCount(), stack.getMaxStackSize());
                this.extractItemFromSlot(player, max_pickup);
            } else if (stack.isEmpty() && !carriedStack.isEmpty()) {
                this.insertItemIntoSlot(player, carriedStack, carriedStack.getCount());
            } else if (!stack.isEmpty() && !carriedStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, carriedStack)) {
                int transferAmount = Math.min(this.getSlotLimit(player) - stack.getCount(), carriedStack.getCount());
                this.insertItemIntoSlot(player, carriedStack, transferAmount);
            } else if (!stack.isEmpty() && !carriedStack.isEmpty() && !ItemStack.isSameItemSameComponents(stack, carriedStack) && stack.getCount() <= stack.getMaxStackSize() && carriedStack.getCount() <= carriedStack.getMaxStackSize()) {
                this.extractItemFromSlot(player, stack.getCount());
                this.insertItemIntoSlot(player, carriedStack, carriedStack.getCount());
            }
        }

        if (button == 0 && isShift) {
            this.extractItemFromSlot(player, stack.getCount());
        }

        if (button == 1 && !isShift) {
            if (carriedStack.isEmpty() && !stack.isEmpty()) {
                int halfCount = stack.getCount() / 2;
                this.extractItemFromSlot(player, halfCount);
            } else if (stack.getCount() < this.getSlotLimit(player) && !carriedStack.isEmpty()) {
                this.insertItemIntoSlot(player, carriedStack, 1);
            }
        }

    }

    public ItemStack getStackInSlot(Player p) {
        return itemHandler.getStackInSlot(p.getMainHandItem(), this.targetSlot, p.level().registryAccess());
    }

    public ItemStack insertItemIntoSlot(Player p, ItemStack stack, int amount) {
        ItemStack toInsert = stack.copyWithCount(amount);
        ItemStack notInserted = this.itemHandler.insertItem(p.getMainHandItem() ,this.targetSlot, toInsert, false, p.level().registryAccess());
        int inserted = toInsert.getCount() - notInserted.getCount();
        return stack.copyWithCount(stack.getCount() - inserted);
    }

    public ItemStack extractItemFromSlot(Player p, int amount) {
        return this.itemHandler.extractItem(p.getMainHandItem(), this.targetSlot, amount, false, p.level().registryAccess());
    }

    public int getSlotLimit(Player p) {
        return this.itemHandler.getSlotLimit(p.getMainHandItem(), this.targetSlot, p.level().registryAccess());
    }
}