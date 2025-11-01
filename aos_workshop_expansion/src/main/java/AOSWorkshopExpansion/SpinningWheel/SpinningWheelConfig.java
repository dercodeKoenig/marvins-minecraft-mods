package AOSWorkshopExpansion.SpinningWheel;

import AOSWorkshopExpansion.ConfigUtils;
import AOSWorkshopExpansion.Main;
import AOSWorkshopExpansion.WoodMill.WoodMillConfig;
import ARLib.network.SimpleNetworkPacket;
import ARLib.utils.RecipePartWithProbability;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static AOSWorkshopExpansion.Main.configDir;

public class SpinningWheelConfig implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static SpinningWheelConfig INSTANCE = ConfigUtils.loadConfig(SpinningWheelConfig.class, "spinning_wheel.json");

    public static String packetConfigSyncID = Main.MODID + "packet_spinningwheel_config";

    public float baseResistance;
    public float k;
    public float clickForce;
    public List<SpinningWheelRecipe> recipes = new ArrayList<>();

    public static Runnable jeiRunnableOnConfigLoad = null;

    public SpinningWheelConfig() {
            for (SpinningWheelConfig.SpinningWheelRecipe i : ConfigUtils.loadRecipes(SpinningWheelConfig.SpinningWheelRecipe.class, "spinning_wheel_recipes")) {
                addRecipe(i);
            }
    }

    public void addRecipe(SpinningWheelRecipe r) {
        for (RecipePartWithProbability i : r.outputItems) {
            if (i.p == 0) {
                i.p = 1;
            }
        }
        if (r.inputItem.p == 0) {
            r.inputItem.p = 1;
        }
        if (r.inputItem.id.isEmpty()) return;
        for (SpinningWheelRecipe i : recipes) {
            if (Objects.equals(i.inputItem.id, r.inputItem.id)) {
                i.outputItems.addAll(r.outputItems);
                System.out.println("Added " + r.outputItems.size() + " outputs to Spinning Wheel recipe for input: " + r.inputItem.id);
                return;
            }
        }
        recipes.add(r);
        System.out.println("Created Spinning Wheel recipe for input: " + r.inputItem.id + " with " + r.outputItems.size() + " output items");
    }

    public static void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket(packetConfigSyncID, new Gson().toJson(SpinningWheelConfig.INSTANCE)));
        }
    }

    public void readClient(String data) {
        SpinningWheelConfig.INSTANCE = new Gson().fromJson(data, SpinningWheelConfig.class);
        System.out.println("client loaded spinningwheel config:" + data);
        if (jeiRunnableOnConfigLoad != null) {
            jeiRunnableOnConfigLoad.run();
        }
    }


    public static class SpinningWheelRecipe {
        public RecipePartWithProbability inputItem = new RecipePartWithProbability("");
        public List<RecipePartWithProbability> outputItems = new ArrayList<>();
        public float timeRequired = 3f;
        public float additionalResistance = 5f;
    }

}
