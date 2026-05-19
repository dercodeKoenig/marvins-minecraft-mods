package advRocketry.Worldgen.presets;

import advRocketry.Worldgen.BiomeConfig;
import java.util.List;

public class VENUS {
    public static String name = "venus.json";

    public static BiomeConfig create() {

        // --- BASE BIOMES ---
        // A rocky crust dominated by vegetation-free hot rocks (Badlands).
        // As conditions get hotter and drier, the ground fractures into tectonic volcanic terrain.
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "minecraft:badlands", "minecraft:badlands", "minecraft:badlands", "minecraft:badlands", "minecraft:badlands" },
                { "minecraft:badlands", "minecraft:badlands", "minecraft:badlands", "minecraft:badlands", "minecraft:badlands" },
                { "minecraft:badlands", "minecraft:badlands", "minecraft:badlands", "minecraft:badlands", "biomesoplenty:volcano" },
                { "minecraft:badlands", "minecraft:badlands", "minecraft:badlands", "biomesoplenty:volcano", "biomesoplenty:volcano" },
                { "minecraft:badlands", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno" }
        };

        // --- PEAKS ---
        // Every single mountain peak on the planet erupts skyward into smoking micro-volcanoes.
        String[][] peaksByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno" },
                { "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno" },
                { "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno" },
                { "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno" },
                { "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno" }
        };

        // --- AQUATIC & EDGES ---
        // Tectonic rifts (Rivers) are carved out of smoking, erupting fissures.
        // Large basins (Oceans) settle into massive volcanic fields.
        String[] riversByTemperature = { "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno", "biomesoplenty:erupting_inferno" };
        String[] beachesByTemperature = { "minecraft:badlands", "minecraft:badlands", "minecraft:badlands", "biomesoplenty:volcano", "biomesoplenty:volcano" };
        String[] oceansByTemperature = { "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano" };
        String[] deepOceansByTemperature = { "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano", "biomesoplenty:volcano" };

        BiomeConfig config = BiomeConfigCreator.create(
                biomesByTemperatureAndHumidity, peaksByTemperatureAndHumidity,
                riversByTemperature, beachesByTemperature, oceansByTemperature, deepOceansByTemperature
        );
        return config;
    }
}