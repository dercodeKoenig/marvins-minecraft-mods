package NPCs.programs;

import NPCs.NPCBase;
import NPCs.TownHall.TownHallOwners;
import NPCs.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.*;

import static NPCs.Utils.*;

public class RunForHelpProgram extends Goal {
    NPCBase worker;
    HashSet<Entity> npcNeedHelp = new HashSet<>();

    public RunForHelpProgram(NPCBase worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void requestHelp(NPCBase npc, Entity e) {
        if (Objects.equals(npc.owner, worker.owner) || (worker.townHall != null && TownHallOwners.getOwners(worker.level(), worker.townHall).contains(npc.owner))) {
            if (!worker.slowMobNavigation.isPositionCachedAsInvalid(npc.blockPosition())) {
                npcNeedHelp.add(e);
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return !npcNeedHelp.isEmpty();
    }

    @Override
    public void tick() {
        if (npcNeedHelp.isEmpty()) return;

        List<BlockPos> allPositionsToHelp = new ArrayList<>();
        Map<BlockPos, Entity> bp_to_entity = new HashMap<>();
        for (Entity e : npcNeedHelp) {
            allPositionsToHelp.add(e.blockPosition());
            bp_to_entity.put(e.blockPosition(), e);
        }

        BlockPos toGo = Utils.sortBlockPosByDistanceToNPC(allPositionsToHelp, worker).first();

        int moveExit = worker.slowMobNavigation.moveToPosition(
                toGo,
                3, worker.slowNavigationMaxDistance, worker.slowNavigationMaxNodes, worker.slowNavigationStepPerTick,1.5f
        );
        if (moveExit != SUCCESS_STILL_RUNNING) {
            npcNeedHelp.remove(bp_to_entity.get(toGo));
        }
    }
}
