package NPCs.Npc.programs.Combat;

import NPCs.Npc.CombatNPC;
import NPCs.Npc.NPCBase;
import NPCs.Npc.programs.TakeToolProgram;
import NPCs.Utils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.SwordItem;

public class MeleeAttackGoalWithHunger extends MeleeAttackGoal {

    CombatNPC npc;
    public MeleeAttackGoalWithHunger(CombatNPC npc, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(npc, speedModifier, followingTargetEvenIfNotSeen);
        this.npc = npc;
    }

    @Override
    public boolean canUse() {
        return super.canUse() && npc.hunger > npc.maxHunger * 0.05;
    }

    @Override
    public void tick(){
        npc.takeWeaponProgram.takeBestWeaponToMainHand();
        super.tick();
    }

@Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (this.canPerformAttack(target)) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(target);
            Utils.damageMainHandItem(this.npc);
        }
    }
}