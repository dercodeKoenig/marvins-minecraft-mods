package NPCs.programs;

import NPCs.NPCBase;
import NPCs.Utils;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Objects;

import static NPCs.Utils.*;

public class SlowMobNavigation {
    public NPCBase npc;
    int failTimeOut = 0;
    int removeInvalidTargetsTime = 20 * 60;
    HashMap<BlockPos, Long> unreachableBlocks = new HashMap<>();

    SlowPathFinder pathFinder;

    public SlowMobNavigation(NPCBase npc) {
        this.npc = npc;
        this.pathFinder = new SlowPathFinder(this.npc);
    }

    public boolean isPositionCachedAsInvalid(BlockPos target) {
        if (unreachableBlocks.containsKey(target)) {
            if (unreachableBlocks.get(target) + removeInvalidTargetsTime < npc.level().getGameTime()) {
                unreachableBlocks.remove(target);
            } else {
                //System.out.println(target);
                return true;
            }
        }
        return false;
    }

    public int moveToPosition(BlockPos target, int precision, int maxDistace, int maxNodesVisit, int steps, float speed) {
        //System.out.println("move to "+target+":"+precision);

        if (target == null) return EXIT_FAIL;

        double distToTarget = Utils.distanceManhattan(npc, target.getCenter());
        if (distToTarget <= precision + 1.5) {
            return EXIT_SUCCESS;
        }
        if (isPositionCachedAsInvalid(target)) {
            return EXIT_FAIL;
        }

        if (npc.getNavigation().getPath() == null ||
                !Objects.equals(npc.getNavigation().getPath().getTarget(), target) ||
                npc.getNavigation().isDone()
        ) {
            failTimeOut = 0;
            //long t0 = System.nanoTime();
            SlowPathFinder.PathFindExit exit = pathFinder.findPath(target, maxDistace, precision, maxNodesVisit, steps);
            //long t1 = System.nanoTime();
            //System.out.println("navigator exit code: "+exit.exitCode+":"+(double)(t1-t0) / 1000 / 1000);

            if (exit.exitCode == EXIT_FAIL) {
                // target can not be reached
                unreachableBlocks.put(target, npc.level().getGameTime());
                return EXIT_FAIL;
            }
            if (exit.exitCode == SUCCESS_STILL_RUNNING) {
                return SUCCESS_STILL_RUNNING;
            }
            if (exit.exitCode == EXIT_SUCCESS) {
                npc.getNavigation().moveTo(exit.path, speed);
                return SUCCESS_STILL_RUNNING;
            }
        }


        if (npc.getNavigation().isStuck()) {
            failTimeOut++;
            if (failTimeOut > 50 && npc.getNavigation().isStuck()) {
                // try to jump if we are stuck
                npc.getJumpControl().jump();
            }
            if (failTimeOut > 200) {
                npc.getNavigation().stop();
                unreachableBlocks.put(target, npc.level().getGameTime());
                return EXIT_FAIL;
            }
        } else {
            failTimeOut = 0;
        }
        return SUCCESS_STILL_RUNNING;
    }

    public int moveToPosition(BlockPos target, int precision, int maxDistace, int maxNodesVisit, int steps) {
        return moveToPosition(target, precision, maxDistace, maxNodesVisit, steps, 1);
    }
}
