package ARMachines.lathe;


import ARLib.network.SimpleNetworkPacket;
import ARLib.utils.MachineRecipe;
import ARLib.utils.RecipePartWithProbability;
import ARMachines.ARMachines;
import ARMachines.RecipeLoader;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class LatheConfig {

    public static LatheConfig INSTANCE = new LatheConfig();

    public List<MachineRecipe> recipes = new ArrayList<>();

    public LatheConfig() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            recipes = RecipeLoader.loadRecipes("lathe");
            if (recipes.isEmpty()) {
                List<MachineRecipe> defaultRecipes = makeDefaultRecipes();
                RecipeLoader.saveRecipes("lathe", defaultRecipes);
            }
            recipes = RecipeLoader.loadRecipes("lathe");
            System.out.println(recipes.size() + " recipes loaded for lathe");
        }
    }

    public static void SyncConfig(ServerPlayer p) {
        if (p != null) {
            String config = new Gson().toJson(LatheConfig.INSTANCE);
            System.out.println("send config to player:" + config);
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket("latheConfigSync", config));
        }
    }

    // this is for jei to load the recipes after sync
    public static Runnable jeiRunnableOnConfigLoad = null;

    public static class configReceiver implements SimpleNetworkPacket.SimpleNetworkDataReceiver {
        public void readClient(String config) {
            LatheConfig.INSTANCE = new Gson().fromJson(config, LatheConfig.class);
            System.out.println("client loaded lathe config:" + config);
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
