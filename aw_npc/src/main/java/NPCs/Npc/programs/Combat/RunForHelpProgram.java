package NPCs.Npc.programs.Combat;

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
        return false;
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

        BlockPos toGo = null;
        Entity toGoE = null;
        double closestDistance = 99999;
        for (Entity e : npcNeedHelp) {
            double d = e.getPosition(0).distanceTo(worker.getPosition(0));
            if(d < closestDistance){
                closestDistance = d;
                toGoE = e;
                toGo = e.getOnPos();
            }
        }



        int moveExit = worker.slowMobNavigation.moveToPosition(
                toGo,
                3, worker.slowNavigationMaxDistance, worker.slowNavigationMaxNodes, worker.slowNavigationStepPerTick,1.2f
        );
        if (moveExit != SUCCESS_STILL_RUNNING) {
            npcNeedHelp.remove(toGoE);
        }
    }
}
