package ARMachines.lathe;


import ARLib.utils.MachineRecipe;
import ARLib.utils.RecipePartWithProbability;
import ARMachines.ARMachines;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class LatheConfig {

    public static LatheConfig INSTANCE = loadConfig();

    public List<MachineRecipe> recipes = new ArrayList<>();

    public void addRecipe(MachineRecipe r) {
        if (r.inputs.isEmpty()) return;
        recipes.add(r);
        System.out.println("Created Lathe recipe for input: " + r.inputs + " with " + r.outputs.size() + " output items");
    }

    public void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new LatheConfig.PacketConfigSync(new Gson().toJson(this)));
        }
    }

    public static LatheConfig loadConfig() {

        if (ServerLifecycleHooks.getCurrentServer() == null) return new LatheConfig();

        String recipesDirName = "lathe_recipes";
        Class<MachineRecipe> recipeClass = MachineRecipe.class;
        MachineRecipe recipe;
        System.out.println("load lathe config");
        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString(), ARMachines.MODID);
        Path configRecipesDir = configDir.resolve(recipesDirName);

        try {
            // Create the config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            // Load recipes from the recipesDirName directory
            if (!Files.exists(configRecipesDir)) {
                Files.createDirectories(configRecipesDir);
                System.out.println("Recipes directory created: " + configRecipesDir);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LatheConfig config = new LatheConfig();

        // load recipes from config
        DirectoryStream<Path> stream = null;
        try {
            stream = Files.newDirectoryStream(configRecipesDir, "*.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Path recipeFile : stream) {
            try {
                String recipeContent = Files.readString(recipeFile);
                Gson gson = new Gson();
                recipe = gson.fromJson(recipeContent, recipeClass);
                for (RecipePartWithProbability i : recipe.outputs) {
                    // if no p set, i set it to 1
                    if (i.p == 0) {
                        i.p = 1;
                        System.out.println(recipeFile + " - output with id " + i.id + " has no probability set or it is set to 0. It will default to one.");
                    }
                }
                for (RecipePartWithProbability i : recipe.inputs) {
                    // if no p set, i set it to 1
                    if (i.p == 0) {
                        i.p = 1;
                        System.out.println(recipeFile + " - input with id " + i.id + " has no probability set or it is set to 0. It will default to one.");
                    }
                }

                config.addRecipe(recipe);
                System.out.println("Loaded recipe: " + recipeFile.getFileName());
            } catch (JsonSyntaxException e) {
                System.err.println("Failed to parse JSON, skipping recipe file: " + recipeFile.getFileName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return config;
    }

    public static class PacketConfigSync implements CustomPacketPayload {

        public static final Type<PacketConfigSync> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ARMachines.MODID, "packet_lathe_config_sync"));


        public PacketConfigSync(String config) {
            this.config = config;
        }

        String config;

        public String getConfig() {
            return config;
        }


        public static final StreamCodec<ByteBuf, PacketConfigSync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                PacketConfigSync::getConfig,
                PacketConfigSync::new
        );

        // this is for jei to load the recipes after sync
        public static Runnable jeiRunnableOnConfigLoad = null;

        public static void readClient(final PacketConfigSync data, final IPayloadContext context) {
            String config = data.getConfig();
            LatheConfig.INSTANCE = new Gson().fromJson(config, LatheConfig.class);
            System.out.println("client loaded lathe config:" + config);
            if (jeiRunnableOnConfigLoad != null) {
                jeiRunnableOnConfigLoad.run();
            }
        }

        public static void readServer(final PacketConfigSync data, final IPayloadContext context) {
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void register(PayloadRegistrar registrar) {
            registrar.playBidirectional(
                    PacketConfigSync.TYPE,
                    PacketConfigSync.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            PacketConfigSync::readClient,
                            PacketConfigSync::readServer
                    )
            );
        }
    }
}
