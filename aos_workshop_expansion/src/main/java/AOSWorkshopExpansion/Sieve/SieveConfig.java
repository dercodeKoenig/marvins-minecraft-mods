package AOSWorkshopExpansion.Sieve;

import AOSWorkshopExpansion.ConfigUtils;
import AOSWorkshopExpansion.Main;
import ARLib.network.SimpleNetworkPacket;
import ARLib.utils.RecipePart;
import ARLib.utils.RecipePartWithProbability;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SieveConfig implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static SieveConfig INSTANCE = ConfigUtils.loadConfig(SieveConfig.class, "sieve.json");

    public static String packetConfigSyncID = Main.MODID + "packet_sieve_config";

    public float baseResistance;
    public float k;
    public float clickForce;
    public List<SieveRecipe> recipes = new ArrayList<>();
    public int inventorySize;
    public int inventorySizeHopper;

    public static Runnable jeiRunnableOnConfigLoad = null;


    public SieveConfig() {
            for (SieveConfig.SieveRecipe i : ConfigUtils.loadRecipes(SieveConfig.SieveRecipe.class, "sieve_recipes")) {
                addRecipe(i);
            }
    }

    public void addRecipe(SieveRecipe r) {
        for (RecipePartWithProbability i : r.outputItems) {
            if (i.p == 0) {
                i.p = 1;
            }
        }
        if (r.inputItem.id.isEmpty()) return;
        if (r.requiredMesh.isEmpty()) return;
        for (SieveRecipe i : recipes) {
            if (Objects.equals(i.inputItem.id, r.inputItem.id) && Objects.equals(r.requiredMesh, i.requiredMesh)) {
                i.outputItems.addAll(r.outputItems);
                System.out.println("Added " + r.outputItems.size() + " outputs to sieve recipe for input: " + r.inputItem.id + ", " + r.requiredMesh);
                return;
            }
        }
        recipes.add(r);
        System.out.println("Created Sieve recipe for input: " + r.inputItem.id + ", " + r.requiredMesh + " with " + r.outputItems.size() + " output items");
    }

    public static void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket(packetConfigSyncID, new Gson().toJson(SieveConfig.INSTANCE)));
        }
    }

    public void readClient(String data) {
        SieveConfig.INSTANCE = new Gson().fromJson(data, SieveConfig.class);
        System.out.println("client loaded sieve config:" + data);
        if (jeiRunnableOnConfigLoad != null) {
            jeiRunnableOnConfigLoad.run();
        }
    }

    public static class SieveRecipe {
        public RecipePart inputItem = new RecipePart("");
        public List<RecipePartWithProbability> outputItems = new ArrayList<>();
        public float timeRequired = 3f;
        public float additionalResistance = 10f;
        public String requiredMesh = "";
    }
}
