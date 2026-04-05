package advRocketry.LifeSupport;

import advRocketry.Dimension.Dimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SurvivalSystem {
    public static final HashMap<Class<?>, ICanSurvive> survivalData = new HashMap<>();
    private static final ICanSurvive NO_OP_RULE = (e, level, pos, problems) -> {
    };

    static {
        survivalData.put(Player.class, (e, level, pos, problems) -> {
            if (e instanceof Player player) {
                if (player.isCreative() || player.isSpectator())
                    problems.clear();

                // check if it has armor
                // check and consume oxygen...
            }
        });
    }

    public static ICanSurvive getSurvivalRule(LivingEntity e) {
        Class<? extends LivingEntity> entityClass = e.getClass();

        // 1. Check if we already know the rule for this specific class
        ICanSurvive rule = survivalData.get(entityClass);
        if (rule != null) return rule;

        // 2. If not, find a matching rule from parent class
        for (Map.Entry<Class<?>, ICanSurvive> entry : survivalData.entrySet()) {
            if (entry.getKey().isAssignableFrom(entityClass)) {
                rule = entry.getValue();

                // 3. CACHE IT!
                // Next time a 'ServerPlayer' or 'Zombie' checks, it hits step 1.
                survivalData.put(entityClass, rule);
                System.out.println("cache rule for " + entityClass.getName() + " - " + rule.toString());
                return rule;
            }
        }

        survivalData.put(entityClass, NO_OP_RULE); // Cache the "nothing found" result
        System.out.println("cache no_op rule for " + entityClass.getName());
        return NO_OP_RULE;
    }

    public interface ICanSurvive {
        void trySurvive(LivingEntity e, Level level, BlockPos pos, Set<Dimension.SurvivalProblem> problems);

        default boolean allowInMobSpawn(Mob e, ServerLevel level, double x, double y, double z, Set<Dimension.SurvivalProblem> problems) {
            return problems.isEmpty();
        }
    }
}
