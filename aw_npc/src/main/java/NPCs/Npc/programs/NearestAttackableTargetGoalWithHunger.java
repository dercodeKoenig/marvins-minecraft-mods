package NPCs.Npc.programs;

import NPCs.Npc.NPCBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class NearestAttackableTargetGoalWithHunger<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    NPCBase npc;

    public NearestAttackableTargetGoalWithHunger(NPCBase npc, Class<T> targetType, int randomInterval, boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> targetPredicate) {
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
        return super.canUse() && npc.hunger > npc.maxHunger * 0.05;
    }
}
