package NPCs.Npc.programs;

import NPCs.Items.ItemRoutingOrder;
import NPCs.Items.RoutingEntry;
import NPCs.Npc.WorkerNPC;
import WorkSites.Warehouse.ComparableItemStack;
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

import static NPCs.Utils.EXIT_SUCCESS;
import static NPCs.Utils.SUCCESS_STILL_RUNNING;

public class ForgetLastWorksiteProgram extends Goal {

    public WorkerNPC worker;
    int timer = 0;

    // the worker remembers its last worksite position so that things dont go random every time the server restarts or a worksite program is interrupted.
    // but this can cause the worker to get stuck forever if the last worksite is no longer available.
    // this is why when all other programs can not run, this program will reset the worksite
    // this program should run after the worksite programs.
    // it should go to the worksite position and if it can not go there and another program does not take over after some time, forget the worksite position
    // if a worksite program ends, it should nullify the last position itself. this is only in case it gets stuck bc worksite is removed or job changed
    // in best case, it never runs

    public ForgetLastWorksiteProgram(WorkerNPC worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return worker.lastWorksitePosition != null;
    }

    @Override
    public void start(){
        timer = 0;
    }

    @Override
    public void tick(){
        if(worker.lastWorksitePosition != null) {
            int exit = worker.slowMobNavigation.moveToPosition(worker.lastWorksitePosition,5,worker.slowNavigationMaxDistance,worker.slowNavigationMaxNodes,worker.slowNavigationStepPerTick,1);
            if( exit != SUCCESS_STILL_RUNNING){
                timer ++;
                if(timer > 20*60){
                    worker.lastWorksitePosition = null;
                }
            }
        }
    }
}