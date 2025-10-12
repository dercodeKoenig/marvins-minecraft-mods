package AWGenerators.Config;

import ARLib.network.SimpleNetworkPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Config implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static Config INSTANCE = loadConfig();

    public double stirlingGenerator_maxForceMultiplier = 100;
    public double stirlingGenerator_k = 10;
    public double stirlingGenerator_burnTimeMultiplier = 1;
    public double stirlingGenerator_friction = 1;
    public double stirlingGenerator_inertia = 20;
    public double stirlingGenerator_maxStress = 100000;


    public double waterWheel_inertia = 100;
    public double waterWheel_maxStress = 100000;
    public double waterWheel_friction = 1;
    public double waterWheel_maxForceMultiplier = 120;
    public double waterWheel_k = 120;


    public double windmill_forcePerBlock = 0.125;
    public double windmill_windSpeedMultiplier = 10;
    public double windmill_frictionPerBlock = 0.005;
    public double windmill_inertiaPerBlock = 1;
    public double windmill_maxStress = 100000;
    public int windmill_maxSize = 18;

    public static String ConfigPacketId = "aw_generators_config_sync";


    public void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket(ConfigPacketId, new Gson().toJson(this)));
        }
    }

    public void readClient(String data) {
        Config.INSTANCE = new Gson().fromJson(data, Config.class);
        System.out.println("load config:" + data);
    }


    public static Config loadConfig() {
        String filename = "aw_generators_main_config.json";
        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString());
        Path filePath = configDir.resolve(filename);
        try {
            // Create the config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                Files.write(filePath, new GsonBuilder().setPrettyPrinting().create().toJson(new Config()).getBytes(StandardCharsets.UTF_8));
            }
            // Load JSON from the file
            String jsonContent = Files.readString(filePath);
            return new Gson().fromJson(jsonContent, Config.class);
        } catch (JsonSyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}


