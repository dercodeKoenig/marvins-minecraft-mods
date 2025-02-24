package ResearchSystem.Config;

import ARLib.utils.RecipePart;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class RecipeConfig {

    public static RecipeConfig INSTANCE = loadConfig();

    public static class Recipe {
        public String requiredResearch = "";
        public RecipePart output = new RecipePart("minecraft:air",0);
        public List<String> pattern = new ArrayList<>();
        public Map<String,RecipeInput> keys = new HashMap<>();
    }
    public static class RecipeInput{
        public RecipePart input = new RecipePart("",1);
        public RecipePart onComplete = new RecipePart("",0);
        public RecipeInput(RecipePart in, RecipePart out){
            this.input = in;this.onComplete = out;
        }
        public RecipeInput(RecipePart in){
            this(in, new RecipePart(""));
        }
    }

    public List<Recipe> recipeList = new ArrayList<>();

    public static String[] shrink(List<String> pattern) {
        int i = Integer.MAX_VALUE;
        int j = 0;
        int k = 0;
        int l = 0;

        for(int i1 = 0; i1 < pattern.size(); ++i1) {
            String s = (String)pattern.get(i1);
            i = Math.min(i, firstNonSpace(s));
            int j1 = lastNonSpace(s);
            j = Math.max(j, j1);
            if (j1 < 0) {
                if (k == i1) {
                    ++k;
                }

                ++l;
            } else {
                l = 0;
            }
        }

        if (pattern.size() == l) {
            return new String[0];
        } else {
            String[] astring = new String[pattern.size() - l - k];

            for(int k1 = 0; k1 < astring.length; ++k1) {
                astring[k1] = ((String)pattern.get(k1 + k)).substring(i, j + 1);
            }

            return astring;
        }
    }

     static int firstNonSpace(String row) {
        int i;
        for(i = 0; i < row.length() && row.charAt(i) == ' '; ++i) {
        }

        return i;
    }

     static int lastNonSpace(String row) {
        int i;
        for(i = row.length() - 1; i >= 0 && row.charAt(i) == ' '; --i) {
        }

        return i;
    }


    public RecipeConfig() {}

    public void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new PacketConfigSync(new Gson().toJson(this)));
        }
    }

    // because i can not call the direct jei method from here (the class may not be found if not installed),
    // i let the plugin insert a runnable here and i execute it on recipe load.
    // this way it should not crash when jei is not found.
    public static Set<Runnable> jeiRunnableOnConfigLoad = new HashSet<>();

    public void loadConfig(String configString) {
        RecipeConfig.INSTANCE = new Gson().fromJson(configString, RecipeConfig.class);
        System.out.println("client load config:" + configString);
        for( Runnable i : jeiRunnableOnConfigLoad) {
            i.run();
        }
    }

    public static RecipeConfig loadConfig() {
        String folderName = "research_recipes";
        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString());
        Path folderPath = configDir.resolve(folderName);

        RecipeConfig recipeConfig = new RecipeConfig();

        try {
            // Create the folder if it doesn't exist
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
                System.out.println("Created recipes folder: " + folderPath);
            }

            // Scan for recipe files in the folder
            DirectoryStream<Path> recipeFiles = Files.newDirectoryStream(folderPath, "*.json");
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .excludeFieldsWithModifiers(Modifier.PRIVATE)
                    .create();

            for (Path filePath : recipeFiles) {
                try {
                    // Read each recipe file
                    String jsonContent = Files.readString(filePath);
                    Recipe recipe = gson.fromJson(jsonContent, Recipe.class); // Assuming individual recipe files correspond to a Recipe class

                    if (recipe != null) {
                        recipeConfig.recipeList.add(recipe); // Add the parsed recipe to RecipeConfig
                        System.out.println("added recipe:"+filePath.getFileName());
                    }
                } catch (JsonSyntaxException e) {
                    System.err.println("Failed to parse recipe file: " + filePath);
                    e.printStackTrace();
                } catch (IOException e) {
                    System.err.println("Failed to read recipe file: " + filePath);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error accessing recipes folder", e);
        }

        return recipeConfig;
    }





    public static class PacketConfigSync implements CustomPacketPayload {

        public static final Type<PacketConfigSync> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("research_station", "packet_engineering_config_sync"));


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

        public static void readClient(final PacketConfigSync data, final IPayloadContext context) {
            String config = data.getConfig();
            RecipeConfig.INSTANCE.loadConfig(config);
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


