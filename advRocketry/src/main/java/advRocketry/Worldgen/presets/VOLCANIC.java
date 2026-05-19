package advRocketry.Worldgen.presets;

import advRocketry.Worldgen.BiomeConfig;

public class VOLCANIC {
    public static String name = "volcanic.json";

    public static BiomeConfig create() {

        // Base Biomes: A mix of scorched wastes, dead forests, and jagged volcanic rock.
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                {
                        // FROZEN: Grey, jagged, and desolate scree slopes and peaks
                        "minecraft:jagged_peaks", "biomesoplenty:crag", "minecraft:stony_peaks", "biomesoplenty:cold_desert", "minecraft:windswept_gravelly_hills"
                },
                {
                        // LOW (Cool): Forests choked out by ash, dead vegetation, and cracked earth
                        "biomesoplenty:ominous_woods", "biomesoplenty:dead_forest", "biomesoplenty:wasteland_steppe", "biomesoplenty:dryland", "biomesoplenty:old_growth_dead_forest"
                },
                {
                        // MID (Temperate): Murky sulfur bogs transitioning into harsh rocky wastes
                        "biomesoplenty:bog", "biomesoplenty:crag", "biomesoplenty:wasteland", "biomesoplenty:volcanic_plains", "biomesoplenty:dead_forest"
                },
                {
                        // WARM: Geothermal springs and deeply carved, baked red canyons
                        "biomesoplenty:hot_springs", "biomesoplenty:rocky_shrubland", "minecraft:badlands", "biomesoplenty:volcanic_plains", "minecraft:eroded_badlands"
                },
                {
                        // HOT: The epicenter. Boiling pools, volcanoes, and deeply scorched earth
                        "biomesoplenty:volcano", "biomesoplenty:hot_springs", "biomesoplenty:volcano", "biomesoplenty:volcanic_plains", "biomesoplenty:wasteland"
                }
        };

        // Peak Biomes: Obsidian-like peaks, volcanoes, and crags
        String[][] peaksByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                {
                        "minecraft:stony_peaks", "minecraft:stony_peaks", "minecraft:basalt_deltas", "biomesoplenty:hot_springs", "biomesoplenty:hot_springs"
                }, // FROZEN
                {
                        "biomesoplenty:crag", "minecraft:basalt_deltas", "biomesoplenty:hot_springs", null, "biomesoplenty:volcano"
                }, // LOW
                {
                        "minecraft:basalt_deltas", "biomesoplenty:hot_springs", null, "biomesoplenty:volcano", "biomesoplenty:crag"
                }, // MID
                {
                        "minecraft:basalt_deltas", "biomesoplenty:hot_springs", null, "biomesoplenty:volcano", "biomesoplenty:volcano"
                }, // WARM
                {
                        "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano"
                }  // HOT
        };

        // Use Basalt Deltas for "Rivers" to create paths of ash and jagged stone
        String[] riversByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas"
                };

        // Shorelines are either stone, gravel, or purely volcanic
        String[] beachesByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:stony_shore", "biomesoplenty:gravel_beach", "minecraft:basalt_deltas", null, null
                };

        // Oceans: Exclusively Basalt Deltas to ensure the "floor" of the world looks like a jagged lava bed
        String[] oceansByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas", "minecraft:basalt_deltas"
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