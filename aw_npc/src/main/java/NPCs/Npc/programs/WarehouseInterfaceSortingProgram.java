package NPCs.Npc.programs;

import ARLib.utils.BlockIdentifier;
import NPCs.Npc.WorkerNPC;
import NPCs.Npc.programs.CropFarming.CropFarmingProgram;
import NPCs.Npc.programs.CropFarming.UnloadInventoryToFarmProgram;
import NPCs.Npc.programs.CropFarming.UseMillStoneProgram;
import NPCs.Utils;
import WorkSites.CropFarm.EntityCropFarm;
import WorkSites.EntityWorkSiteBase;
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
                EntityWarehouse.ComparableItemStack c = new EntityWarehouse.ComparableItemStack(stack);
                BlockEntity target = WarehouseItemHandler.getBlockEntityWhereStackIsInsertable(c, warehouseInterface.warehouseReference);
                if (target != null)
                    return true;
            }
        }

        return false;
    }

    @Override
    public boolean canUse() {

        if (worker.level().isNight()) return false;

        long gameTime = worker.level().getGameTime();
        if (lastCheck + timeoutForWorkCheck > gameTime)
            return false;
        lastCheck = gameTime;

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
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return worker.lastWorksitePosition != null && !worker.level().isNight();
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


        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            ItemStack stack = worker.combinedInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                EntityWarehouse.ComparableItemStack c = new EntityWarehouse.ComparableItemStack(stack);

                if (warehouseInterface.nextStackToInsert != null) {
                    if (warehouseInterface.nextStackToInsert.equals(c)) {
                        int exit = unloadInventoryProgram.run(warehouseInterface.inventory, warehouseInterface.getBlockPos(), c.stack);
                        if (exit == EXIT_FAIL) {
                            return EXIT_FAIL;
                        }
                        return SUCCESS_STILL_RUNNING;
                    }
                }


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


        if (!warehouseInterface.nextStackToRemove.isEmpty()) {
            int exit = takeFromInventoryProgram.run(warehouseInterface.inventory, warehouseInterface.getBlockPos(), warehouseInterface.nextStackToRemove);
            if (exit == EXIT_FAIL) {
                return EXIT_FAIL;
            }
            return SUCCESS_STILL_RUNNING;
        }


        if (warehouseInterface.nextStackToInsert != null) {
            BlockEntity target = WarehouseItemHandler.getBlockEntityContainingItemStack(warehouseInterface.nextStackToInsert, warehouseInterface.warehouseReference);
            if (target != null) {
                IItemHandler inv = worker.level().getCapability(Capabilities.ItemHandler.BLOCK, target.getBlockPos(), target.getBlockState(), target, Direction.UP);
                if (inv != null) {
                    int exit = takeFromInventoryProgram.run(inv, target.getBlockPos(), warehouseInterface.nextStackToInsert.stack);
                    if (exit == EXIT_FAIL) {
                        return EXIT_FAIL;
                    }
                    return SUCCESS_STILL_RUNNING;
                }
            }
        }

        return EXIT_SUCCESS;
    }
}