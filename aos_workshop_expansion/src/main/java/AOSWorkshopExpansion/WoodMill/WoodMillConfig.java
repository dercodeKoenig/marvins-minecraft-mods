package AOSWorkshopExpansion.WoodMill;

import AOSWorkshopExpansion.ConfigUtils;
import AOSWorkshopExpansion.Main;
import ARLib.network.SimpleNetworkPacket;
import ARLib.utils.RecipePart;
import ARLib.utils.RecipePartWithProbability;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WoodMillConfig implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static WoodMillConfig INSTANCE = ConfigUtils.loadConfig(WoodMillConfig.class, "woodmill.json");

    public static String packetConfigSyncID = Main.MODID + "packet_woodmill_config";

    public float baseResistance;
    public float maxStress;

    public List<WoodMillRecipe> recipes = new ArrayList<>();
    public float speedMultiplier;

    public static Runnable jeiRunnableOnConfigLoad = null;

    public WoodMillConfig() {
            for(WoodMillRecipe i : ConfigUtils.loadRecipes(WoodMillRecipe.class,"woodmill_recipes")){
                addRecipe(i);
            }
    }

    public void addRecipe(WoodMillRecipe r) {
        for (RecipePartWithProbability i : r.outputItems) {
            if (i.p == 0) {
                i.p = 1;
            }
        }
        if (r.inputItem.id.isEmpty()) return;
        for (WoodMillRecipe i : recipes) {
            if (Objects.equals(i.inputItem.id, r.inputItem.id)) {
                i.outputItems.addAll(r.outputItems);
                System.out.println("Added " + r.outputItems.size() + " outputs to woodmill recipe for input: " + r.inputItem.id);
                return;
            }
        }
        recipes.add(r);
        System.out.println("Created WoodMill recipe for input: " + r.inputItem.id + " with " + r.outputItems.size() + " output items");
    }


    public static void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket(packetConfigSyncID, new Gson().toJson(WoodMillConfig.INSTANCE)));
        }
    }

    public void readClient(String data) {
        WoodMillConfig.INSTANCE = new Gson().fromJson(data, WoodMillConfig.class);
        System.out.println("client loaded woodmill config:" + data);
        if (jeiRunnableOnConfigLoad != null) {
            jeiRunnableOnConfigLoad.run();
        }
    }

    public static class WoodMillRecipe {
        public RecipePart inputItem = new RecipePartWithProbability("");
        public List<RecipePartWithProbability> outputItems = new ArrayList<>();
        public float additionalResistance = 10f;
    }
}
