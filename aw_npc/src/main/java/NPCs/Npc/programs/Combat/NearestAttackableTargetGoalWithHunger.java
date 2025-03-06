package NPCs.Npc.programs.Combat;

import NPCs.Npc.CombatNPC;
import NPCs.Npc.NPCBase;
import NPCs.Npc.programs.TakeToolProgram;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.item.SwordItem;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static NPCs.Npc.CombatNPC.DATA_WORKTYPE;

public class NearestAttackableTargetGoalWithHunger<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    CombatNPC npc;

    public NearestAttackableTargetGoalWithHunger(CombatNPC npc, Class<T> targetType, int randomInterval, boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> targetPredicate) {
        super(npc,
                targetType,
                randomInterval,
                mustSee,
                mustReach,
                targetPredicate);
        this.npc = npc;

    }


    @Override
    public boolean canUse() {
        boolean c=  super.canUse() && npc.hunger > npc.maxHunger * 0.05;
        return c;
    }
}
