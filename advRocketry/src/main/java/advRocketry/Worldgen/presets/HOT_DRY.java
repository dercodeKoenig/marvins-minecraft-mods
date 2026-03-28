package advRocketry.Worldgen.presets;

import advRocketry.Worldgen.BiomeConfig;

public class HOT_DRY {
    public static String name = "hot_dry.json";

    public static BiomeConfig create() {

        // add base biomes
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                {
                        "minecraft:savanna", "minecraft:savanna", "minecraft:plains", "minecraft:desert", "minecraft:desert"
                }, // FROZEN (Relative to this planet - these are its "coolest" regions)
                {
                        "minecraft:sparse_jungle", "minecraft:savanna_plateau", "minecraft:savanna", "minecraft:desert", "minecraft:badlands"
                }, // LOW
                {
                        "minecraft:wooded_badlands", "minecraft:wooded_badlands", "minecraft:badlands", "minecraft:desert", "minecraft:desert"
                }, // MID
                {
                        "minecraft:badlands", "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:desert", "minecraft:desert"
                }, // WARM
                {
                        "minecraft:eroded_badlands", "minecraft:desert", "minecraft:desert", "minecraft:desert", "minecraft:desert"
                }  // HOT
        };

        // peak biomes
        String[][] peaksByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                {
                        "minecraft:windswept_savanna", "minecraft:windswept_savanna", "minecraft:stony_peaks", "minecraft:stony_peaks", null
                }, // FROZEN
                {
                        "minecraft:windswept_savanna", "minecraft:stony_peaks", null, null, "minecraft:eroded_badlands"
                }, // LOW
                {
                        "minecraft:basalt_deltas", "minecraft:stony_peaks", null, null, null
                }, // MID
                {
                        "minecraft:basalt_deltas", null, "minecraft:eroded_badlands", null, null
                }, // WARM
                {
                        "minecraft:basalt_deltas", null, null, null, null
                }  // HOT
        };

        // Drying out the rivers in the hotter zones
        String[] riversByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:river", "minecraft:river", "minecraft:desert", "minecraft:badlands", "minecraft:basalt_deltas"
                };

        // Rocky shores and mostly sand
        String[] beachesByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:beach", "minecraft:stony_shore", "minecraft:desert", null, null
                };

        // Oceans are predominantly warm, meaning coral reefs might spawn in the coolest areas, but mostly just warm water
        String[] oceansByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:lukewarm_ocean", "minecraft:warm_ocean", "minecraft:warm_ocean", "minecraft:warm_ocean", "minecraft:warm_ocean"
                };

        // Deep oceans follow the same warm trend
        String[] deepOceansByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:deep_lukewarm_ocean", "minecraft:warm_ocean", "minecraft:warm_ocean", "minecraft:warm_ocean", "minecraft:warm_ocean"
                };

        return BiomeConfigCreator.create(
                biomesByTemperatureAndHumidity,
                peaksByTemperatureAndHumidity,
                riversByTemperature,
                beachesByTemperature,
                oceansByTemperature,
                deepOceansByTemperature
        );
    }
}