package advRocketry.Items;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.PlanetDimensionProperties;
import advRocketry.Registry.GasRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

public class ItemAtmAnalyzer extends Item {


    public ItemAtmAnalyzer() {
        super(new Properties().stacksTo(1));
    }


    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide) return InteractionResultHolder.success(player.getItemInHand(usedHand));

        if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {

            String description = String.format("\n\n");

            description += String.format("Composition:\n");

            for (String gas : GasRegistry.gases.keySet()) {
                PlanetDimensionProperties.GasProperty prop = planet.getGasProperty(gas);
                if (prop.in_atm > 0)
                    description += "g  " + String.format("%-10s", gas) + String.format("%.5f", prop.in_atm) + "\n";
                if (prop.liquid > 0)
                    description += "l   " + String.format("%-10s", gas) + String.format("%.5f", prop.liquid) + "\n";
                if (prop.frozen_surface > 0)
                    description += "s  " + String.format("%-10s", gas) + String.format("%.5f", prop.frozen_surface) + "\n";
                if (prop.underground > 0)
                    description += "u " + String.format("%-10s", gas) + String.format("%.5f", prop.underground) + "\n";
            }
            description += "\n";

            description += String.format("Temperature: " + planet.getCurrentTemp() + "\n\n");
            description += String.format("Water level: " + planet.getGasProperty(GasRegistry.water).getSeaLevel() + "\n\n");

            Set<Dimension.SurvivalProblem> problems = planet.getSurvivalProblems();
            if (problems.isEmpty()) {
                description += "Survival Possible";
            } else {
                description += "Survival problems: \n";
                for (Dimension.SurvivalProblem p : problems) {
                    description += p.reason + "\n";
                }
            }

            player.sendSystemMessage(Component.literal(description));
        }

        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }
}