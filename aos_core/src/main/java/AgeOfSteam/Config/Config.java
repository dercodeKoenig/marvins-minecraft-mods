package AgeOfSteam.Config;

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

    public double wooden_axle_friction = 0.1;
    public double wooden_axle_inertia = 0.1;
    public double wooden_axle_max_stress = 600;


    public double wooden_flywheel_friction = 0.1;
    public double wooden_flywheel_inertia = 20;
    public double wooden_flywheel_max_stress = 600;


    public double wooden_flywheel_large_friction = 0.1;
    public double wooden_flywheel_large_inertia = 500;
    public double wooden_flywheel_large_max_stress = 600;


    public double wooden_clutch_friction_per_side = 0.1;
    public double wooden_clutch_inertia_per_side = 3;
    public double wooden_clutch_max_stress = 900;
    public double wooden_clutch_max_force = 590;


    public double wooden_crankshaft_big_friction = 0.2;
    public double wooden_crankshaft_big_inertia = 2;
    public double wooden_crankshaft_big_max_stress = 600;


    public double wooden_crankshaft_small_friction = 0.2;
    public double wooden_crankshaft_small_inertia = 2;
    public double wooden_crankshaft_small_max_stress = 600;


    public double wooden_distributor_gearbox_friction = 2;
    public double wooden_distributor_gearbox_inertia = 1;
    public double wooden_distributor_gearbox_max_stress = 600;


    public double wooden_gearbox_friction = 2;
    public double wooden_gearbox_inertia = 1;
    public double wooden_gearbox_max_stress = 900;


    public double wooden_t_junction_friction = 2;
    public double wooden_t_junction_inertia = 1;
    public double wooden_t_junction_max_stress = 600;


    public double hand_generator_friction = 0.1;
    public double hand_generator_inertia = 5;
    public double hand_generator_max_stress = 600;
    public double hand_generator_max_force = 50;
    public double hand_generator_max_speed = 10;

    public double motor_rf_multiplier = 1.5;


    public void readClient(String data) {
        Config.INSTANCE = new Gson().fromJson(data, Config.class);
        System.out.println("client load AOS config:" + data);
    }

    public void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket("aos_config_sync", new Gson().toJson(this)));
        }
    }

    public static Config loadConfig() {
        String filename = "age_of_steam_config.json";
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
            Gson gson = new Gson();
            return gson.fromJson(jsonContent, Config.class);
        } catch (JsonSyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}

