package NPCs.programs;

import NPCs.NPCBase;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class MeleeAttackGoalWithHunger extends MeleeAttackGoal {
    NPCBase npc;
    public MeleeAttackGoalWithHunger(NPCBase npc, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(npc, speedModifier, followingTargetEvenIfNotSeen);
        this.npc = npc;
    }

    @Override
    public boolean canUse() {
        return super.canUse() && npc.hunger > npc.maxHunger * 0.05;
    }
}