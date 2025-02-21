package NPCs.Npc.programs;

import NPCs.Npc.HostileEntities;
import NPCs.Npc.NPCBase;
import NPCs.Blocks.TownHall.TownHallOwners;
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
                if (!HostileEntities.isUnableToAttack(e, worker)) {
                    npcNeedHelp.add(e);
                }
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return !npcNeedHelp.isEmpty() && worker.hunger > worker.maxHunger * 0.05;
    }

    @Override
    public void tick() {
        if (npcNeedHelp.isEmpty()) return;

        List<Entity> allEntities =npcNeedHelp.stream().toList();
        for (Entity e : allEntities) {
            if (!e.isAlive())
                npcNeedHelp.remove(e);
        }

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
                3, worker.slowNavigationMaxDistance, worker.slowNavigationMaxNodes, worker.slowNavigationStepPerTick,1.2f
        );
        if (moveExit != SUCCESS_STILL_RUNNING) {
            npcNeedHelp.remove(bp_to_entity.get(toGo));
        }
    }
}
