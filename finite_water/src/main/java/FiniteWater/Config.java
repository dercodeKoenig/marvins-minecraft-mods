package FiniteWater;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {

    public static Config INSTANCE = loadConfig();

    public String _comment = "when isBlackList is set to false, only biomes in the list are allowed to form water sources. If isBlackList is set to true, only biomes in the list are prevented from forming water sources.";
    public boolean isBlackList = false;
    public Set<String> biomes = new HashSet<>();

    public Config() {
        biomes.add("minecraft:river");
        biomes.add("minecraft:frozen_river");

        biomes.add("minecraft:ocean");
        biomes.add("minecraft:frozen_ocean");
        biomes.add("minecraft:cold_ocean");
        biomes.add("minecraft:lukewarm_ocean");
        biomes.add("minecraft:warm_ocean");

        biomes.add("minecraft:deep_ocean");
        biomes.add("minecraft:deep_frozen_ocean");
        biomes.add("minecraft:deep_cold_ocean");
        biomes.add("minecraft:deep_lukewarm_ocean");

        biomes.add("minecraft:swamp");

        biomes.add("minecraft:snowy_beach");
        biomes.add("minecraft:stony_shore");
        biomes.add("minecraft:beach");

    }

    public static Config loadConfig() {
        String filename = "finite_water.json";
        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString());
        Path filePath = configDir.resolve(filename);

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                String json = gson.toJson(new Config());
                Files.writeString(filePath, json);
                System.out.println("Created finite_water config file: " + filePath);
            }
            String json = Files.readString(filePath);
            Config c = gson.fromJson(json, Config.class);
            System.out.println("Loaded finite_water config file: " + filePath);
            return c;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
