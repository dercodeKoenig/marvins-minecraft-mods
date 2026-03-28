package advRocketry.Worldgen.presets;

import advRocketry.Worldgen.BiomeConfig;

public class VOLCANIC {
    public static String name = "volcanic.json";

    public static BiomeConfig create() {

        // Base Biomes: A mix of scorched wastes and jagged volcanic rock.
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                {
                        "minecraft:basalt_deltas", "minecraft:nether_wastes", "minecraft:stony_peaks", "minecraft:basalt_deltas", "minecraft:basalt_deltas"
                }, // FROZEN (Relative term - still extremely hot)
                {
                        "minecraft:basalt_deltas", "minecraft:nether_wastes", "minecraft:nether_wastes", "minecraft:basalt_deltas", "minecraft:crimson_forest"
                }, // LOW
                {
                        "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:nether_wastes", "minecraft:nether_wastes", "minecraft:crimson_forest"
                }, // MID
                {
                        "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:nether_wastes", "minecraft:nether_wastes"
                }, // WARM
                {
                        "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas"
                }  // HOT (Pure volcanic jaggedness)
        };

        // Peak Biomes: Obsidian-like peaks and glowing fungal "forests"
        String[][] peaksByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                {
                        "minecraft:stony_peaks", "minecraft:stony_peaks", "minecraft:basalt_deltas", null, null
                }, // FROZEN
                {
                        "minecraft:crimson_forest", "minecraft:basalt_deltas", null, null, null
                }, // LOW
                {
                        "minecraft:basalt_deltas", null, null, null, "minecraft:crimson_forest"
                }, // MID
                {
                        "minecraft:basalt_deltas", null, null, null, null
                }, // WARM
                {
                        "minecraft:basalt_deltas", "minecraft:basalt_deltas", null, null, null
                }  // HOT
        };

        // Use Basalt Deltas for "Rivers" to create paths of ash and jagged stone
        String[] riversByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas"
                };

        // Shorelines are either stone or purely volcanic
        String[] beachesByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:stony_shore", "minecraft:stony_shore", "minecraft:basalt_deltas", null, null
                };

        // Oceans: Using Basalt Deltas and Nether Wastes to ensure the "floor" of the world looks like a lava bed
        String[] oceansByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:nether_wastes", "minecraft:nether_wastes", "minecraft:basalt_deltas"
                };

        String[] deepOceansByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas"
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