package NPCs.Npc.programs.Combat;


import NPCs.Npc.HostileEntities;
import NPCs.Npc.NPCBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.EnumSet;

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
            if (HostileEntities.isUnableToAttack(livingentity, worker))
                return false;
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