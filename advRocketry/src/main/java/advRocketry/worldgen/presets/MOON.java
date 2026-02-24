package advRocketry.worldgen.presets;

import advRocketry.Main;
import advRocketry.worldgen.BiomeConfig;

import java.util.List;

public class MOON {
    public static String name = "moon.json";

    public static BiomeConfig create() {

        BiomeConfig config = new BiomeConfig();

        BiomeConfig.BiomeDefinition definition = new BiomeConfig.BiomeDefinition();
        definition.biome1 = "adv_rocketry:moon";
        definition.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        definition.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        definition.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        definition.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(definition);

        definition = new BiomeConfig.BiomeDefinition();
        definition.biome1 = "adv_rocketry:moon_dark";
        definition.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.OCEAN, BiomeConfig.Continentalness.DEEP_OCEAN));
        definition.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        definition.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        definition.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(definition);

        return config;
    }
}
