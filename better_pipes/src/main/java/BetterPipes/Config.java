package BetterPipes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;

public class Config {
    static Path configFilePath = Paths.get(String.valueOf(FMLPaths.CONFIGDIR.get()) ).resolve("better_pipes.json");

    public static Config INSTANCE = loadConfig();

    public int flowRateIronPipe = 50;
    public int mainCapacityIronPipe = 500;

    public int flowRateWoodenPipe = 20;
    public int mainCapacityWoodenPipe = 200;

    public double autoPumpForce = 100.0;
    public double autoPumpSpeedConstant = 10.0;
    public double autoPumpInertia = 5.0;

    public static Config loadConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            if (!Files.exists(configFilePath)) {
                Files.createFile(configFilePath);
                String json = gson.toJson(new Config());
                Files.writeString(configFilePath, json);
                System.out.println("Created BetterPipes config file: " + configFilePath);
            }
            String json = Files.readString(configFilePath);
            Config c = gson.fromJson(json, Config.class);
            System.out.println("Loaded BetterPipes config file: " + configFilePath);
            return c;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}