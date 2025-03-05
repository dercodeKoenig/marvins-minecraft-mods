package Vehicles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    public static Config INSTANCE = loadConfig();
    public float ballista_damage = 50f;
    public float ballista_life = 20f;
    public float ballista_reload_speed = 0.01f;

    public Config() {
    }

    public static Config loadConfig() {
        String filename = "ballista_config.json";
        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString());
        Path filePath = configDir.resolve(filename);

        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                Files.write(filePath, (new GsonBuilder()).setPrettyPrinting().create().toJson(new Config()).getBytes(StandardCharsets.UTF_8));
            }

            String jsonContent = Files.readString(filePath);
            Gson gson = new Gson();
            return gson.fromJson(jsonContent, Config.class);
        } catch (JsonSyntaxException e) {
            System.err.println("Failed to parse config JSON");
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
