package WorkSites;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Config {
    public static Config INSTANCE = loadConfig();

    public boolean allow_mechanical_energy = true;
    public int max_energy_storage = 10000;
    public double k = 10;

    public int crop_farm_energy_plant = 3000;
    public int crop_farm_energy_harvest = 3000;
    public int crop_farm_energy_boneMeal = 2000;

    public int energy_try_fish = 8000;

    public int energy_try_quarry = 8000;

    public int tree_farm_energy_plant = 4000;
    public int tree_farm_energy_harvest_leaves = 2000;
    public int tree_farm_energy_harvest_logs = 9000;
    public int tree_farm_energy_boneMeal = 2000;

    public static Config loadConfig() {
        String filename = "aw_worksites.json";
        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString());
        Path filePath = configDir.resolve(filename);

        try {
            if (!Files.exists(filePath, new LinkOption[0])) {
                Files.createFile(filePath);
                Files.write(filePath, (new GsonBuilder()).setPrettyPrinting().create().toJson(new Config()).getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
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