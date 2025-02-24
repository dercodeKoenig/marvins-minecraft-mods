package WorkSites.Warehouse;

import ARLib.gui.modules.*;
import ARLib.utils.BlockIdentifier;
import WorkSites.EntityWorkSiteBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.*;

import static WorkSites.Registry.ENTITY_WAREHOUSE;

public class EntityWarehouse extends EntityWorkSiteBase {

    public static Set<BlockIdentifier> knownWarehouses = new HashSet<>();

    int currentBlockToScanIndex_blocks = 0;
    int currentBlockToScanIndex_inventories = 0;

    public EntityWarehouse(BlockPos pos, BlockState blockState) {
        super(ENTITY_WAREHOUSE.get(), pos, blockState);


        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 210, 500, 0, 1, guiHandlerMain)) {
            guiHandlerMain.getModules().add(m);
        }
        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 150, 600, 0, 1, guiHandlerMain)) {
            guiHandlerMain.getModules().add(m);
        }

    }

    @Override
    public void onLoad() {
        super.onLoad();
        knownWarehouses.add(new BlockIdentifier(getLevel(), getBlockPos()));
    }

    @Override
    public void setRemoved() {
        knownWarehouses.remove(new BlockIdentifier(getLevel(), getBlockPos()));
        super.setRemoved();
    }

    public void scanStep() {
        // this will scan for new inventories one block a tick
        if (!allowedBlocksList.isEmpty() && allowedBlocks.size() != knownInventoriesList.size()) {
            if (currentBlockToScanIndex_blocks >= allowedBlocksList.size()) {
                currentBlockToScanIndex_blocks = 0;
            }
            BlockPos nextPosToScan = allowedBlocksList.get(currentBlockToScanIndex_blocks);
            currentBlockToScanIndex_blocks += 1;

            if (!knownInventories.keySet().contains(nextPosToScan)) {
                BlockEntity be = level.getBlockEntity(nextPosToScan);
                if (be != null) {
                    IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, nextPosToScan, be.getBlockState(), be, Direction.UP);
                    if (inventory != null) {
                        addBlockEntityInventory(be);
                    }
                }
            }
        }
        // this will scan discovered inventories for changes, 1 block a tick
        // it will also take care of removing invalid inventories
        if (!knownInventoriesList.isEmpty()) {
            if (currentBlockToScanIndex_inventories >= knownInventoriesList.size()) {
                currentBlockToScanIndex_inventories = 0;
            }
            BlockEntity nextToScan = knownInventoriesList.get(currentBlockToScanIndex_inventories);
            currentBlockToScanIndex_inventories += 1;

            scanInventory(nextToScan);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            //long t0 = System.nanoTime();
            scanStep();
            //long t1 = System.nanoTime();
            //System.out.println((double) (t1 - t0) / 1000 / 1000 ); // 0.01-0.1ms

            if (level.getGameTime() % 100 == 0) {
                for (ComparableItemStack i : allItemStacksWithCount.keySet()) {
                    System.out.println(i.stack + ":" + allItemStacksWithCount.get(i));
                }
            }
        }
    }

    @Override
    public void openMainGui() {
        if (level.isClientSide) {
            guiHandlerMain.openGui(180, 240, true);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }


    @Override
    public void updateBoundsBp() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos p1 = getBlockPos().relative(facing, controllerOffsetH - 1);
        p1 = p1.relative(facing.getClockWise(), controllerOffsetW);
        p1 = new BlockPos(p1.getX(), getBlockPos().getY(), p1.getZ());
        BlockPos p2 = p1.relative(facing.getCounterClockWise(), w - 1).relative(facing.getOpposite(), h - 1).relative(Direction.UP, 4);

        pmin = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
        pmax = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));

        // this farm does not use blacklist
        updateAllowedBlocksList();

        // clean all maps because blocks that was before in the are might no longer be
        // the most important is that "filteredItemStacksMap_copy" and "allItemStacksWithCount" are always kept in sync
        knownInventories.clear();
        filteredItemStacksMap_reference.clear();
        filteredItemStacksMap_copy.clear();
        fullItemStacksMap_copy.clear();
        knownInventoriesList.clear();
        allItemStacksWithCount.clear();
    }


    public static class ComparableItemStack {
        public ItemStack stack;

        public ComparableItemStack(ItemStack stack) {
            this.stack = stack.copyWithCount(1);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj != null && this.getClass() == obj.getClass()) {
                ComparableItemStack that = (ComparableItemStack) obj;
                return ItemStack.isSameItemSameComponents(stack, that.stack);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return ItemStack.hashItemAndComponents(stack);
        }
    }

    // all the detected inventories are here
    public HashMap<BlockPos, BlockEntity> knownInventories = new HashMap<>();
    public List<BlockEntity> knownInventoriesList = new ArrayList<>(); // same but as a list

    // This one is to hold references to all the scanned ItemStacks.
    // It is used to check if a itemstack was already processed.
    // (this happens because a chest has 2 itemhandlers but both return the same itemstacks)
    BiDirectionalMultiMap<ItemStack, BlockPos> filteredItemStacksMap_reference = new BiDirectionalMultiMap<>();

    // This one will also be added only once for any ItemStack, but it holds a copy.
    // The copy is used to remove the previous ItemStacks from the total sum of all ItemStacks during scanning.
    // To avoid rescanning the entire map to get the sum of all items, members of this map will be subtracted from the total map
    // before scanning and re-added after scanning.
    BiDirectionalMultiMap<ItemStack, BlockPos> filteredItemStacksMap_copy = new BiDirectionalMultiMap<>();

    // this one holds every itemstack in the correct order as it is in the inventory to detect changes
    BiDirectionalMultiMap<ItemStack, BlockPos> fullItemStacksMap_copy = new BiDirectionalMultiMap<>();

    // final map with all items and count
    // also referenced as "full map" in comments below
    // because the system never makes a full re-scan of the entire inventory at once (to save a lot of time),
    // you should not write to this yourself. you risk making the map out of sync with the inventories and it will
    // never recover until world reload when out of sync. so if you modify anything here, make sure you know what you are doing
    // (dont touch it and it should work)
    public Map<ComparableItemStack, Integer> allItemStacksWithCount = new HashMap<>();

    public void addBlockEntityInventory(BlockEntity e) {
        knownInventories.put(e.getBlockPos(), e);
        knownInventoriesList = new ArrayList<>(knownInventories.values());
    }

    public void removeBlockEntityInventory(BlockPos p) {
        knownInventories.remove(p);

        // remove all the items added to the full map by this entity before removing it
        for (ItemStack i : filteredItemStacksMap_copy.getFromvalue(p)) {
            ComparableItemStack c = new ComparableItemStack(i);
            if (allItemStacksWithCount.get(c) != null) {
                allItemStacksWithCount.put(c, allItemStacksWithCount.get(c) - i.getCount());
                if (allItemStacksWithCount.get(c) == 0)
                    allItemStacksWithCount.remove(c);
            }
        }
        // cleanup the maps
        filteredItemStacksMap_copy.removeByValue(p);
        filteredItemStacksMap_reference.removeByValue(p);
        fullItemStacksMap_copy.removeByValue(p);

        // recompute the list for iteration
        knownInventoriesList = new ArrayList<>(knownInventories.values());
    }

    public void scanInventory(BlockEntity e) {
        if (e.isRemoved()) {
            removeBlockEntityInventory(e.getBlockPos());
            return;
        }

        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, e.getBlockPos(), e.getBlockState(), e, Direction.UP);
        LinkedHashSet<ItemStack> cachedContents = fullItemStacksMap_copy.getFromvalue(e.getBlockPos());
        if (itemHandler != null) {
            // check if anything in the inventory has changed since last scanning
            // if nothing is changed, the set should be exactly as the itemhandler by item, components and count
            boolean needsRescan = false;
                if (cachedContents.size() != itemHandler.getSlots()) {
                    needsRescan = true;
                } else {
                    List<ItemStack> cachedStackList = cachedContents.stream().toList();
                    for (int i = 0; i < cachedStackList.size(); i++) {
                        if (!ItemStack.isSameItemSameComponents(cachedStackList.get(i), itemHandler.getStackInSlot(i)) || cachedStackList.get(i).getCount() != itemHandler.getStackInSlot(i).getCount()) {
                            needsRescan = true;
                            break;
                        }
                    }
                }
            if (needsRescan) {
                // if the inventory is changed, first revert the added itemstacks to the full map
                // last scan it added some itemstacks to the full map. now this exact items need to be removed again
                // this requires the map with copies of the itemstacks, because the references already reflect the new state
                for (ItemStack i : filteredItemStacksMap_copy.getFromvalue(e.getBlockPos())) {
                    ComparableItemStack c = new ComparableItemStack(i);
                    if (allItemStacksWithCount.get(c) != null) {
                        allItemStacksWithCount.put(c, allItemStacksWithCount.get(c) - i.getCount());
                        if (allItemStacksWithCount.get(c) == 0)
                            allItemStacksWithCount.remove(c);
                    }
                }

                // now reset all the entries for this blockentity
                filteredItemStacksMap_reference.removeByValue(e.getBlockPos());
                fullItemStacksMap_copy.removeByValue(e.getBlockPos());
                filteredItemStacksMap_copy.removeByValue(e.getBlockPos());

                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack stackInSlot = itemHandler.getStackInSlot(i);
                    if(stackInSlot.isEmpty()) {
                        // this entry will be used later to check for inventory change
                        // do not use .copy on empty stack, this will not work! it will return ItemStack.EMPTY as of 1.21.1
                        fullItemStacksMap_copy.put(new ItemStack(Items.AIR, 0), e.getBlockPos());
                    }else{
                        // because some blocks like chests can have 2 positions and 2 itemhandler,
                        // this can cause items to be scanned and added double.
                        // because of this, check if the reference to this itemstack is already registered
                        // and only add it if it is not.
                        // because the references to all itemstacks addedby this inventory are cleared above,
                        // if there is stil a reference this indicates that the itemstack was already added by another blockentity on a shared inventory
                        if(filteredItemStacksMap_reference.getFromKey(stackInSlot) == null) {
                            // add the reference for future scans
                            filteredItemStacksMap_reference.put(stackInSlot, e.getBlockPos());
                            // add the copy to revert adding to the full map on the next inventory change
                            filteredItemStacksMap_copy.put(stackInSlot.copy(), e.getBlockPos());
                        }
                        // add a copy of all itemstacks in the inventory to later detect changes
                        fullItemStacksMap_copy.put(stackInSlot.copy(), e.getBlockPos());
                    }
                }

                //  this part adds the filtered scanned stacks to the full map. this is the map that shows itemstack + count for all items
                for (ItemStack i : filteredItemStacksMap_copy.getFromvalue(e.getBlockPos())) {
                    if (!i.isEmpty()) {
                        ComparableItemStack c = new ComparableItemStack(i);
                        allItemStacksWithCount.putIfAbsent(c, 0);
                        allItemStacksWithCount.put(c, allItemStacksWithCount.get(c) + i.getCount());
                    }
                }
            }
        } else {
            removeBlockEntityInventory(e.getBlockPos());
        }

    }
}
