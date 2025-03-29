package ARMachines.rollingMachine;


import ARLib.network.SimpleNetworkPacket;
import ARLib.utils.MachineRecipe;
import ARLib.utils.RecipePartWithProbability;
import ARMachines.RecipeLoader;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

public class RollingMachineConfig {

    public static RollingMachineConfig INSTANCE = new RollingMachineConfig();

    public List<MachineRecipe> recipes = new ArrayList<>();

    public RollingMachineConfig() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            recipes = RecipeLoader.loadRecipes("rollingmachine");
            if (recipes.isEmpty()) {
                List<MachineRecipe> defaultRecipes = makeDefaultRecipes();
                RecipeLoader.saveRecipes("rollingmachine", defaultRecipes);
            }
            recipes = RecipeLoader.loadRecipes("rollingmachine");
            System.out.println(recipes.size() + " recipes loaded for rollingmachine");
        }
    }

    public static void SyncConfig(ServerPlayer p) {
        if (p != null) {
            String config = new Gson().toJson(RollingMachineConfig.INSTANCE);
            System.out.println("send config to player:" + config);
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket("rollingmachineConfigSync", config));
        }
    }

    // this is for jei to load the recipes after sync
    public static Runnable jeiRunnableOnConfigLoad = null;

    public static class configReceiver implements SimpleNetworkPacket.SimpleNetworkDataReceiver {
        public void readClient(String config) {
            RollingMachineConfig.INSTANCE = new Gson().fromJson(config, RollingMachineConfig.class);
            System.out.println("client loaded rollingmachine config:" + config);
            if (jeiRunnableOnConfigLoad != null) {
                jeiRunnableOnConfigLoad.run();
            }
        }
    }


    List<MachineRecipe> makeDefaultRecipes() {
        List<MachineRecipe> recipes = new ArrayList<>();
        MachineRecipe r1 = new MachineRecipe();
        r1.inputs.add(new RecipePartWithProbability("c:ingots/iron", 1, 1));
        r1.outputs.add(new RecipePartWithProbability("minecraft:gold_ingot", 1, 1));
        r1.ticksRequired = 50;
        r1.energyPerTick = 20;
        recipes.add(r1);

        return recipes;
    }
}
