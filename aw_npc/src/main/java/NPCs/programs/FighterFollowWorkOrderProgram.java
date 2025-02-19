package NPCs.programs;

import NPCs.CombatNPC;
import NPCs.Items.ItemWorkOrder;
import NPCs.NPCBase;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;

import static NPCs.Utils.*;

public class FighterFollowWorkOrderProgram extends Goal {

    CombatNPC worker;
    long lastCheck = 0;
    long lastCheckTick = 0;
    boolean canUse;
    int currentIndex = 0;
    long timeArrivedAtLocation = 0;

    public FighterFollowWorkOrderProgram(CombatNPC worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (worker.level().getGameTime() < lastCheck + 20 * 5) {
            return canUse;
        }
        lastCheck = worker.level().getGameTime();

        if (worker.hunger < worker.maxHunger * 0.05) {
            canUse = false;
            return false;
        }
        ItemStack order = worker.ordersStackHandler.getStackInSlot(0);
        if (order.getItem() instanceof ItemWorkOrder) {
            if (!ItemWorkOrder.getBlockList(order).isEmpty()) {
                canUse = true;
                return true;
            }
        }
        canUse = false;
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    @Override
    public void tick() {
        if (worker.level().getGameTime() < lastCheckTick + 20 * 1) {
            return;
        }
        lastCheckTick = worker.level().getGameTime();

        ItemStack order = worker.ordersStackHandler.getStackInSlot(0);
        if (order.isEmpty() || !(order.getItem() instanceof ItemWorkOrder)) {
            canUse = false;
            return;
        }
        List<ItemWorkOrder.vec3> blockList = ItemWorkOrder.getBlockList(order);
        if (blockList.isEmpty()) {
            canUse = false;
            return;
        }

        if (currentIndex >= blockList.size()) {
            currentIndex = 0;
        }
        ItemWorkOrder.vec3 v = blockList.get(currentIndex);
        BlockPos target = new BlockPos(v.x, v.y, v.z);
        //System.out.println(target);

        int moveExit = worker.slowMobNavigation.moveToPosition(target, 0, worker.slowNavigationMaxDistance, worker.slowNavigationMaxNodes, worker.slowNavigationStepPerTick);
        if (moveExit == EXIT_FAIL) {
            currentIndex++;
            return;
        }
        if (moveExit == SUCCESS_STILL_RUNNING) {
            timeArrivedAtLocation = -1;
            return;
        }

        if (moveExit == EXIT_SUCCESS) {
            if (timeArrivedAtLocation == -1)
                timeArrivedAtLocation = worker.level().getGameTime();

            if(worker.level().getGameTime() > timeArrivedAtLocation + 20*10){
                currentIndex++;
            }
            return;
        }
    }
}
