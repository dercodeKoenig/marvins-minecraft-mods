package advRocketry.Worldgen.Presets;

import advRocketry.Worldgen.BiomeConfig;
import java.util.List;

public class COLD {
    public static String name = "cold.json";

    public static BiomeConfig create() {

        // ==========================================
        // 1. BASELINE — ALL CLIMATE GRID COVERS
        // ==========================================
        // Fill the entire climate grid with tundras, snowlands, and dead winter forests.
        // This ensures a cold, barren feel without relying on pure ice crystals.
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "biomesoplenty:snowy_coniferous_forest", "biomesoplenty:snowy_fir_clearing", "minecraft:snowy_taiga", "biomesoplenty:tundra", "biomesoplenty:cold_desert" },
                { "biomesoplenty:snowy_maple_woods", "biomesoplenty:snowblossom_grove", "biomesoplenty:muskeg", "biomesoplenty:tundra", "biomesoplenty:cold_desert" },
                { "minecraft:snowy_taiga", "biomesoplenty:dead_forest", "minecraft:snowy_plains", "biomesoplenty:tundra", "biomesoplenty:cold_desert" },
                { "biomesoplenty:snowy_fir_clearing", "biomesoplenty:muskeg", "minecraft:snowy_plains", "biomesoplenty:tundra", "biomesoplenty:cold_desert" },
                { "biomesoplenty:snowy_coniferous_forest", "minecraft:snowy_taiga", "biomesoplenty:dead_forest", "biomesoplenty:tundra", "biomesoplenty:cold_desert" }
        };

        // --- PEAKS ---
        // Conventional snowy mountains and jagged peaks to fit a standard frozen wasteland.
        String[][] peaksByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "minecraft:frozen_peaks", "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:snowy_slopes", "biomesoplenty:hot_springs" },
                { "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:jagged_peaks", "minecraft:snowy_slopes", "biomesoplenty:hot_springs" },
                { "minecraft:jagged_peaks", "minecraft:jagged_peaks", "minecraft:jagged_peaks", "minecraft:snowy_slopes", "biomesoplenty:hot_springs" },
                { "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:jagged_peaks", "minecraft:snowy_slopes", "biomesoplenty:hot_springs" },
                { "minecraft:frozen_peaks", "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:snowy_slopes", "biomesoplenty:hot_springs" }
        };

        // ==========================================
        // 2. OCEANS & COASTS — FROZEN WATERS
        // ==========================================
        // Coastal regions and ocean floors get snowy coasts and deep-frozen oceans.
        String[] riversByTemperature = { "minecraft:frozen_river", "minecraft:frozen_river", "minecraft:frozen_river", "minecraft:frozen_river", "minecraft:frozen_river" };
        String[] beachesByTemperature = { "minecraft:snowy_beach", "minecraft:snowy_beach", "minecraft:snowy_beach", "minecraft:snowy_beach", "minecraft:snowy_beach" };
        String[] oceansByTemperature = { "minecraft:frozen_ocean", "minecraft:frozen_ocean", "minecraft:frozen_ocean", "minecraft:frozen_ocean", "minecraft:frozen_ocean" };
        String[] deepOceansByTemperature = { "minecraft:deep_frozen_ocean", "minecraft:deep_frozen_ocean", "minecraft:deep_frozen_ocean", "minecraft:deep_frozen_ocean", "minecraft:deep_frozen_ocean" };

        BiomeConfig config = BiomeConfigCreator.create(
                biomesByTemperatureAndHumidity, peaksByTemperatureAndHumidity,
                riversByTemperature, beachesByTemperature, oceansByTemperature, deepOceansByTemperature
        );

        // ==========================================
        // 3. TOPOGRAPHICAL OVERRIDES
        // ==========================================

        // DEAD FROZEN STEPPES (High Erosion Flats)
        // Smooth, flat high-erosion terrain becomes vast, empty cold deserts and tundras.
        BiomeConfig.BiomeDefinition flats = new BiomeConfig.BiomeDefinition();
        flats.biome1 = "biomesoplenty:tundra";
        flats.biome2 = "biomesoplenty:cold_desert";
        flats.river1 = "minecraft:frozen_river";
        flats.peak1 = "minecraft:snowy_slopes";
        flats.temperaturesList.addAll(List.of(BiomeConfig.Temperature.FROZEN, BiomeConfig.Temperature.LOW));
        flats.humidityList.addAll(List.of(BiomeConfig.Humidity.VERY_DRY, BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.MID));
        flats.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        flats.erosionList.add(BiomeConfig.Erosion.VERY_HIGH);
        config.biomes.add(flats);

        return config;
    }
}