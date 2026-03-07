package advRocketry.Worldgen.presets;

import advRocketry.Worldgen.BiomeConfig;

public class HOT {
    public static String name = "hot.json";

    public static BiomeConfig create() {

        // add base biomes
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                {
                        "minecraft:forest", "minecraft:plains", "minecraft:meadow", "minecraft:savanna", "minecraft:savanna"
                }, // FROZEN
                {
                        "minecraft:jungle", "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:savanna", "minecraft:desert"
                }, // LOW
                {
                        "minecraft:bamboo_jungle", "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:savanna_plateau", "minecraft:desert"
                }, // MID
                {
                        "minecraft:mangrove_swamp", "minecraft:wooded_badlands", "minecraft:badlands", "minecraft:desert", "minecraft:desert"
                }, // WARM
                {
                        "minecraft:desert", "minecraft:badlands", "minecraft:badlands", "minecraft:badlands", "minecraft:eroded_badlands"
                }  // HOT
        };

        // peak biomes
        String[][] peaksByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                {
                        "minecraft:windswept_forest", "minecraft:windswept_hills", "minecraft:stony_peaks", "minecraft:windswept_savanna", "minecraft:windswept_gravelly_hills"
                }, // FROZEN
                {
                        "minecraft:warped_forest", "minecraft:cherry_grove", null, "minecraft:flower_forest", "minecraft:stony_peaks"
                }, // LOW
                {
                        "minecraft:sunflower_plains", null, "minecraft:savanna", "minecraft:windswept_savanna", null
                }, // MID
                {
                        null, "minecraft:basalt_deltas", null, null, null,
                }, // WARM
                {
                        null, null, null, null, null
                }  // HOT
        };


        String[] riversByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:river", "minecraft:river", "minecraft:river", "minecraft:river", "minecraft:basalt_deltas"
                };

        String[] beachesByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:stony_shore", "minecraft:stony_shore", "minecraft:beach", null, null
                };

        String[] oceansByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:lukewarm_ocean", "minecraft:lukewarm_ocean", "minecraft:warm_ocean", "minecraft:warm_ocean", "minecraft:warm_ocean"
                };
        String[] deepOceansByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:deep_ocean", "minecraft:deep_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:warm_ocean"
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
