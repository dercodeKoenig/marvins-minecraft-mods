package AOSWorkshopExpansion.MillStone;

import AOSWorkshopExpansion.ConfigUtils;
import AOSWorkshopExpansion.Main;
import ARLib.network.SimpleNetworkPacket;
import ARLib.utils.RecipePart;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MillStoneConfig implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static MillStoneConfig INSTANCE = ConfigUtils.loadConfig(MillStoneConfig.class, "millstone.json");

    public static String packetConfigSyncID = Main.MODID + "packet_millstone_config";

    public float resistance = 100;
    public List<MillStoneRecipe> recipes = new ArrayList<>();

    public static Runnable jeiRunnableOnConfigLoad = null;


    public MillStoneConfig() {
            for (MillStoneConfig.MillStoneRecipe i : ConfigUtils.loadRecipes(MillStoneConfig.MillStoneRecipe.class, "millstone_recipes")) {
                addRecipe(i);
            }
    }


    public void addRecipe(MillStoneRecipe r) {
        if(r.inputItem.id.isEmpty())return;
        for (MillStoneRecipe i : recipes) {
            if (Objects.equals(i.inputItem.id, r.inputItem.id)) {
                    System.out.println("recipe for input "+r.inputItem.id+" already exists and produces "+i.outputItem.id+". This recipe will be skipped");
                return;
            }
        }
        recipes.add(r);
        System.out.println("Created Millstone recipe for input: " + r.inputItem.id + " with output " + r.outputItem.id+" "+r.outputItem.amount);
    }
    public static void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket(packetConfigSyncID, new Gson().toJson(MillStoneConfig.INSTANCE)));
        }
    }
    public void readClient(String data) {
        MillStoneConfig.INSTANCE = new Gson().fromJson(data, MillStoneConfig.class);
        System.out.println("client loaded millstone config:" + data);
        if(jeiRunnableOnConfigLoad != null){
            jeiRunnableOnConfigLoad.run();
        }
    }

    public static class MillStoneRecipe {
        public RecipePart inputItem =new RecipePart("");
        public RecipePart outputItem = new RecipePart("");
        public float timeRequired = 3f;
    }
}