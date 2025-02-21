package NPCs.Npc.programs;


import NPCs.Blocks.TownHall.TownHallOwners;
import NPCs.Npc.NPCBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Objects;

public class NPCHurtByTargetProgram extends TargetGoal {
    private static final TargetingConditions HURT_BY_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

    private int timestamp;
    NPCBase worker;

    public NPCHurtByTargetProgram(NPCBase worker, boolean mustSee, boolean mustReach) {
        super(worker, mustSee, mustReach);
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    public boolean canUse() {
        int i = this.mob.getLastHurtByMobTimestamp();
        LivingEntity livingentity = this.mob.getLastHurtByMob();
        if (i != this.timestamp && livingentity != null) {
            if(livingentity instanceof Player player) {
                String pName = player.getName().getString();
                if (Objects.equals(worker.owner, pName))
                    return false;
                if (worker.townHall != null && TownHallOwners.getOwners(worker.level(), worker.townHall).contains(pName))
                    return false;
            }
            if(livingentity instanceof NPCBase otherNPC) {
                String pName = otherNPC.owner;
                if (Objects.equals(worker.owner, pName))
                    return false;
                if (worker.townHall != null && TownHallOwners.getOwners(worker.level(), worker.townHall).contains(pName))
                    return false;
            }
            return this.canAttack(livingentity, HURT_BY_TARGETING);
        }
        return false;
    }

    public void start() {
        this.mob.setTarget(this.mob.getLastHurtByMob());
        this.targetMob = this.mob.getTarget();
        this.timestamp = this.mob.getLastHurtByMobTimestamp();
        this.unseenMemoryTicks = 300;

        super.start();
    }
}