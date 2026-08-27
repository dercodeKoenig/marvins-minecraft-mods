package advRocketry.Worldgen.Presets;

import advRocketry.Worldgen.BiomeConfig;
import java.util.List;

public class MUSTAFAR {
    public static String name = "mustafar.json";

    public static BiomeConfig create() {

        // --- BASE BIOMES ---
        // Completely dominated by volcanic plains and volcanoes to ensure the planet feels hostile immediately.
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "biomesoplenty:old_growth_dead_forest", "biomesoplenty:old_growth_dead_forest", "biomesoplenty:volcanic_plains", "biomesoplenty:erupting_inferno", "adv_rocketry:volcanic_plains" },
                { "biomesoplenty:old_growth_dead_forest", "biomesoplenty:volcanic_plains", "biomesoplenty:volcano", "adv_rocketry:volcanic_plains", "biomesoplenty:erupting_inferno" },
                { "biomesoplenty:volcanic_plains", "biomesoplenty:volcano", "biomesoplenty:erupting_inferno", "adv_rocketry:volcanic_plains", "adv_rocketry:volcanic_plains" },
                { "biomesoplenty:erupting_inferno", "biomesoplenty:volcanic_plains", "biomesoplenty:volcano", "biomesoplenty:erupting_inferno", "adv_rocketry:volcano" },
                { "biomesoplenty:volcano", "biomesoplenty:erupting_inferno", "biomesoplenty:volcanic_plains", "adv_rocketry:volcano", "adv_rocketry:volcano" }
        };

        // --- PEAKS ---
        // Pure fiery spikes. We use basalt deltas in the wettest regions to simulate cooled magma spires.
        String[][] peaksByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "minecraft:basalt_deltas", "minecraft:basalt_deltas", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano" },
                { "minecraft:basalt_deltas", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano" },
                { "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "adv_rocketry:volcano", "adv_rocketry:volcano" },
                { "adv_rocketry:volcano", "adv_rocketry:volcano", "adv_rocketry:volcano", "adv_rocketry:volcano", "adv_rocketry:volcano" },
                { "adv_rocketry:volcano", "adv_rocketry:volcano", "adv_rocketry:volcano", "adv_rocketry:volcano", "adv_rocketry:volcano" }
        };

        // --- AQUATIC & EDGES ---
        // Oceans, deep oceans, beaches, and rivers are entirely replaced by Basalt Deltas.
        // This gives the impression of massive, dried-out lava basins and jagged shores.
        String[] riversByTemperature = { "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas" };
        String[] beachesByTemperature = { "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas" };
        String[] oceansByTemperature = { "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas" };
        String[] deepOceansByTemperature = { "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas" };

        BiomeConfig config = BiomeConfigCreator.create(
                biomesByTemperatureAndHumidity, peaksByTemperatureAndHumidity,
                riversByTemperature, beachesByTemperature, oceansByTemperature, deepOceansByTemperature
        );


        BiomeConfig.BiomeDefinition mountains = new BiomeConfig.BiomeDefinition();
        mountains.biome1 = "adv_rocketry:volcanic_plains";
        mountains.biome2 = "biomesoplenty:volcanic_plains";
        mountains.river1 = "minecraft:basalt_deltas";
        mountains.peak1 = "biomesoplenty:volcano";
        mountains.peak2 = "adv_rocketry:volcano";

        mountains.temperaturesList.addAll(List.of(BiomeConfig.Temperature.LOW, BiomeConfig.Temperature.MID));
        mountains.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        mountains.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.NEAR_INLAND, BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        mountains.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW));
        config.biomes.add(mountains);


        BiomeConfig.BiomeDefinition mountains2 = new BiomeConfig.BiomeDefinition();
        mountains2.biome1 = "minecraft:stony_peaks";
        mountains2.river1 = "minecraft:basalt_deltas";
        mountains2.peak1 = "biomesoplenty:volcano";
        mountains2.peak2 = "adv_rocketry:volcano";

        mountains2.temperaturesList.addAll(List.of(BiomeConfig.Temperature.WARM, BiomeConfig.Temperature.HOT));
        mountains2.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        mountains2.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.NEAR_INLAND, BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        mountains2.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW));
        config.biomes.add(mountains2);


        BiomeConfig.BiomeDefinition mountains3 = new BiomeConfig.BiomeDefinition();
        mountains3.biome1 = "minecraft:frozen_peaks";
        mountains3.river1 = "minecraft:basalt_deltas";
        mountains3.peak1 = "biomesoplenty:volcano";
        mountains3.peak2 = "adv_rocketry:volcano";

        mountains3.temperaturesList.add(BiomeConfig.Temperature.FROZEN);
        mountains3.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        mountains3.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        mountains3.erosionList.add(BiomeConfig.Erosion.VERY_LOW);
        config.biomes.add(mountains3);

        return config;
    }
}