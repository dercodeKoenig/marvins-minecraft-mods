package advRocketry.Worldgen.Presets;

import advRocketry.Worldgen.BiomeConfig;
import java.util.List;

public class WARM_DRY {
    public static String name = "warm_dry.json";

    public static BiomeConfig create() {

        // --- BASE BIOMES (Low Weirdness) ---
        // A warm, dry-leaning planet. The wettest regions support Mediterranean forests and lush savannas.
        // As humidity decreases, the terrain transitions through prairies and drylands, eventually
        // yielding to harsh deserts, lush deserts, and badlands in the driest zones.
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "biomesoplenty:mediterranean_forest", "biomesoplenty:scrubland", "biomesoplenty:shrubland", "biomesoplenty:dryland", "minecraft:badlands" },
                { "biomesoplenty:mediterranean_forest", "biomesoplenty:scrubland", "biomesoplenty:prairie", "biomesoplenty:dryland", "minecraft:badlands" },
                { "biomesoplenty:lush_savanna", "minecraft:savanna", "biomesoplenty:prairie", "biomesoplenty:lush_desert", "minecraft:eroded_badlands" },
                { "biomesoplenty:lush_savanna", "minecraft:savanna", "minecraft:savanna", "minecraft:desert", "minecraft:eroded_badlands" },
                { "biomesoplenty:lush_savanna", "minecraft:savanna", "biomesoplenty:dryland", "minecraft:desert", "minecraft:desert" }
        };

        // --- WEIRDNESS / VARIANT BIOMES (Shattered Terrain) ---
        // Generates in areas of high weirdness.
        // We use rocky shrublands and windswept savannas for the vegetated areas,
        // and wooded badlands to create striking, elevated terracotta variations in the dry zones.
        String[][] weirdnessByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "biomesoplenty:crag", "biomesoplenty:rocky_shrubland", "biomesoplenty:rocky_shrubland", "minecraft:wooded_badlands", "minecraft:wooded_badlands" },
                { "biomesoplenty:crag", "biomesoplenty:rocky_shrubland", "biomesoplenty:rocky_shrubland", "minecraft:wooded_badlands", "minecraft:wooded_badlands" },
                { "biomesoplenty:rocky_shrubland", "minecraft:windswept_savanna", "minecraft:windswept_savanna", "minecraft:desert", "minecraft:wooded_badlands" },
                { "minecraft:windswept_savanna", "minecraft:windswept_savanna", "minecraft:windswept_savanna", "biomesoplenty:lush_desert", "minecraft:wooded_badlands" },
                { "minecraft:windswept_savanna", "minecraft:windswept_savanna", "minecraft:windswept_savanna", "minecraft:desert", "minecraft:wooded_badlands" }
        };

        // --- AQUATIC & EDGES ---
        // Oceans remain as normal bodies of water, primarily warm to fit the planet's theme.
        // Beaches transition from standard sand to dune beaches in the driest areas.
        String[] riversByTemperature = { "minecraft:river", "minecraft:river", "minecraft:river", "minecraft:river", "minecraft:river" };
        String[] beachesByTemperature = { "minecraft:beach", "minecraft:beach", "minecraft:beach", "biomesoplenty:dune_beach", "biomesoplenty:dune_beach" };
        String[] oceansByTemperature = { "minecraft:ocean", "minecraft:ocean", "minecraft:warm_ocean", "minecraft:warm_ocean", "minecraft:warm_ocean" };
        String[] deepOceansByTemperature = { "minecraft:deep_ocean", "minecraft:deep_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_lukewarm_ocean" };

        BiomeConfig config = BiomeConfigCreator.create(
                biomesByTemperatureAndHumidity, weirdnessByTemperatureAndHumidity,
                riversByTemperature, beachesByTemperature, oceansByTemperature, deepOceansByTemperature
        );


        // ==========================================
        // --- TOPOGRAPHICAL OVERRIDES ---
        // ==========================================

        // 1. The Highland Ranges (Wetter Mountains)
        // Inland mountains in regions that still receive moisture. Grassy, craggy, and rugged.
        BiomeConfig.BiomeDefinition highlandMountains = new BiomeConfig.BiomeDefinition();
        highlandMountains.biome1 = "biomesoplenty:highland";
        highlandMountains.biome2 = "biomesoplenty:crag";
        highlandMountains.river1 = "minecraft:river";
        highlandMountains.peak1 = "biomesoplenty:crag";
        highlandMountains.peak2 = "minecraft:savanna_plateau";

        highlandMountains.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        // Targets the wetter half of the planet
        highlandMountains.humidityList.addAll(List.of(BiomeConfig.Humidity.VERY_WET, BiomeConfig.Humidity.WET));
        highlandMountains.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        highlandMountains.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW)); // Low erosion = Mountains
        config.biomes.add(highlandMountains);


        // 2. The Arid Canyons & Plateaus (Dry Mountains)
        // Inland mountains in the dry bands. Creates sprawling, towering badlands and rocky scrublands.
        BiomeConfig.BiomeDefinition aridMountains = new BiomeConfig.BiomeDefinition();
        aridMountains.biome1 = "minecraft:wooded_badlands";
        aridMountains.biome2 = "minecraft:windswept_savanna";
        aridMountains.river1 = "minecraft:river";
        aridMountains.peak1 = "minecraft:eroded_badlands";
        aridMountains.peak2 = "minecraft:savanna_plateau";

        aridMountains.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        // Targets the drier half of the planet
        aridMountains.humidityList.addAll(List.of(BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.VERY_DRY));
        aridMountains.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        aridMountains.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW)); // Low erosion = Mountains
        config.biomes.add(aridMountains);

        return config;
    }
}