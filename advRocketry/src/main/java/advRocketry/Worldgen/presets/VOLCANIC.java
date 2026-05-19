package advRocketry.Worldgen.presets;

import advRocketry.Worldgen.BiomeConfig;
import java.util.List;

public class VOLCANIC {
    public static String name = "volcanic.json";

    public static BiomeConfig create() {

        // --- BASE BIOMES ---
        // Completely dominated by volcanic plains and volcanoes to ensure the planet feels hostile immediately.
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "biomesoplenty:volcanic_plains", "biomesoplenty:erupting_inferno", "biomesoplenty:volcanic_plains", "biomesoplenty:erupting_inferno", "biomesoplenty:volcano" },
                { "biomesoplenty:erupting_inferno", "biomesoplenty:volcanic_plains", "biomesoplenty:volcano", "biomesoplenty:volcanic_plains", "biomesoplenty:erupting_inferno" },
                { "biomesoplenty:volcanic_plains", "biomesoplenty:volcano", "biomesoplenty:erupting_inferno", "biomesoplenty:volcanic_plains", "biomesoplenty:volcano" },
                { "biomesoplenty:erupting_inferno", "biomesoplenty:volcanic_plains", "biomesoplenty:volcano", "biomesoplenty:erupting_inferno", "biomesoplenty:volcanic_plains" },
                { "biomesoplenty:volcano", "biomesoplenty:erupting_inferno", "biomesoplenty:volcanic_plains", "biomesoplenty:volcano", "biomesoplenty:volcano" }
        };

        // --- PEAKS ---
        // Pure fiery spikes. We use basalt deltas in the wettest regions to simulate cooled magma spires.
        String[][] peaksByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "minecraft:basalt_deltas", "minecraft:basalt_deltas", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano" },
                { "minecraft:basalt_deltas", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano" },
                { "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano" },
                { "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano" },
                { "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano" }
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


        // ==========================================
        // --- TOPOGRAPHICAL OVERRIDES ---
        // ==========================================

        // add some alternative biomes far away from the lava oceans

        // 1. The Ash Steppes (Flatlands)
        // High erosion means flatter, smoothed-out terrain. Placed in hot, dry regions.
        BiomeConfig.BiomeDefinition steppesOverride = new BiomeConfig.BiomeDefinition();
        steppesOverride.biome1 = "biomesoplenty:wasteland_steppe";
        steppesOverride.biome2 = "biomesoplenty:wasteland";
        steppesOverride.river1 = "minecraft:basalt_deltas";
        steppesOverride.peak1 = "biomesoplenty:volcano";
        steppesOverride.peak2 = "biomesoplenty:volcano";

        steppesOverride.temperaturesList.addAll(List.of(BiomeConfig.Temperature.WARM, BiomeConfig.Temperature.HOT));
        steppesOverride.humidityList.addAll(List.of(BiomeConfig.Humidity.MID, BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.VERY_DRY));
        steppesOverride.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        steppesOverride.erosionList.addAll(List.of(BiomeConfig.Erosion.HIGH, BiomeConfig.Erosion.VERY_HIGH));
        config.biomes.add(steppesOverride);


        // 2. The Jagged Crags (Mountains)
        // Low erosion means highly mountainous, jagged terrain.
        BiomeConfig.BiomeDefinition cragsOverride = new BiomeConfig.BiomeDefinition();
        cragsOverride.biome1 = "biomesoplenty:crag";
        cragsOverride.biome2 = "biomesoplenty:dead_forest";
        cragsOverride.river1 = "minecraft:basalt_deltas";
        cragsOverride.peak1 = "biomesoplenty:volcano";
        cragsOverride.peak2 = "biomesoplenty:volcano";

        // Shares the same climate as the Steppes, but separated entirely by the erosion parameter.
        cragsOverride.temperaturesList.addAll(List.of(BiomeConfig.Temperature.MID, BiomeConfig.Temperature.WARM, BiomeConfig.Temperature.HOT));
        cragsOverride.humidityList.addAll(List.of(BiomeConfig.Humidity.MID, BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.VERY_DRY));
        cragsOverride.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        cragsOverride.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW));
        config.biomes.add(cragsOverride);


        // 3. The Charred Forests (Versatile)
        // Can appear on both flatlands or mountains (all erosions).
        // Protected from overwriting Steppes/Crags by requiring wetter, cooler climates.
        BiomeConfig.BiomeDefinition deadForestOverride = new BiomeConfig.BiomeDefinition();
        deadForestOverride.biome1 = "biomesoplenty:dead_forest";
        deadForestOverride.biome2 = "biomesoplenty:old_growth_dead_forest";
        deadForestOverride.river1 = "minecraft:basalt_deltas";
        deadForestOverride.peak1 = "biomesoplenty:volcano";
        deadForestOverride.peak2 = "biomesoplenty:volcano";

        deadForestOverride.temperaturesList.addAll(List.of(BiomeConfig.Temperature.LOW, BiomeConfig.Temperature.MID));
        deadForestOverride.humidityList.addAll(List.of(BiomeConfig.Humidity.VERY_WET, BiomeConfig.Humidity.WET));
        deadForestOverride.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        deadForestOverride.erosionList.addAll(List.of(BiomeConfig.Erosion.values())); // Any erosion
        config.biomes.add(deadForestOverride);

        return config;
    }
}