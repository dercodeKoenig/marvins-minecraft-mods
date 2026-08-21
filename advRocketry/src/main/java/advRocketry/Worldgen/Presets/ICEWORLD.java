package advRocketry.Worldgen.Presets;

import advRocketry.Worldgen.BiomeConfig;

import java.util.List;

/**
 * An ice-covered world preset dominated by the {@code adv_rocketry:ice_crystals} biome,
 * interspersed with vanilla ice biomes in oceans, rivers, and coastal areas.
 * <p>
 * The entire climate grid is frozen, ensuring every inland region is either
 * ice crystals or snowy terrain, while water bodies are replaced by frozen
 * oceans and frozen rivers.
 */
public class ICEWORLD {
    public static String name = "iceworld.json";

    public static BiomeConfig create() {
        BiomeConfig config = new BiomeConfig();

        // ==========================================
        // 1. BASELINE — ALL CLIMATE GRID COVERS
        // ==========================================
        // Fill the entire climate grid with ice_crystals as the dominant biome,
        // so no valid climate combination is left without a biome.
        BiomeConfig.BiomeDefinition baseline = new BiomeConfig.BiomeDefinition();
        baseline.biome1 = "adv_rocketry:ice_crystals";
        baseline.biome2 = "minecraft:ice_spikes";
        baseline.river1 = "minecraft:frozen_river";
        baseline.peak1 = "minecraft:ice_spikes";
        baseline.peak2 = "minecraft:ice_spikes";

        baseline.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        baseline.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        baseline.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        baseline.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(baseline);

        // ==========================================
        // 2. OCEANS & COASTS — FROZEN WATERS
        // ==========================================
        // Coastal regions and ocean floors get snowy coasts and deep-frozen oceans.
        BiomeConfig.BiomeDefinition oceans = new BiomeConfig.BiomeDefinition();
        oceans.biome1 = "adv_rocketry:ice_crystals";
        oceans.biome2 = "minecraft:ice_spikes";
        oceans.river1 = "minecraft:frozen_river";
        oceans.peak1 = "minecraft:ice_spikes";
        oceans.peak2 = "minecraft:ice_spikes";

        oceans.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        oceans.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        oceans.continentalnessList.addAll(List.of(
                BiomeConfig.Continentalness.OCEAN,
                BiomeConfig.Continentalness.DEEP_OCEAN,
                BiomeConfig.Continentalness.COAST
        ));
        oceans.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(oceans);

        // ==========================================
        // 3. RIVERS — ALL FROZEN
        // ==========================================
        // Low-erosion river valleys are frozen rivers.
        BiomeConfig.BiomeDefinition rivers = new BiomeConfig.BiomeDefinition();
        rivers.biome1 = "minecraft:frozen_river";
        rivers.temperaturesList.addAll(List.of(BiomeConfig.Temperature.LOW, BiomeConfig.Temperature.MID));
        rivers.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        rivers.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        rivers.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW));
        config.biomes.add(rivers);

        // ==========================================
        // 4. SPARSE ICE BERG ZONES (High Erosion Flats)
        // ==========================================
        // Smooth, flat high-erosion terrain becomes frozen oceans.
        BiomeConfig.BiomeDefinition flats = new BiomeConfig.BiomeDefinition();
        flats.biome1 = "minecraft:deep_frozen_ocean";
        flats.river1 = "minecraft:frozen_river";
        flats.temperaturesList.addAll(List.of(BiomeConfig.Temperature.FROZEN, BiomeConfig.Temperature.LOW));
        flats.humidityList.addAll(List.of(BiomeConfig.Humidity.VERY_DRY, BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.MID));
        flats.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        flats.erosionList.addAll(List.of(BiomeConfig.Erosion.HIGH, BiomeConfig.Erosion.VERY_HIGH));
        config.biomes.add(flats);

        return config;
    }
}
