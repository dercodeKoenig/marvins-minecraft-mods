package advRocketry.LifeSupport;

import advRocketry.Dimension.Dimension;
import advRocketry.Items.ItemPortablePressureTank;
import advRocketry.Registry.Fluids;
import advRocketry.SpaceSuit.ISpaceSuitInventory;
import advRocketry.SpaceSuit.SpaceSuit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

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
                if (player.isCreative() || player.isSpectator()) {
                    problems.clear();
                    return;
                }

                boolean hasFullSuit = true;
                for (EquipmentSlot slot : new EquipmentSlot[]{
                        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
                }) {
                    if (!(player.getItemBySlot(slot).getItem() instanceof SpaceSuit))
                        hasFullSuit = false;
                }
                if (hasFullSuit) {
                    // space suit can remove most problems easily
                    problems.remove(Dimension.SurvivalProblem.TOO_COLD);
                    problems.remove(Dimension.SurvivalProblem.TOO_HOT);
                    problems.remove(Dimension.SurvivalProblem.TOO_MUCH_PRESSURE);
                    problems.remove(Dimension.SurvivalProblem.TOO_LOW_PRESSURE);
                    problems.remove(Dimension.SurvivalProblem.TOO_MUCH_O2);
                    problems.remove(Dimension.SurvivalProblem.TOO_MUCH_CO2);

                    // o2 requires a pressure tank with o2
                    if (problems.contains(Dimension.SurvivalProblem.TOO_LITTLE_O2)) {
                        ItemStack chestPlate = player.getItemBySlot(EquipmentSlot.CHEST);
                        CompoundTag cached = ((ISpaceSuitInventory)chestPlate.getItem()).getCachedData(chestPlate);
                        int oxygenAvailable = cached.getInt("oxygen");
                        int oxygenRequired = 20;
                        if (oxygenAvailable >= oxygenRequired) {
                            problems.remove(Dimension.SurvivalProblem.TOO_LITTLE_O2);
                            // remove oxyhen from tanks and save back to the space suit
                            int toDrain = oxygenRequired;
                            ItemStackHandler inventory = ISpaceSuitInventory.loadInventory(chestPlate, player.registryAccess());
                            for (int i = 0; i < inventory.getSlots(); i++) {
                                ItemStack stack = inventory.getStackInSlot(i);
                                if (stack.getItem() instanceof ItemPortablePressureTank) {
                                    IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
                                    if (fluidHandler.getFluidInTank(0).getFluid().equals(Fluids.OXYGEN.get())) {
                                        toDrain -= fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE).getAmount();
                                    }
                                }
                            }
                            ISpaceSuitInventory.saveInventory(inventory, chestPlate, player.registryAccess());
                        }
                    }
                }
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
