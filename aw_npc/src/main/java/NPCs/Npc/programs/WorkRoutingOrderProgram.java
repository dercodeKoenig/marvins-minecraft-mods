package NPCs.Npc.programs;

import ARLib.utils.BlockIdentifier;
import NPCs.Items.ItemRoutingOrder;
import NPCs.Items.RoutingEntry;
import NPCs.Npc.WorkerNPC;
import NPCs.Utils;
import WorkSites.Warehouse.ComparableItemStack;
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
import java.util.List;
import java.util.Map;

import static NPCs.Utils.*;

public class WorkRoutingOrderProgram extends Goal {

    long lastCheck = 0;

    public WorkerNPC worker;
    public int timeoutForWorkCheck = 20 * 10;

    UnloadInventoryProgram unloadInventoryProgram;
    TakeFromInventoryProgram takeFromInventoryProgram;

    BlockEntity target;
    Direction targetFace;
    Map<ComparableItemStack, Integer> toExtract;
    Map<ComparableItemStack, Integer> toInsert;

    int currentIndex;

    public WorkRoutingOrderProgram(WorkerNPC worker) {
        this.worker = worker;
        unloadInventoryProgram = new UnloadInventoryProgram(worker);
        takeFromInventoryProgram = new TakeFromInventoryProgram(worker);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    public BlockEntity computeWork(RoutingEntry i) {
        toInsert = null;
        toExtract = null;
        BlockEntity e = worker.level().getBlockEntity(new BlockPos(i.posX, i.posY, i.posZ));
        if (e != null) {
            IItemHandler itemHandler = worker.level().getCapability(Capabilities.ItemHandler.BLOCK, e.getBlockPos(), e.getBlockState(), e, Direction.values()[i.facingOrdinal]);
            if (itemHandler != null) {
               Map<ComparableItemStack, Integer> _toExtract = i.getStacksToExtract(itemHandler, worker.combinedInventory);
                Map<ComparableItemStack, Integer> _toInsert = i.getStacksToInsert(itemHandler, worker.combinedInventory);
                if(!_toInsert.isEmpty())
                    toInsert = _toInsert;
                if(!_toExtract.isEmpty())
                    toExtract = _toExtract;

                if(!_toExtract.isEmpty() || !_toInsert.isEmpty())
                    return e;
                        /*
                        for(ComparableItemStack c : toExtract.keySet())
                            System.out.println(e.getBlockPos()+": toInsert :" + c.stack+":"+toExtract.get(c));
                        for(ComparableItemStack c : toInsert.keySet())
                            System.out.println(e.getBlockPos()+": toExtract :" +c.stack+":"+toInsert.get(c));
                         */
            }
        }
        return null;
    }

    @Override
    public boolean canUse() {

        if (worker.level().isNight()) return false;

        long gameTime = worker.level().getGameTime();
        if (lastCheck + timeoutForWorkCheck > gameTime)
            return false;
        lastCheck = gameTime;

        ItemStack stack = worker.ordersStackHandler.getStackInSlot(0);
        List<RoutingEntry> entries = ItemRoutingOrder.getRoutingEntries(stack, worker.level().registryAccess());
        if (!stack.isEmpty() && stack.getItem() instanceof ItemRoutingOrder routingOrder) {
            for (int n = 0; n < entries.size(); n++) {
                currentIndex++;
                if (currentIndex >= entries.size())
                    currentIndex = 0;

                BlockEntity e = computeWork(entries.get(currentIndex));
                if (e != null) {
                    target = e;
                    targetFace = Direction.values()[entries.get(currentIndex).facingOrdinal];
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null;
    }

    @Override
    public void tick() {
        if(target == null)
            return;
        if(target.isRemoved()) {
            target = null;
            return;
        }
        IItemHandler itemHandler = worker.level().getCapability(Capabilities.ItemHandler.BLOCK, target.getBlockPos(), target.getBlockState(), target, targetFace);
        if (itemHandler != null) {
            if (toInsert != null) {
                int exit = unloadInventoryProgram.run(itemHandler,target.getBlockPos(),toInsert.keySet().stream().toList().getFirst().stack);
                if(exit == SUCCESS_STILL_RUNNING || exit == EXIT_SUCCESS)
                    return;
            }
            if (toExtract != null) {
                int exit = takeFromInventoryProgram.run(itemHandler,target.getBlockPos(),toExtract.keySet().stream().toList().getFirst().stack);
                if(exit == SUCCESS_STILL_RUNNING || exit == EXIT_SUCCESS)
                    return;
            }
        }
        target = null;
    }
}