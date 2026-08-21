package advRocketry.Worldgen.Presets;

import advRocketry.Worldgen.BiomeConfig;
import java.util.List;

public class DESERT_WASTELAND {
    public static String name = "desert_wasteland.json";

    public static BiomeConfig create() {
        // Start with a blank canvas to build via custom climate points
        BiomeConfig config = new BiomeConfig();

        // ==========================================
        // 1. THE SAFETY CATCH-ALL BASELINE
        // ==========================================
        // This baseline fills 100% of the climate grid right out of the gate.
        // No matter what crazy Temperature/Humidity/Erosion combo the world seed rolls,
        // it will always have a valid desert/wasteland definition to fall back on.
        BiomeConfig.BiomeDefinition baseline = new BiomeConfig.BiomeDefinition();
        baseline.biome1 = "minecraft:desert";
        baseline.biome2 = "minecraft:badlands";
        baseline.river1 = "minecraft:desert";
        baseline.peak1 = "minecraft:badlands";
        baseline.peak2 = "minecraft:desert";

        // Feed it every single possible enum value to guarantee total coverage
        baseline.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        baseline.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        baseline.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        baseline.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(baseline);


        // ==========================================
        // 2. THE PAINT LAYER: DRIED BASINS (Low Elevation / Oceans)
        // ==========================================
        // We override the baseline in low-lying basins. Where massive seas once existed,
        // there are now completely dead, gray salt flats and deeply cracked earth crusts.
        BiomeConfig.BiomeDefinition driedOceans = new BiomeConfig.BiomeDefinition();
        driedOceans.biome1 = "biomesoplenty:wasteland";
        driedOceans.biome2 = "biomesoplenty:dryland";
        driedOceans.river1 = "biomesoplenty:wasteland_steppe";
        driedOceans.peak1 = "minecraft:desert";
        driedOceans.peak2 = "minecraft:desert";

        driedOceans.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        driedOceans.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        driedOceans.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        // Specifically targets ocean floors and coastlines to strip them of water look
        driedOceans.continentalnessList.addAll(List.of(
                BiomeConfig.Continentalness.OCEAN,
                BiomeConfig.Continentalness.DEEP_OCEAN,
                BiomeConfig.Continentalness.COAST
        ));
        config.biomes.add(driedOceans);


        // ==========================================
        // 3. THE PAINT LAYER: THE CLAY CRAGS (Low Erosion / Mountains)
        // ==========================================
        // We paint over highland mountain areas. Low erosion means steep, jagged elevation.
        // Instead of standard sand, these areas push up towering ridges of exposed terracotta.
        BiomeConfig.BiomeDefinition clayHighlands = new BiomeConfig.BiomeDefinition();
        clayHighlands.biome1 = "minecraft:badlands";
        clayHighlands.biome2 = "minecraft:eroded_badlands";
        clayHighlands.river1 = "minecraft:desert";
        clayHighlands.peak1 = "minecraft:eroded_badlands";
        clayHighlands.peak2 = "biomesoplenty:dune_beach"; // Used as sweeping high-altitude sand drifts

        clayHighlands.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        clayHighlands.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        clayHighlands.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        // Paint strictly over sharp, vertical, non-eroded hills and peaks
        clayHighlands.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW));
        config.biomes.add(clayHighlands);


        // ==========================================
        // 4. THE PAINT LAYER: THE COLD TUNDRA WASTES (Low Temperatures)
        // ==========================================
        // Deserts don't have to be burning hot. This paint layer targets cold climate points,
        // converting standard hot deserts into freezing, wind-blasted gravel flats.
        BiomeConfig.BiomeDefinition polarWastes = new BiomeConfig.BiomeDefinition();
        polarWastes.biome1 = "biomesoplenty:cold_desert";
        polarWastes.biome2 = "biomesoplenty:tundra";
        polarWastes.river1 = "biomesoplenty:dryland";
        polarWastes.peak1 = "minecraft:stony_peaks";
        polarWastes.peak2 = "minecraft:stony_shore";

        // Paint strictly over the lower thermal climate ranges
        polarWastes.temperaturesList.addAll(List.of(BiomeConfig.Temperature.LOW));
        polarWastes.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        polarWastes.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        polarWastes.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(polarWastes);

        return config;
    }
}