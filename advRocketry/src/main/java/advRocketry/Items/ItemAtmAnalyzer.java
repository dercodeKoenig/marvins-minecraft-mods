package advRocketry.Items;

import ARLib.utils.RecipePartWithProbability;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.PlanetDimensionProperties;
import advRocketry.Main;
import advRocketry.Registry.GasRegistry;
import advRocketry.Utils.ItemUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ItemAtmAnalyzer extends Item {


    public ItemAtmAnalyzer() {
        super(new Properties().stacksTo(1));
    }


    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide) return InteractionResultHolder.success(player.getItemInHand(usedHand));

        if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {

            String description = String.format("\n\nWater level: " + planet.getGasProperty(GasRegistry.water).getSeaLevel() + "\n\n");


            description += String.format("Composition:\n");

            for (String gas : GasRegistry.gases.keySet()) {
                PlanetDimensionProperties.GasProperty prop = planet.getGasProperty(gas);
                if (prop.in_atm > 0)
                    description += "g  " + String.format("%-10s", gas) + String.format("%.5f", prop.in_atm) + "\n";
                if (prop.liquid > 0)
                    description += "l   " + String.format("%-10s", gas) + String.format("%.5f", prop.liquid) + "\n";
                if (prop.frozen_surface > 0)
                    description += "s  " + String.format("%-10s", gas) + String.format("%.5f", prop.frozen_surface) + "\n";
                if (prop.frozen_deep_below_surface > 0)
                    description += "sg " + String.format("%-10s", gas) + String.format("%.5f", prop.frozen_deep_below_surface) + "\n";
            }
            description += "\n";

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