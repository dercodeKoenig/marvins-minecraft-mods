package NPCs.Npc.programs;

import ARLib.utils.BlockIdentifier;
import NPCs.Items.ItemRoutingOrder;
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

import static NPCs.Utils.*;

public class WorkRoutingOrderProgram extends Goal {

    long lastCheck = 0;

    public WorkerNPC worker;
    public int timeoutForWorkCheck = 20 * 10;

    boolean isLoading = false;

    UnloadInventoryProgram unloadInventoryProgram;
    TakeFromInventoryProgram takeFromInventoryProgram;

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


    @Override
    public boolean canUse() {

        if (worker.level().isNight()) return false;

        long gameTime = worker.level().getGameTime();
        if (lastCheck + timeoutForWorkCheck > gameTime)
            return false;
        lastCheck = gameTime;

        ItemStack stack = worker.ordersStackHandler.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemRoutingOrder routingOrder) {
System.out.println(getStackTagOrEmpty(stack));
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {

    }
}