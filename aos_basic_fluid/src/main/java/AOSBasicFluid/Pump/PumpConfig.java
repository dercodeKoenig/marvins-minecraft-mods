package AOSBasicFluid.Pump;

import FiniteWater.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class PumpConfig {
        public static PumpConfig INSTANCE = loadConfig();

        public boolean consumeWater = true;
        int maxRadius = 96;
        int scanPerTick = 1000;
        int tankCapacity = 10000;
        double resistance = 60.0;

        public static PumpConfig loadConfig() {
            String filename = "mechanical_pump.json";
            Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString());
            Path filePath = configDir.resolve(filename);

            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                if (!Files.exists(filePath)) {
                    Files.createFile(filePath);
                    String json = gson.toJson(new PumpConfig());
                    Files.writeString(filePath, json);
                    System.out.println("Created Mechanical Pump config file: " + filePath);
                }
                String json = Files.readString(filePath);
                PumpConfig c = gson.fromJson(json, PumpConfig.class);
                System.out.println("Loaded Mechanical Pump config file: " + filePath);
                return c;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
}
