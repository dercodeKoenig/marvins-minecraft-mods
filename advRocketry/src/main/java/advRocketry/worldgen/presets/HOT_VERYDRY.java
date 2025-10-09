package advRocketry.worldgen.presets;

import advRocketry.worldgen.BiomeConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class HOT_VERYDRY {
public static String name="hot_verydry.json";
    public static BiomeConfig create() {
        BiomeConfig config = new BiomeConfig();

        BiomeConfig.BiomeDefinition desert = new BiomeConfig.BiomeDefinition();
        desert.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "desert");
        desert.temperaturesList.addAll(List.of(BiomeConfig.temperature.values()));
        desert.humidityList.addAll(List.of(BiomeConfig.humidity.DRY, BiomeConfig.humidity.VERY_DRY));
        desert.continentalnessList.addAll(List.of(BiomeConfig.continentalness.OCEAN, BiomeConfig.continentalness.COAST, BiomeConfig.continentalness.NEAR_INLAND, BiomeConfig.continentalness.MID_INLAND, BiomeConfig.continentalness.FAR_INLAND));
        desert.erosionList.addAll(List.of(BiomeConfig.erosion.LOW, BiomeConfig.erosion.HIGH, BiomeConfig.erosion.VERY_HIGH));
        config.biomes.add(desert);

        BiomeConfig.BiomeDefinition badlands = new BiomeConfig.BiomeDefinition();
        badlands.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "badlands");
        badlands.temperaturesList.addAll(List.of(BiomeConfig.temperature.values()));
        badlands.humidityList.addAll(List.of(BiomeConfig.humidity.MID, BiomeConfig.humidity.WET, BiomeConfig.humidity.VERY_WET));
        badlands.continentalnessList.addAll(List.of(BiomeConfig.continentalness.COAST, BiomeConfig.continentalness.NEAR_INLAND, BiomeConfig.continentalness.MID_INLAND, BiomeConfig.continentalness.FAR_INLAND));
        badlands.erosionList.addAll(List.of(BiomeConfig.erosion.VERY_LOW, BiomeConfig.erosion.LOW, BiomeConfig.erosion.HIGH, BiomeConfig.erosion.VERY_HIGH));
        config.biomes.add(badlands);

        BiomeConfig.BiomeDefinition eroded_badlands = new BiomeConfig.BiomeDefinition();
        eroded_badlands.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "eroded_badlands");
        eroded_badlands.temperaturesList.addAll(List.of(BiomeConfig.temperature.values()));
        eroded_badlands.humidityList.addAll(List.of(BiomeConfig.humidity.MID, BiomeConfig.humidity.WET, BiomeConfig.humidity.VERY_WET));
        eroded_badlands.continentalnessList.addAll(List.of(BiomeConfig.continentalness.DEEP_OCEAN));
        eroded_badlands.erosionList.addAll(List.of(BiomeConfig.erosion.VERY_HIGH));
        config.biomes.add(eroded_badlands);

        BiomeConfig.BiomeDefinition wooded_badlands = new BiomeConfig.BiomeDefinition();
        wooded_badlands.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "wooded_badlands");
        wooded_badlands.temperaturesList.addAll(List.of(BiomeConfig.temperature.FROZEN, BiomeConfig.temperature.LOW));
        wooded_badlands.humidityList.addAll(List.of(BiomeConfig.humidity.WET, BiomeConfig.humidity.VERY_WET));
        wooded_badlands.continentalnessList.addAll(List.of(BiomeConfig.continentalness.OCEAN, BiomeConfig.continentalness.COAST, BiomeConfig.continentalness.NEAR_INLAND));
        wooded_badlands.erosionList.addAll(List.of(BiomeConfig.erosion.VERY_LOW, BiomeConfig.erosion.LOW, BiomeConfig.erosion.HIGH, BiomeConfig.erosion.VERY_HIGH));
        config.biomes.add(wooded_badlands);

        BiomeConfig.BiomeDefinition stony_peaks = new BiomeConfig.BiomeDefinition();
        stony_peaks.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "stony_peaks");
        stony_peaks.temperaturesList.addAll(List.of(BiomeConfig.temperature.values()));
        stony_peaks.humidityList.addAll(List.of(BiomeConfig.humidity.values()));
        stony_peaks.continentalnessList.addAll(List.of(BiomeConfig.continentalness.values()));
        stony_peaks.erosionList.addAll(List.of(BiomeConfig.erosion.VERY_LOW, BiomeConfig.erosion.LOW));
        config.biomes.add(stony_peaks);

        BiomeConfig.BiomeDefinition stony_shore = new BiomeConfig.BiomeDefinition();
        stony_shore.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "stony_shore");
        stony_shore.temperaturesList.addAll(List.of(BiomeConfig.temperature.values()));
        stony_shore.humidityList.addAll(List.of(BiomeConfig.humidity.values()));
        stony_shore.continentalnessList.addAll(List.of(BiomeConfig.continentalness.DEEP_OCEAN, BiomeConfig.continentalness.OCEAN));
        stony_shore.erosionList.addAll(List.of(BiomeConfig.erosion.values()));
        config.biomes.add(stony_shore);

        BiomeConfig.BiomeDefinition beach = new BiomeConfig.BiomeDefinition();
        beach.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "beach");
        beach.temperaturesList.addAll(List.of(BiomeConfig.temperature.values()));
        beach.humidityList.addAll(List.of(BiomeConfig.humidity.values()));
        beach.continentalnessList.addAll(List.of(BiomeConfig.continentalness.DEEP_OCEAN, BiomeConfig.continentalness.OCEAN));
        beach.erosionList.addAll(List.of(BiomeConfig.erosion.values()));
        config.biomes.add(beach);

        return config;
    }
}
