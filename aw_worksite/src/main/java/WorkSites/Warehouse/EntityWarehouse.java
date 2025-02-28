package WorkSites.Warehouse;

import ARLib.gui.ModularScreen;
import ARLib.gui.modules.*;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockIdentifier;
import WorkSites.EntityWorkSiteBase;
import WorkSites.WarehouseInterface.EntityWarehouseInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static WorkSites.Registry.ENTITY_WAREHOUSE;

public class EntityWarehouse extends EntityWorkSiteBase {

    public static Set<BlockIdentifier> knownWarehouses = new HashSet<>();

    int currentBlockToScanIndex_blocks = 0;
    int currentBlockToScanIndex_inventories = 0;

    guiModuleScrollContainer scrollContainer;

    public EntityWarehouse(BlockPos pos, BlockState blockState) {
        super(ENTITY_WAREHOUSE.get(), pos, blockState);


        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 210, 500, 0, 2, guiHandlerMain)) {
            guiHandlerMain.getModules().add(m);
        }
        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 150, 600, 0, 2, guiHandlerMain)) {
            guiHandlerMain.getModules().add(m);
        }

        scrollContainer = new guiModuleScrollContainer(new ArrayList<>(), 0xf0f0f0, guiHandlerMain, 7, 30, 166, 230);
        guiHandlerMain.getModules().add(scrollContainer);

    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(!level.isClientSide){
            updateGuiSlots();
        }
        knownWarehouses.add(new BlockIdentifier(getLevel(), getBlockPos()));
    }

    @Override
    public void setRemoved() {
        knownWarehouses.remove(new BlockIdentifier(getLevel(), getBlockPos()));
        super.setRemoved();
    }

    public void scanStep() {
        // this will scan for new inventories one block a tick
        // it also scans for interfaces and places itself as the warehouse reference to it
        if (!allowedBlocksList.isEmpty() && allowedBlocks.size() != knownInventoriesList.size()) {
            if (currentBlockToScanIndex_blocks >= allowedBlocksList.size()) {
                currentBlockToScanIndex_blocks = 0;
            }
            BlockPos nextPosToScan = allowedBlocksList.get(currentBlockToScanIndex_blocks);
            currentBlockToScanIndex_blocks += 1;

            if (!knownInventories.containsKey(nextPosToScan)) {
                BlockEntity be = level.getBlockEntity(nextPosToScan);
                if (be != null) {
                    if(be instanceof EntityWarehouseInterface warehouseInterface){
                        warehouseInterface.warehouseReference = this;
                    }
                    else{
                        IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, nextPosToScan, be.getBlockState(), be, Direction.UP);
                        if (inventory != null) {
                            addBlockEntityInventory(be);
                        }
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
/*
            if (level.getGameTime() % 100 == 0) {
               for (int i = 0; i < myItemHandler.getSlots(); i++) {
                System.out.println(myItemHandler.getStackInSlot(i));
            }
            }

 */
        }
    }

    @Override
    public void openMainGui() {
        if (level.isClientSide) {
            guiHandlerMain.openGui(180, 240, true);
            CompoundTag request = new CompoundTag();
            request.put("requestSlotNum", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, request));
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
        knownInventoriesList.clear();
        filteredItemStacksMap_reference.clear();
        filteredItemStacksMap_copy.clear();
        fullItemStacksMap_copy.clear();
        allItemStacksWithCount.clear();
        whereItemStacksComeFrom.clear();
    }


    public WarehouseItemHandler myItemHandler = new WarehouseItemHandler(this);

    // all the detected inventories are here
    public HashMap<BlockPos, BlockEntity> knownInventories = new HashMap<>();
    public List<BlockEntity> knownInventoriesList = new ArrayList<>(); // same but as a list

    // This one is to hold references to all the scanned ItemStacks.
    // It is used to check if a itemstack was already processed.
    // (this happens because a chest has 2 itemhandlers but both return the same itemstacks)
    public BiDirectionalMultiMap<ItemStack, BlockPos> filteredItemStacksMap_reference = new BiDirectionalMultiMap<>();

    // This one will also be added only once for any ItemStack, but it holds a copy.
    // The copy is used to remove the previous ItemStacks from the total sum of all ItemStacks during scanning.
    // To avoid rescanning the entire map to get the sum of all items, members of this map will be subtracted from the total map
    // before scanning and re-added after scanning.
    public BiDirectionalMultiMap<ItemStack, BlockPos> filteredItemStacksMap_copy = new BiDirectionalMultiMap<>();

    // this one holds every itemstack in the correct order as it is in the inventory to detect changes
    public BiDirectionalMultiMap<ItemStack, BlockPos> fullItemStacksMap_copy = new BiDirectionalMultiMap<>();

    // final map with all items and count
    // also referenced as "full map" in comments below
    // because the system never makes a full re-scan of the entire inventory at once (to save a lot of time),
    // you should not write to this yourself. you risk making the map out of sync with the inventories and it will
    // never recover until world reload when out of sync. so if you modify anything here, make sure you know what you are doing
    // (dont touch it and it should work)
    public Map<ComparableItemStack, Integer> allItemStacksWithCount = new HashMap<>();

    // caches the last x blockentities where an itemstack was found to speed up insertion and extraction
    // a blockentity in here may no longer contain the item or is completely removed
    public Map<ComparableItemStack, LinkedHashSet<BlockEntity>> whereItemStacksComeFrom = new HashMap<>();

    // insertion and extraction can be compute expensive on large storage areas.
    // if another program wants to insert into this inventory and it fails,
    // the program needs not to try again until this value changes indicating the inventory has changed
    public long lastContentUpdateTime = 0;

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
        if (itemHandler != null) {
            // check if anything in the inventory has changed since last scanning
            // if nothing is changed, the set should be exactly as the itemhandler by item, components and count
            LinkedHashSet<ItemStack> lastContents = fullItemStacksMap_copy.getFromvalue(e.getBlockPos());
            boolean needsRescan = false;
            if (lastContents.size() != itemHandler.getSlots()) {
                needsRescan = true;
            } else {
                List<ItemStack> cachedStackList = lastContents.stream().toList();
                for (int i = 0; i < cachedStackList.size(); i++) {
                    if (!ItemStack.isSameItemSameComponents(cachedStackList.get(i), itemHandler.getStackInSlot(i)) || cachedStackList.get(i).getCount() != itemHandler.getStackInSlot(i).getCount()) {
                        needsRescan = true;
                        break;
                    }
                }
            }
            if (needsRescan) {
                int numSlotsBefore = myItemHandler.getSlots();
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
                    if (stackInSlot.isEmpty()) {
                        // this entry will be used later to check for inventory change
                        // do not use .copy on empty stack, this will not work! it will return ItemStack.EMPTY as of 1.21.1
                        fullItemStacksMap_copy.put(new ItemStack(Items.AIR, 0), e.getBlockPos());
                    } else {
                        // because some blocks like chests can have 2 positions and 2 itemhandler,
                        // this can cause items to be scanned and added double.
                        // because of this, check if the reference to this itemstack is already registered
                        // and only add it if it is not.
                        // because the references to all itemstacks addedby this inventory are cleared above,
                        // if there is stil a reference this indicates that the itemstack was already added by another blockentity on a shared inventory
                        if (filteredItemStacksMap_reference.getFromKey(stackInSlot) == null) {
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

                        whereItemStacksComeFrom.computeIfAbsent(c, (k1) -> new LinkedHashSet<>());
                        whereItemStacksComeFrom.get(c).add(e);
                        if (whereItemStacksComeFrom.get(c).size() > 20) {
                            whereItemStacksComeFrom.get(c).removeFirst();
                        }
                    }
                }
                int numSlotsAfter = myItemHandler.getSlots();
                if (numSlotsAfter != numSlotsBefore) {
                    updateGuiSlots();
                }
                lastContentUpdateTime = level.getGameTime();
                //System.out.println(e.getBlockPos()+" was rescanned");
            }
        } else {
            removeBlockEntityInventory(e.getBlockPos());
        }

    }

    public void notifyPlayersOfSlotNum(ServerPlayer p) {
        CompoundTag infoTag = new CompoundTag();
        infoTag.putInt("slotNum", myItemHandler.getSlots());
        if (p != null) {
            PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, infoTag));
        } else {
            if (level instanceof ServerLevel l) {
                for (UUID id : guiHandlerMain.playersTrackingGui.keySet()) {
                    Player _p = l.getPlayerByUUID(id);
                    if (_p instanceof ServerPlayer sp) {
                        PacketDistributor.sendToPlayer(sp, PacketBlockEntity.getBlockEntityPacket(this, infoTag));
                    }
                }
            }
        }
    }

    public void updateGuiSlots() {
        scrollContainer.modules.clear();
        for (int i = 0; i < myItemHandler.getSlots(); i++) {
            // because the guihandler will try to insert in any slot of this group,
            // but the itemhandler does not care about the index and will scan the entire area if required
            // if you have the group id on all slots it can in worst case do a full rescan for EVERY slot currently in the inventory
            // so only make the correct group id on the last (or any) slot
            int inventoryGroup =(i+1 == myItemHandler.getSlots() )? 2 : 1;
            guiModuleItemHandlerSlot slot = new guiModuleItemHandlerSlot(10000 + i, myItemHandler, i, inventoryGroup, 0, guiHandlerMain, 0, 0);
            scrollContainer.modules.add(slot);
        }
        notifyPlayersOfSlotNum(null);
    }

    public void updateGuiSlotsClient(int numSlots) {
        scrollContainer.modules.clear();
        for (int i = 0; i < numSlots; i++) {
            int x = i % 9 * 18;
            int y = i / 9 * 18 + 10;
            guiModuleItemHandlerSlot slot = new guiModuleItemHandlerSlot(10000 + i, new ItemStackHandler(1), 0, 1, 0, guiHandlerMain, x, y);
            scrollContainer.modules.add(slot);
        }
        if (guiHandlerMain.screen instanceof ModularScreen m) {
            m.calculateGuiOffsetAndNotifyModules();
        }

        CompoundTag request = new CompoundTag();
        request.put("updateSlotContents", new CompoundTag());
        PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, request));
    }

    public void sortSlotsByCount() {
        TreeSet<guiModuleItemHandlerSlot> sortedByCount = new TreeSet<>(
                new Comparator<guiModuleItemHandlerSlot>() {
                    @Override
                    public int compare(guiModuleItemHandlerSlot o1, guiModuleItemHandlerSlot o2) {
                        int diff = o1.client_getItemStackToRender().getCount() - o2.client_getItemStackToRender().getCount();

                        if (diff != 0) {
                            return -diff; // Sort descending by count
                        }

                        String name1 = o1.client_getItemStackToRender().getHoverName().getString();
                        String name2 = o2.client_getItemStackToRender().getHoverName().getString();

                        int nameDiff = name1.compareToIgnoreCase(name2);
                        if (nameDiff != 0) {
                            return nameDiff; // Sort alphabetically if count is the same
                        }

                        return Integer.compare(
                                ItemStack.hashItemAndComponents(o1.client_getItemStackToRender()),
                                ItemStack.hashItemAndComponents(o2.client_getItemStackToRender())
                        ); // Fallback to hash comparison
                    }
                }
        );

        // sort slots by count
        for (GuiModuleBase i : scrollContainer.modules) {
            if (i instanceof guiModuleItemHandlerSlot is) {
                sortedByCount.add(is);
            }
        }
        ArrayList<guiModuleItemHandlerSlot> sortedList = new ArrayList<>(sortedByCount);
        for (int i = 0; i < sortedList.size(); i++) {
            int x = i % 9 * 18;
            int y = i / 9 * 18 + 10;
            sortedList.get(i).x = x;
            sortedList.get(i).y = y;
        }
        if (guiHandlerMain.screen instanceof ModularScreen m)
            m.calculateGuiOffsetAndNotifyModules();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {
        super.readServer(compoundTag, p);
        if (compoundTag.contains("requestSlotNum")) {
            notifyPlayersOfSlotNum(p);
        }
        if (compoundTag.contains("updateSlotContents")) {
            CompoundTag guiData = new CompoundTag();
            for (GuiModuleBase guiModule : scrollContainer.modules) {
                guiModule.server_writeDataToSyncToClient(guiData);
            }
            PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, guiData));
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        super.readClient(compoundTag);
        if (compoundTag.contains("slotNum")) {
            updateGuiSlotsClient(compoundTag.getInt("slotNum"));
        }

        // if a slot was updated, sort by count again
        for (GuiModuleBase i : scrollContainer.modules) {
            if (compoundTag.contains(i.getMyTagKey())) {
                sortSlotsByCount();
                break;
            }
        }
    }
}
