package advRocketry.Worldgen.presets;
import advRocketry.Worldgen.BiomeConfig;

public class DESERT_WASTELAND {
    public static String name = "desert_wasteland.json";

    public static BiomeConfig create() {

        // add base biomes - No plant life anywhere, just sand, terracotta, and jagged rock.
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                {
                        "minecraft:badlands", "minecraft:desert", "minecraft:desert", "minecraft:stony_peaks", "minecraft:basalt_deltas"
                }, // FROZEN (The "coldest" regions are just rocky, desolate flats)
                {
                        "minecraft:badlands", "minecraft:badlands", "minecraft:desert", "minecraft:desert", "minecraft:basalt_deltas"
                }, // LOW
                {
                        "minecraft:eroded_badlands", "minecraft:badlands", "minecraft:desert", "minecraft:desert", "minecraft:desert"
                }, // MID
                {
                        "minecraft:eroded_badlands", "minecraft:eroded_badlands", "minecraft:desert", "minecraft:desert", "minecraft:basalt_deltas"
                }, // WARM
                {
                        "minecraft:desert", "minecraft:desert", "minecraft:desert", "minecraft:basalt_deltas", "minecraft:basalt_deltas"
                }  // HOT
        };

        // peak biomes - Mountains are either sheer rock, eroded spikes, or volcanic ash hills
        String[][] peaksByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                {
                        "minecraft:stony_peaks", "minecraft:stony_peaks", null, "minecraft:basalt_deltas", "minecraft:basalt_deltas"
                }, // FROZEN
                {
                        "minecraft:eroded_badlands", "minecraft:stony_peaks", null, null, null
                }, // LOW
                {
                        "minecraft:eroded_badlands", null, null, null, null
                }, // MID
                {
                        "minecraft:basalt_deltas", null, null, null, null
                }, // WARM
                {
                        "minecraft:basalt_deltas", "minecraft:basalt_deltas", null, null, null
                }  // HOT
        };


        // Entirely null - no rivers will generate, leaving unbroken landmasses
        String[] riversByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        null, null, null, null, null
                };

        // No water means no beaches needed
        String[] beachesByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        null, null, null, null, null
                };

        // Oceans are completely disabled to create an endless continental wasteland
        String[] oceansByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        null, null, null, null, null
                };

        String[] deepOceansByTemperature = new String[]
                {   // FROZEN , LOW , MID , WARM , HOT
                        null, null, null, null, null
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