package advRocketry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientConfig {

    public static ClientConfig INSTANCE = loadConfig();

    // noise rendering is extremely expensive, so make it configurable
    public int planet_Cloud_Noise_Samples = 5; // can be 0 for no clouds
    public boolean planet_Cloud_Noise_Warp = true;

    public static ClientConfig loadConfig() {
        if (!FMLLoader.getDist().isClient()) return new ClientConfig();
        Path configDir = Path.of(FMLPaths.CONFIGDIR.get().toString(), Main.MODID);
        Path filePath = configDir.resolve("client_config.json");
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, new GsonBuilder().setPrettyPrinting().create().toJson(new ClientConfig()));
            }
            String jsonContent = Files.readString(filePath);
            return new Gson().fromJson(jsonContent, ClientConfig.class);
        } catch (JsonSyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
