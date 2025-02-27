package NPCs.Npc.programs;

import ARLib.utils.BlockIdentifier;
import NPCs.Npc.WorkerNPC;
import NPCs.Npc.programs.CropFarming.CropFarmingProgram;
import NPCs.Npc.programs.CropFarming.UnloadInventoryToFarmProgram;
import NPCs.Npc.programs.CropFarming.UseMillStoneProgram;
import NPCs.Utils;
import WorkSites.CropFarm.EntityCropFarm;
import WorkSites.EntityWorkSiteBase;
import WorkSites.Warehouse.ComparableItemStack;
import WorkSites.Warehouse.EntityWarehouse;
import WorkSites.Warehouse.WarehouseItemHandler;
import WorkSites.WarehouseInterface.EntityWarehouseInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.EnumSet;

import static NPCs.Utils.*;

public class WarehouseInterfaceSortingProgram extends Goal {

    long lastCheck = 0;

    public WorkerNPC worker;
    public int timeoutForWorkCheck = 20 * 10;

    boolean isLoading = false;

    // because as soon as the worker removes the item from the inventory,
    // the interface thinks it is no longer available and removes it as a request.
    // so cache it.
    // the worker needs to remember the requested item and it will be reset when it was failed to be inserted or
    // when the total number of requested items was inserted
    ComparableItemStack lastRequestedItem;

    UnloadInventoryProgram unloadInventoryProgram;
    TakeFromInventoryProgram takeFromInventoryProgram;

    public WarehouseInterfaceSortingProgram(WorkerNPC worker) {
        this.worker = worker;
        unloadInventoryProgram = new UnloadInventoryProgram(worker);
        takeFromInventoryProgram = new TakeFromInventoryProgram(worker);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    public boolean hasWorkAtInterface(BlockPos p) {

        BlockEntity e = worker.level().getBlockEntity(p);
        if (!(e instanceof EntityWarehouseInterface warehouseInterface)) return false;
        if (warehouseInterface.warehouseReference == null) return false;

        if (!warehouseInterface.nextStackToRemove.isEmpty()) return true;
        if (warehouseInterface.nextStackToInsert != null) return true;


        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            ItemStack stack = worker.combinedInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ComparableItemStack c = new ComparableItemStack(stack);
                BlockEntity target = WarehouseItemHandler.getBlockEntityWhereStackIsInsertable(c, warehouseInterface.warehouseReference);
                if (target != null)
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean canUse() {

        long gameTime = worker.level().getGameTime();
        if (lastCheck + timeoutForWorkCheck > gameTime)
            return false;
        lastCheck = gameTime;

        if(worker.ordersStackHandler.getStackInSlot(0) != ItemStack.EMPTY)
            // if he has a routing order do not sort the warehouse,
            return false;

        // make sure he does not just switch to this worksite while another worksite is active (if last position != null)
        // except he can switch to this program if the last worksite was of this program (eg after sleep, server restart)
        if (worker.lastWorksitePosition != null) {
            if (worker.level().isLoaded(worker.lastWorksitePosition)) {
                BlockEntity worksite = worker.level().getBlockEntity(worker.lastWorksitePosition);
                if (worksite instanceof EntityWarehouseInterface w) {
                    return true;
                }
            }
            return false;
        }

        for (BlockIdentifier p : Utils.sortBlockIdentifiersByDistanceToVec(EntityWarehouseInterface.knownWarehouseInterfaces, worker.level(), worker.getPosition(0))) {

            if (Utils.distanceManhattan(worker, p.pos.getCenter()) > 256) break;

            BlockEntity worksite = worker.level().getBlockEntity(p.pos);
            if (worksite instanceof EntityWarehouseInterface w) {

                if (!w.workersWorkingHereWithTimeout.isEmpty())
                    continue;

                if (hasWorkAtInterface(p.pos)) {
                    worker.lastWorksitePosition = p.pos;
                    w.workersWorkingHereWithTimeout.put(worker, 0);
                    lastRequestedItem = null;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return worker.lastWorksitePosition != null;
    }

    @Override
    public void tick() {
        long t0 = System.nanoTime();
        int e = run();
        long t1 = System.nanoTime();
        //System.out.println((double)(t1-t0) / 1000 / 1000);
        if (e == EXIT_SUCCESS || e == EXIT_FAIL) worker.lastWorksitePosition = null;
    }

    public int run() {
        if (worker.lastWorksitePosition == null) return EXIT_FAIL;

        BlockEntity e = worker.level().getBlockEntity(worker.lastWorksitePosition);
        if (!(e instanceof EntityWarehouseInterface warehouseInterface)) return EXIT_FAIL;
        if (warehouseInterface.warehouseReference == null) return EXIT_SUCCESS;

        warehouseInterface.workersWorkingHereWithTimeout.put(worker, 0);

        if (lastRequestedItem == null && warehouseInterface.nextStackToInsert != null) {
            lastRequestedItem = new ComparableItemStack(warehouseInterface.nextStackToInsert.stack);
            lastRequestedItem.stack.setCount(warehouseInterface.nextStackToInsert.stack.getCount());
        }

        if (!isLoading) {
            if (lastRequestedItem != null) {
                for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
                    ItemStack stack = worker.combinedInventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ComparableItemStack c = new ComparableItemStack(stack);
                        if (lastRequestedItem.equals(c)) {
                            int exit = unloadInventoryProgram.run(warehouseInterface.inventory, warehouseInterface.getBlockPos(), c.stack);
                            
                            if (exit == EXIT_FAIL) {
                                lastRequestedItem = null;
                                return EXIT_FAIL;
                            }if(exit == EXIT_SUCCESS) {
                                lastRequestedItem.stack.shrink(1);
                                if (lastRequestedItem.stack.isEmpty()) lastRequestedItem = null;
                            }
                            return SUCCESS_STILL_RUNNING;
                        }
                    }
                }
            }
        }
        int emptySlots = countEmptySlots(worker);
        if (lastRequestedItem != null && (emptySlots > 5 || isLoading) && countItems(lastRequestedItem.stack.getItem(), worker.combinedInventory) < lastRequestedItem.stack.getCount()) {
            BlockEntity target = WarehouseItemHandler.getBlockEntityContainingItemStack(lastRequestedItem, warehouseInterface.warehouseReference);
            if (target != null) {
                IItemHandler inv = worker.level().getCapability(Capabilities.ItemHandler.BLOCK, target.getBlockPos(), target.getBlockState(), target, Direction.UP);
                if (inv != null) {
                    isLoading = true;
                    int exit = takeFromInventoryProgram.run(inv, target.getBlockPos(), lastRequestedItem.stack);
                    if (exit == EXIT_FAIL) {
                        isLoading = false;
                    }
                    return SUCCESS_STILL_RUNNING;
                }
            }
        }


        if (!warehouseInterface.nextStackToRemove.isEmpty() && (emptySlots > 5 || isLoading)) {
            isLoading = true;
            int exit = takeFromInventoryProgram.run(warehouseInterface.inventory, warehouseInterface.getBlockPos(), warehouseInterface.nextStackToRemove);
            if (exit == EXIT_FAIL) {
                isLoading = false;
            }
            return SUCCESS_STILL_RUNNING;
        }

        if (isLoading) {
            isLoading = false;
            return SUCCESS_STILL_RUNNING;
        }


        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            ItemStack stack = worker.combinedInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ComparableItemStack c = new ComparableItemStack(stack);

                BlockEntity target = WarehouseItemHandler.getBlockEntityWhereStackIsInsertable(c, warehouseInterface.warehouseReference);
                if (target != null) {
                    IItemHandler inv = worker.level().getCapability(Capabilities.ItemHandler.BLOCK, target.getBlockPos(), target.getBlockState(), target, Direction.UP);
                    if (inv != null) {
                        int exit = unloadInventoryProgram.run(inv, target.getBlockPos(), stack);
                        if (exit == EXIT_FAIL) {
                            return EXIT_FAIL;
                        }
                        return SUCCESS_STILL_RUNNING;
                    }
                }
            }
        }
        return EXIT_SUCCESS;
    }
}