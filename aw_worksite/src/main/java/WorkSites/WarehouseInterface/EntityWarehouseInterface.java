package WorkSites.WarehouseInterface;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import WorkSites.Config;
import WorkSites.Warehouse.EntityWarehouse;
import WorkSites.Warehouse.WarehouseItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

import static WorkSites.Registry.ENTITY_WAREHOUSE_INTERFACE;


public class EntityWarehouseInterface extends BlockEntity implements INetworkTagReceiver {

    public ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
            reScan();
        }
    };

    public EntityWarehouse warehouseReference;
    public long lastWarehouseScanTime;

    public int durabilityPercentFilter = 0;
    public boolean durabilityFilter_needsToBeAbove = true;
    guiModuleTextInput durabilityPercentFilterInput;
    guiModuleButton durabilityFilter_needsToBeAboveBtn;

    public ItemStack nextStackToRemove = ItemStack.EMPTY;
    public EntityWarehouse.ComparableItemStack nextStackToInsert = null;


    public ItemStackHandler filterInventory = new ItemStackHandler(9) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
            reScan();
        }
    };

    public GuiHandlerBlockEntity guiHandler;

    public EntityWarehouseInterface(BlockPos pos, BlockState blockState) {
        super(ENTITY_WAREHOUSE_INTERFACE.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);

        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 160, 500, 0, 2, guiHandler)) {
            guiHandler.getModules().add(m);
        }
        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 100, 600, 0, 2, guiHandler)) {
            guiHandler.getModules().add(m);
        }

        guiModuleText t1 = new guiModuleText(-2, "Filter", guiHandler, 10, 10, 0xff000000, false);
        guiHandler.getModules().add(t1);
        for (int i = 0; i < 9; i++) {
            int x = 10 + i * 18;
            int y = 20;
            guiModuleFakeItemHandlerSlot slot = new guiModuleFakeItemHandlerSlot(100 + i, filterInventory, i, 1, 0, guiHandler, x, y);
            guiHandler.getModules().add(slot);
        }

        guiModuleText t2 = new guiModuleText(-3, "Inventory", guiHandler, 10, 40, 0xff000000, false);
        guiHandler.getModules().add(t2);
        for (int i = 0; i < 9; i++) {
            int x = 10 + i * 18;
            int y = 50;
            guiModuleItemHandlerSlot slot = new guiModuleItemHandlerSlot(200 + i, inventory, i, 2, 0, guiHandler, x, y);
            guiHandler.getModules().add(slot);
        }

        durabilityFilter_needsToBeAboveBtn = new guiModuleDefaultButton(500, "?", guiHandler, 100, 79, 10, 12);
        guiHandler.getModules().add(durabilityFilter_needsToBeAboveBtn);

        guiModuleText durabilityFilterText = new guiModuleText(-1, "Durability % Filter:", guiHandler, 10, 80, 0xff000000, false);
        guiHandler.getModules().add(durabilityFilterText);

        durabilityPercentFilterInput = new guiModuleTextInput(501, guiHandler, 115, 80, 20, 10, true) {
            @Override
            public void server_readNetworkData(CompoundTag tag) {
                super.server_readNetworkData(tag);
                durabilityPercentFilter = getAsInt();
                reScan();
            }
        };
        guiHandler.getModules().add(durabilityPercentFilterInput);
    }

    @Override
    public void onLoad() {
        if (!level.isClientSide) {
            refreshGui();
        }
    }

    public void popInventory() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            Block.popResource(level, getBlockPos(), inventory.getStackInSlot(i));
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.put("filterInventory", filterInventory.serializeNBT(registries));
        tag.putBoolean("durabilityFilter_needsToBeAbove", durabilityFilter_needsToBeAbove);
        tag.putInt("durabilityPercentFilter", durabilityPercentFilter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        filterInventory.deserializeNBT(registries, tag.getCompound("filterInventory"));
        durabilityFilter_needsToBeAbove = tag.getBoolean("durabilityFilter_needsToBeAbove");
        durabilityPercentFilter = tag.getInt("durabilityPercentFilter");
    }


    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("guiButtonClick")) {
            int clicked = compoundTag.getInt("guiButtonClick");
            if (clicked == 500) {
                durabilityFilter_needsToBeAbove = !durabilityFilter_needsToBeAbove;
                refreshGui();
                reScan();
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
    }

    public void refreshGui() {
        durabilityPercentFilterInput.setTextAndSync(durabilityPercentFilter);
        if (durabilityFilter_needsToBeAbove) {
            durabilityFilter_needsToBeAboveBtn.setTextAndSync(">");
        } else {
            durabilityFilter_needsToBeAboveBtn.setTextAndSync("<");
        }
    }

    public boolean fitsDurabilityFilter(ItemStack stack, boolean needsToBeAbove, int percentValue) {
        int damage = stack.getDamageValue();
        int maxDamage = stack.getMaxDamage();
        int durabilityPercent = 100;
        if (maxDamage > 0) {
            durabilityPercent = (int) ((1 - (float) damage / maxDamage) * 100);
        }
        if (needsToBeAbove) {
            if (durabilityPercent < percentValue) {
                return false;
            }
        } else {
            if (durabilityPercent > percentValue) {
                return false;
            }
        }
        return true;
    }

    public void reScan() {

        nextStackToRemove = ItemStack.EMPTY;
        nextStackToInsert = null;
        if (warehouseReference == null)
            return;

        // sum up all the target stacks
        // items are inserted without components
        Map<EntityWarehouse.ComparableItemStack, Integer> targetStacks = new HashMap<>();
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            ItemStack stackInSlot = filterInventory.getStackInSlot(i);
            EntityWarehouse.ComparableItemStack c = new EntityWarehouse.ComparableItemStack(new ItemStack(stackInSlot.getItem(), stackInSlot.getCount()));
            targetStacks.computeIfAbsent(c, (key) -> 0);
            targetStacks.put(c, targetStacks.get(c) + stackInSlot.getCount());
        }


        // this are the ItemStacks that are available in the interface
        // items are inserted without components
        Map<EntityWarehouse.ComparableItemStack, Integer> availableStacks = new HashMap<>();

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stackInSlot = inventory.getStackInSlot(i);

            if (stackInSlot.isEmpty()) continue;

            // check for the durability filter first
            if (!fitsDurabilityFilter(stackInSlot, durabilityFilter_needsToBeAbove, durabilityPercentFilter)) {
                nextStackToRemove = stackInSlot.copy();
                return;
            }

            EntityWarehouse.ComparableItemStack c = new EntityWarehouse.ComparableItemStack(new ItemStack(stackInSlot.getItem(), stackInSlot.getCount()));
            availableStacks.computeIfAbsent(c, (key) -> 0);
            availableStacks.put(c, availableStacks.get(c) + stackInSlot.getCount());

        }

        for (EntityWarehouse.ComparableItemStack key : availableStacks.keySet()) {
            int available = availableStacks.get(key);
            if (targetStacks.containsKey(key)) {
                int targetCount = targetStacks.get(key);
                int toRemove = available - targetCount;
                if (toRemove > 0) {
                    nextStackToRemove = new ItemStack(key.stack.getItem(), toRemove);
                    return;
                }
            } else {
                nextStackToRemove = new ItemStack(key.stack.getItem(), available);
                return;
            }
        }

        // at this point it can scan if it can pull any from the warehouse

        for (EntityWarehouse.ComparableItemStack key : targetStacks.keySet()) {
            int required = targetStacks.get(key);
            int toInsert = 0;
            if (availableStacks.containsKey(key)) {
                int available = availableStacks.get(key);
                toInsert = required - available;
            } else {
                toInsert = required;
            }
            if (toInsert > 0) {
                // scan if the warehouse can deliver a match
                for (EntityWarehouse.ComparableItemStack c : warehouseReference.allItemStacksWithCount.keySet()) {
                    if (ItemStack.isSameItem(c.stack, key.stack)) {
                        if (fitsDurabilityFilter(c.stack, durabilityFilter_needsToBeAbove, durabilityPercentFilter)) {
                            nextStackToInsert = new EntityWarehouse.ComparableItemStack(c.stack);
                            nextStackToInsert.stack.setCount(required);
                            return;
                        }
                    }
                }
            }
        }
    }

    public ItemStack extractOneItemToRemove(boolean simulate) {
        if (!nextStackToRemove.isEmpty()) {
            // first check with matching components
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stackInSlot = inventory.getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(stackInSlot, nextStackToRemove)) {
                    return inventory.extractItem(i, 1, simulate);
                }
            }
            // now check without components
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stackInSlot = inventory.getStackInSlot(i);
                if (ItemStack.isSameItem(stackInSlot, nextStackToRemove)) {
                    return inventory.extractItem(i, 1, simulate);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public void interact() {
        if (level.isClientSide) {
            guiHandler.openGui(180, 190, true);
        } else {
            reScan();
            //System.out.println(nextStackToRemove);
        }
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
            if (warehouseReference != null) {
                if (warehouseReference.isRemoved() || !warehouseReference.allowedBlocks.contains(getBlockPos())) {
                    warehouseReference = null;
                } else {
                    if (warehouseReference.lastContentUpdateTime != lastWarehouseScanTime) {
                        lastWarehouseScanTime = warehouseReference.lastContentUpdateTime;
                        reScan();
                    }

                    if (Config.INSTANCE.automatic_warehouse_interface) {
                        if(!nextStackToRemove.isEmpty()) {
                            ItemStack extracted = extractOneItemToRemove(true);
                            if (!extracted.isEmpty()) {
                                if (warehouseReference.myItemHandler.insertItem(0, extracted, true) == ItemStack.EMPTY) {
                                    warehouseReference.myItemHandler.insertItem(0, extractOneItemToRemove(false), false);
                                } else {
                                    // disable removing items until next rescan
                                    // it can be compute heavy to try to insert into a full warehouse
                                    nextStackToRemove = ItemStack.EMPTY;
                                }
                            }
                        }

                        if (nextStackToInsert != null) {
                            //BlockEntity target = WarehouseItemHandler.getBlockEntityContainingItemStack(nextStackToInsert, warehouseReference);
                            //if (target != null)
                            //    System.out.println(target.getBlockPos());

                            ItemStack extracted = warehouseReference.myItemHandler.extractItem(nextStackToInsert, 1, true);
                            if (extracted.isEmpty()) {
                                reScan();
                            } else {
                                for (int j = 0; j < inventory.getSlots(); j++) {
                                    ItemStack ret = inventory.insertItem(j, extracted, true);
                                    if (ret.isEmpty()) {
                                        inventory.insertItem(j, warehouseReference.myItemHandler.extractItem(nextStackToInsert, 1, false), false);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityWarehouseInterface) t).tick();
    }
}
