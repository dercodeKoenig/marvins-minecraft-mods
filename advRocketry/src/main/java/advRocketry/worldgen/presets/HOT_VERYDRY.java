package advRocketry.worldgen.presets;

import advRocketry.worldgen.BiomeConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;

import java.util.List;

public class HOT_VERYDRY {
    public static String name = "hot_verydry.json";

    public static BiomeConfig create() {
        BiomeConfig config = new BiomeConfig();

        BiomeConfig.BiomeDefinition desert = new BiomeConfig.BiomeDefinition();
        desert.biome1 = Biomes.DESERT.location().toString();
        desert.biome2 = Biomes.BASALT_DELTAS.location().toString();
        desert.peak1 = Biomes.STONY_PEAKS.location().toString();
        desert.peak2 = Biomes.STONY_PEAKS.location().toString();        // jagged peak looks nice with basalt delta
        desert.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        desert.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        desert.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        desert.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(desert);

        BiomeConfig.BiomeDefinition badlands = new BiomeConfig.BiomeDefinition();
        badlands.biome1 = Biomes.BADLANDS.location().toString();
        badlands.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        badlands.humidityList.addAll(List.of(BiomeConfig.Humidity.MID, BiomeConfig.Humidity.WET, BiomeConfig.Humidity.VERY_WET));
        badlands.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        badlands.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(badlands);

        BiomeConfig.BiomeDefinition eroded_badlands = new BiomeConfig.BiomeDefinition();
        eroded_badlands.biome1 = Biomes.ERODED_BADLANDS.location().toString();
        eroded_badlands.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        eroded_badlands.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        eroded_badlands.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        eroded_badlands.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_HIGH));
        config.biomes.add(eroded_badlands);

        BiomeConfig.BiomeDefinition wooded_badlands = new BiomeConfig.BiomeDefinition();
        wooded_badlands.biome1 = Biomes.WOODED_BADLANDS.location().toString();
        wooded_badlands.temperaturesList.addAll(List.of(BiomeConfig.Temperature.FROZEN, BiomeConfig.Temperature.LOW));
        wooded_badlands.humidityList.addAll(List.of(BiomeConfig.Humidity.WET, BiomeConfig.Humidity.VERY_WET));
        wooded_badlands.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        wooded_badlands.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(wooded_badlands);


        return config;
    }
}
