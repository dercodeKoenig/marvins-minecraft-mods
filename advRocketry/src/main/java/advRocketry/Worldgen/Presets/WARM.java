package advRocketry.Worldgen.Presets;

import advRocketry.Worldgen.BiomeConfig;
import java.util.List;

public class WARM {
    public static String name = "warm.json";

    public static BiomeConfig create() {

        // --- BASE BIOMES (Normal Terrain) ---
        // Handles standard elevations and coasts. No ice or snow.
        // "Cool" areas (top rows) are mild forests and plains.
        // "Hot" areas (bottom rows) are jungles, tropics, and deserts.
        String[][] biomesByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "biomesoplenty:bog", "minecraft:forest", "minecraft:plains", "biomesoplenty:scrubland", "biomesoplenty:prairie" },
                { "biomesoplenty:bayou", "biomesoplenty:orchard", "biomesoplenty:grassland", "biomesoplenty:shrubland", "minecraft:savanna" },
                { "minecraft:mangrove_swamp", "minecraft:dark_forest", "biomesoplenty:field", "biomesoplenty:mediterranean_forest", "minecraft:savanna" },
                { "biomesoplenty:rainforest", "biomesoplenty:mystic_grove", "biomesoplenty:lush_savanna", "minecraft:savanna", "minecraft:desert" },
                { "minecraft:jungle", "biomesoplenty:bamboo_jungle", "biomesoplenty:tropics", "minecraft:desert", "minecraft:desert" }
        };

        // --- WEIRDNESS / VARIANT BIOMES (Shattered Terrain) ---
        // Generates when the weirdness noise is high (rough, broken, or chaotic terrain).
        String[][] weirdnessByTemperatureAndHumidity = new String[][]{
                // Columns: VERY_WET, WET, MID, DRY, VERY_DRY
                { "minecraft:windswept_forest", "minecraft:windswept_hills", "minecraft:windswept_hills", "biomesoplenty:rocky_shrubland", "biomesoplenty:rocky_shrubland" },
                { "biomesoplenty:jade_cliffs", "minecraft:windswept_forest", "minecraft:windswept_hills", "biomesoplenty:rocky_shrubland", "minecraft:windswept_savanna" },
                { "biomesoplenty:rocky_rainforest", "biomesoplenty:jade_cliffs", "minecraft:windswept_hills", "minecraft:windswept_savanna", "minecraft:windswept_savanna" },
                { "biomesoplenty:rocky_rainforest", "minecraft:sparse_jungle", "minecraft:windswept_savanna", "minecraft:windswept_savanna", "minecraft:badlands" },
                { "minecraft:sparse_jungle", "minecraft:sparse_jungle", "minecraft:windswept_savanna", "minecraft:wooded_badlands", "minecraft:eroded_badlands" }
        };

        // --- AQUATIC & EDGES ---
        String[] riversByTemperature = { "minecraft:river", "minecraft:river", "minecraft:river", "minecraft:river", "minecraft:river" };
        String[] beachesByTemperature = { "minecraft:beach", "minecraft:beach", "minecraft:beach", "minecraft:beach", "biomesoplenty:dune_beach" };
        String[] oceansByTemperature = { "minecraft:ocean", "minecraft:ocean", "minecraft:lukewarm_ocean", "minecraft:warm_ocean", "minecraft:warm_ocean" };
        String[] deepOceansByTemperature = { "minecraft:deep_ocean", "minecraft:deep_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_lukewarm_ocean" };

        BiomeConfig config = BiomeConfigCreator.create(
                biomesByTemperatureAndHumidity, weirdnessByTemperatureAndHumidity,
                riversByTemperature, beachesByTemperature, oceansByTemperature, deepOceansByTemperature
        );


        // ==========================================
        // --- TOPOGRAPHICAL OVERRIDES ---
        // ==========================================
        // We now STRICTLY filter by Temperature and Erosion so the matrix shines through everywhere else.

        // 1. Mild Mountains (Low Temperature, Moderate to High Humidity)
        // Overrides ONLY the coldest/mildest mountain peaks so they don't generate snow.
        BiomeConfig.BiomeDefinition mildMountains = new BiomeConfig.BiomeDefinition();
        mildMountains.biome1 = "biomesoplenty:highland";
        mildMountains.biome2 = "biomesoplenty:jade_cliffs";
        mildMountains.river1 = "minecraft:river";
        mildMountains.peak1 = "biomesoplenty:crag";
        mildMountains.peak2 = "minecraft:stony_peaks";

        mildMountains.temperaturesList.addAll(List.of(BiomeConfig.Temperature.FROZEN, BiomeConfig.Temperature.LOW)); // Only mild zones
        mildMountains.humidityList.addAll(List.of(BiomeConfig.Humidity.VERY_WET, BiomeConfig.Humidity.WET, BiomeConfig.Humidity.MID));
        mildMountains.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        mildMountains.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW)); // Only actual mountains
        config.biomes.add(mildMountains);


        // 2. Tropical Jungle Peaks (Hot Temperature, High Humidity)
        // Overrides hot, wet mountains to be covered in rainforests and stony peaks instead of bare stone.
        BiomeConfig.BiomeDefinition tropicalMountains = new BiomeConfig.BiomeDefinition();
        tropicalMountains.biome1 = "biomesoplenty:rocky_rainforest";
        tropicalMountains.biome2 = "minecraft:sparse_jungle";
        tropicalMountains.river1 = "minecraft:river";
        tropicalMountains.peak1 = "minecraft:stony_peaks";
        tropicalMountains.peak2 = "minecraft:stony_peaks";

        tropicalMountains.temperaturesList.addAll(List.of(BiomeConfig.Temperature.WARM, BiomeConfig.Temperature.HOT)); // Only hot zones
        tropicalMountains.humidityList.addAll(List.of(BiomeConfig.Humidity.VERY_WET, BiomeConfig.Humidity.WET));
        tropicalMountains.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        tropicalMountains.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW));
        config.biomes.add(tropicalMountains);


        // 3. Arid Plateaus (Warm/Hot Temperature, Low Humidity)
        // Overrides dry mountains to generate savannas and badlands.
        BiomeConfig.BiomeDefinition aridMountains = new BiomeConfig.BiomeDefinition();
        aridMountains.biome1 = "minecraft:savanna_plateau";
        aridMountains.biome2 = "minecraft:wooded_badlands";
        aridMountains.river1 = "minecraft:river";
        aridMountains.peak1 = "minecraft:windswept_savanna";
        aridMountains.peak2 = "minecraft:wooded_badlands";

        aridMountains.temperaturesList.addAll(List.of(BiomeConfig.Temperature.MID, BiomeConfig.Temperature.WARM, BiomeConfig.Temperature.HOT)); // Warm/Hot only
        aridMountains.humidityList.addAll(List.of(BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.VERY_DRY)); // Dry only
        aridMountains.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        aridMountains.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW));
        config.biomes.add(aridMountains);


        // 4. Rare Blooming Meadows (Strict Single-Intersection Filter)
        // Now restricted down to a single microscopic combination.
        // It will only trigger in exactly standard temperate, wet, near-coastal flats.
        BiomeConfig.BiomeDefinition bloomingFlats = new BiomeConfig.BiomeDefinition();
        bloomingFlats.biome1 = "biomesoplenty:field";
        bloomingFlats.biome2 = "biomesoplenty:lavender_field";
        bloomingFlats.river1 = "minecraft:river";
        bloomingFlats.peak1 = "minecraft:meadow";
        bloomingFlats.peak2 = "biomesoplenty:grassland";

        bloomingFlats.temperaturesList.addAll(List.of(BiomeConfig.Temperature.MID)); // Shrunk from LOW, MID
        bloomingFlats.humidityList.addAll(List.of(BiomeConfig.Humidity.WET));       // Shrunk from WET, MID
        bloomingFlats.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.NEAR_INLAND)); // Shrunk from NEAR, MID
        bloomingFlats.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_HIGH));
        config.biomes.add(bloomingFlats);

        return config;
    }
}