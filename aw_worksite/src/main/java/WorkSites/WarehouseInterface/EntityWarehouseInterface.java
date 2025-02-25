package WorkSites.WarehouseInterface;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.GuiModuleBase;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.utils.ItemUtils;
import ARLib.utils.RecipePart;
import ResearchSystem.Config.RecipeConfig;
import ResearchSystem.EngineeringStation.CraftingContainerItemStackHandler;
import ResearchSystem.ResearchStation.ItemResearchBook;
import WorkSites.EntityWorkSiteBase;
import WorkSites.Warehouse.EntityWarehouse;
import WorkSites.Warehouse.WarehouseItemHandler;
import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

import static WorkSites.Registry.ENTITY_WAREHOUSE_CRAFTER;
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
    public boolean durabilityFilter_aboveTarget = true;

    public ItemStack nextStackToRemove = ItemStack.EMPTY;
    public EntityWarehouse.ComparableItemStack nextStackToInsert = null;


    public ItemStackHandler filterInventory = new ItemStackHandler(9) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            super.insertItem(slot, stack, simulate);
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            super.extractItem(slot, amount, simulate);
            return ItemStack.EMPTY;
        }

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


        for (int i = 0; i < 9; i++) {
            int x = 10 + i * 18;
            int y = 20;
            guiModuleItemHandlerSlot slot = new guiModuleItemHandlerSlot(100 + i, filterInventory, i, 1, 0, guiHandler, x, y);
            guiHandler.getModules().add(slot);
        }

        for (int i = 0; i < 9; i++) {
            int x = 10 + i * 18;
            int y = 50;
            guiModuleItemHandlerSlot slot = new guiModuleItemHandlerSlot(200 + i, inventory, i, 2, 0, guiHandler, x, y);
            guiHandler.getModules().add(slot);
        }

    }

    @Override
    public void onLoad() {
        if (!level.isClientSide) {

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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        filterInventory.deserializeNBT(registries, tag.getCompound("filterInventory"));
    }


    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {
        guiHandler.readServer(compoundTag);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
    }

    public boolean fitsDurabilityFilter(ItemStack stack) {
        int damage = stack.getDamageValue();
        int maxDamage = stack.getMaxDamage();
        int durabilityPercent = 100;
        if (maxDamage > 0) {
            durabilityPercent = (int) ((1 - (float) damage / maxDamage) * 100);
        }
        if (durabilityFilter_aboveTarget) {
            if (durabilityPercent < durabilityPercentFilter) {
                return false;
            }
        } else {
            if (durabilityPercent > durabilityPercentFilter) {
                return false;
            }
        }
        return true;
    }

    public void reScan() {

        // sum up all the target stacks without components
        Map<EntityWarehouse.ComparableItemStack, Integer> targetStacks = new HashMap<>();
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            ItemStack stackInSlot = filterInventory.getStackInSlot(i);
            EntityWarehouse.ComparableItemStack c = new EntityWarehouse.ComparableItemStack(new ItemStack(stackInSlot.getItem(), stackInSlot.getCount()));
            targetStacks.computeIfAbsent(c, (key) -> 0);
            targetStacks.put(c, targetStacks.get(c) + stackInSlot.getCount());
        }

        // sums up all the items allowed by filters (currently only durability filter)
        Map<EntityWarehouse.ComparableItemStack, Integer> allowedStacks = new HashMap<>();

        nextStackToRemove = ItemStack.EMPTY;
        nextStackToInsert = null;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stackInSlot = inventory.getStackInSlot(i);

            if (stackInSlot.isEmpty()) continue;

            // check for the durability filter first
            if (!fitsDurabilityFilter(stackInSlot)) {
                nextStackToRemove = stackInSlot.copy();
                return;
            }

            EntityWarehouse.ComparableItemStack c = new EntityWarehouse.ComparableItemStack(new ItemStack(stackInSlot.getItem(), stackInSlot.getCount()));
            allowedStacks.computeIfAbsent(c, (key) -> 0);
            allowedStacks.put(c, allowedStacks.get(c) + stackInSlot.getCount());

        }

        for (EntityWarehouse.ComparableItemStack key : allowedStacks.keySet()) {
            int available = allowedStacks.get(key);
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
            if (allowedStacks.containsKey(key)) {
                int available = allowedStacks.get(key);
                toInsert = required - available;
            } else {
                toInsert = required;
            }
            if (toInsert > 0) {
                // scan if the warehouse can deliver a match
                for (EntityWarehouse.ComparableItemStack c : warehouseReference.allItemStacksWithCount.keySet()) {
                    if (ItemStack.isSameItem(c.stack, key.stack)) {
                        if (fitsDurabilityFilter(c.stack)) {
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
                if (warehouseReference.isRemoved()) {
                    warehouseReference = null;
                } else {
                    if (level.getGameTime() % 20 == 0) {
                        ItemStack extraced = extractOneItemToRemove(true);
                        if (!extraced.isEmpty()) {
                            if (warehouseReference.myItemHandler.insertItem(0, extraced, true) == ItemStack.EMPTY) {
                                warehouseReference.myItemHandler.insertItem(0, extractOneItemToRemove(false), false);
                                System.out.println(extraced);
                            }
                        }

                        if (nextStackToInsert != null) {
                            System.out.println(nextStackToInsert.stack);
                            BlockEntity target = WarehouseItemHandler.getBlockEntityContainingItemStack(nextStackToInsert, warehouseReference);
                            if (target != null)
                                System.out.println(target.getBlockPos());
                            Insertion:
                            {
                                for (int i = 0; i < warehouseReference.myItemHandler.getSlots(); i++) {
                                    if (ItemStack.isSameItemSameComponents(warehouseReference.myItemHandler.getStackInSlot(i), nextStackToInsert.stack)) {
                                        ItemStack extracted = warehouseReference.myItemHandler.extractItem(i, 1, true);
                                        for (int j = 0; j < inventory.getSlots(); j++) {
                                            ItemStack ret = inventory.insertItem(j, extracted, true);
                                            if (ret.isEmpty()) {
                                                inventory.insertItem(j, warehouseReference.myItemHandler.extractItem(i, 1, false), false);
                                                break Insertion;
                                            }
                                        }
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
