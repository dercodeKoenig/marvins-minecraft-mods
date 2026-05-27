package advRocketry.LifeSupport;

import advRocketry.Dimension.Dimension;
import advRocketry.Items.ItemPortablePressureTank;
import advRocketry.Registry.Fluids;
import advRocketry.SpaceSuit.ISpaceSuitInventory;
import advRocketry.SpaceSuit.SpaceSuit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SurvivalSystem {
    public static final HashMap<EntityType<?>, ICanSurvive> survivalData = new HashMap<>();
    private static final ICanSurvive NO_OP_RULE = (e, level, pos, problems) -> {
    };

    static {
        survivalData.put(EntityType.PLAYER, (e, level, pos, problems) -> {
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
                        CompoundTag cached = ((ISpaceSuitInventory)chestPlate.getItem()).getCachedDataUnsafe(chestPlate);
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

    public static ICanSurvive getSurvivalRule(EntityType<?> type) {
        ICanSurvive rule = survivalData.get(type);
        if (rule != null) return rule;
        survivalData.put(type, NO_OP_RULE); // Cache the "nothing found" result
        System.out.println("cache no_op rule for " + type);
        return NO_OP_RULE;
    }

    public interface ICanSurvive {
        void trySurvive(Entity e, Level level, BlockPos pos, Set<Dimension.SurvivalProblem> problems);

        default boolean allowInMobSpawn(EntityType<?> type, ServerLevel level, int x, int y, int z, Set<Dimension.SurvivalProblem> problems) {
            return problems.isEmpty();
        }
    }
}
