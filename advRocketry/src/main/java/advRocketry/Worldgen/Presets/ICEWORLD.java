package advRocketry.Worldgen.Presets;

import advRocketry.Worldgen.BiomeConfig;
import java.util.List;

public class ICEWORLD {
    public static String name = "iceworld.json";

    public static BiomeConfig create() {

        // ==========================================
        // 1. BASELINE — ALL CLIMATE GRID COVERS
        // ==========================================
        // Fill the entire climate grid with ice_crystals, ice spikes, dead forests, and frozen tundra as the dominant baseline.
        // This ensures no valid climate combination is left without a fiercely frozen biome.
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "adv_rocketry:ice_crystals", "minecraft:ice_spikes", "adv_rocketry:ice_crystals", "biomesoplenty:tundra", "biomesoplenty:cold_desert" },
                { "minecraft:ice_spikes", "adv_rocketry:ice_crystals", "biomesoplenty:crag", "biomesoplenty:tundra", "biomesoplenty:cold_desert" },
                { "adv_rocketry:ice_crystals", "minecraft:ice_spikes", "adv_rocketry:ice_crystals", "biomesoplenty:muskeg", "biomesoplenty:cold_desert" },
                { "minecraft:ice_spikes", "adv_rocketry:ice_crystals", "biomesoplenty:crag", "biomesoplenty:cold_desert", "biomesoplenty:tundra" },
                { "biomesoplenty:crag", "biomesoplenty:muskeg", "adv_rocketry:ice_crystals", "biomesoplenty:cold_desert", "biomesoplenty:cold_desert" }
        };

        // --- PEAKS ---
        // Pure ice spikes, frozen peaks, jagged peaks, and geothermal hot springs dominate the highlands.
        String[][] peaksByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "minecraft:ice_spikes", "minecraft:frozen_peaks", "minecraft:ice_spikes", "minecraft:jagged_peaks", "biomesoplenty:hot_springs" },
                { "minecraft:ice_spikes", "minecraft:frozen_peaks", "minecraft:ice_spikes", "minecraft:jagged_peaks", "biomesoplenty:hot_springs" },
                { "minecraft:ice_spikes", "minecraft:ice_spikes", "minecraft:jagged_peaks", "minecraft:jagged_peaks", "biomesoplenty:hot_springs" },
                { "minecraft:ice_spikes", "minecraft:frozen_peaks", "minecraft:ice_spikes", "minecraft:snowy_slopes", "biomesoplenty:hot_springs" },
                { "minecraft:frozen_peaks", "minecraft:ice_spikes", "minecraft:ice_spikes", "minecraft:snowy_slopes", "biomesoplenty:hot_springs" }
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

        // THE JAGGED GLACIERS (Mountains)
        // Low erosion means highly mountainous, jagged terrain dominating the landscape.
        BiomeConfig.BiomeDefinition cragsOverride = new BiomeConfig.BiomeDefinition();
        cragsOverride.biome1 = "minecraft:ice_spikes";
        cragsOverride.biome2 = "biomesoplenty:crag";
        cragsOverride.river1 = "minecraft:frozen_river";
        cragsOverride.peak1 = "minecraft:frozen_peaks";
        cragsOverride.peak2 = "minecraft:ice_spikes";

        cragsOverride.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        cragsOverride.humidityList.addAll(List.of(BiomeConfig.Humidity.MID, BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.VERY_DRY));
        cragsOverride.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        cragsOverride.erosionList.add(BiomeConfig.Erosion.VERY_LOW);
        config.biomes.add(cragsOverride);

        // GEOTHERMAL OASIS (Thermal Springs Pockets)
        // Hydrothermal vents and hot springs breaching through the ice sheet in warmer inland pockets.
        BiomeConfig.BiomeDefinition hotSpringsOverride = new BiomeConfig.BiomeDefinition();
        hotSpringsOverride.biome1 = "biomesoplenty:hot_springs";
        hotSpringsOverride.biome2 = "adv_rocketry:ice_crystals";
        hotSpringsOverride.river1 = "minecraft:frozen_river";
        hotSpringsOverride.peak1 = "biomesoplenty:hot_springs";

        hotSpringsOverride.temperaturesList.addAll(List.of(BiomeConfig.Temperature.WARM, BiomeConfig.Temperature.HOT));
        hotSpringsOverride.humidityList.addAll(List.of(BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.VERY_DRY));
        hotSpringsOverride.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        hotSpringsOverride.erosionList.add(BiomeConfig.Erosion.VERY_HIGH);
        config.biomes.add(hotSpringsOverride);

        return config;
    }
}