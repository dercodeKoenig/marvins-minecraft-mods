package advRocketry.worldgen.presets;

import advRocketry.worldgen.BiomeConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;

import java.util.List;

public class HOT_DRY {
    public static String name = "hot_dry.json";

    public static BiomeConfig create() {
        BiomeConfig config = new BiomeConfig();


        BiomeConfig.BiomeDefinition warm_ocean = new BiomeConfig.BiomeDefinition();
        warm_ocean.biome1 = Biomes.WARM_OCEAN.location().toString();
        warm_ocean.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        warm_ocean.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        warm_ocean.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.DEEP_OCEAN));
        warm_ocean.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        warm_ocean.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(warm_ocean);



        BiomeConfig.BiomeDefinition wooded_badlands = new BiomeConfig.BiomeDefinition();
        wooded_badlands.biome1 = Biomes.WOODED_BADLANDS.location().toString();
        wooded_badlands.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        wooded_badlands.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        wooded_badlands.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        wooded_badlands.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(wooded_badlands);

        /*
        BiomeConfig.BiomeDefinition desert = new BiomeConfig.BiomeDefinition();
        desert.biome1 = ResourceLocation.fromNamespaceAndPath("minecraft", "desert");
        desert.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        desert.humidityList.addAll(List.of(BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.VERY_DRY));
        desert.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        desert.erosionList.addAll(List.of(BiomeConfig.Erosion.HIGH, BiomeConfig.Erosion.VERY_HIGH));
        config.biomes.add(desert);

        BiomeConfig.BiomeDefinition savanna = new BiomeConfig.BiomeDefinition();
        savanna.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "savanna");
        savanna.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        savanna.humidityList.addAll(List.of(BiomeConfig.Humidity.MID, BiomeConfig.Humidity.WET, BiomeConfig.Humidity.VERY_WET));
        savanna.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.NEAR_INLAND, BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        savanna.erosionList.addAll(List.of(BiomeConfig.Erosion.HIGH, BiomeConfig.Erosion.VERY_HIGH));
        config.biomes.add(savanna);

        BiomeConfig.BiomeDefinition savanna_plateau = new BiomeConfig.BiomeDefinition();
        savanna_plateau.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "savanna_plateau");
        savanna_plateau.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        savanna_plateau.humidityList.addAll(List.of(BiomeConfig.Humidity.MID, BiomeConfig.Humidity.WET, BiomeConfig.Humidity.VERY_WET));
        savanna_plateau.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.NEAR_INLAND, BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        savanna_plateau.erosionList.addAll(List.of(BiomeConfig.Erosion.LOW, BiomeConfig.Erosion.VERY_LOW));
        config.biomes.add(savanna_plateau);

        BiomeConfig.BiomeDefinition badlands = new BiomeConfig.BiomeDefinition();
        badlands.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "badlands");
        badlands.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        badlands.humidityList.addAll(List.of(BiomeConfig.Humidity.MID, BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.VERY_DRY));
        badlands.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.COAST, BiomeConfig.Continentalness.NEAR_INLAND, BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        badlands.erosionList.addAll(List.of(BiomeConfig.Erosion.LOW, BiomeConfig.Erosion.VERY_LOW));
        config.biomes.add(badlands);

        BiomeConfig.BiomeDefinition eroded_badlands = new BiomeConfig.BiomeDefinition();
        eroded_badlands.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "eroded_badlands");
        eroded_badlands.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        eroded_badlands.humidityList.addAll(List.of(BiomeConfig.Humidity.MID, BiomeConfig.Humidity.DRY, BiomeConfig.Humidity.VERY_DRY));
        eroded_badlands.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.COAST, BiomeConfig.Continentalness.NEAR_INLAND, BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        eroded_badlands.erosionList.addAll(List.of(BiomeConfig.Erosion.HIGH, BiomeConfig.Erosion.VERY_HIGH));
        config.biomes.add(eroded_badlands);

        BiomeConfig.BiomeDefinition wooded_badlands = new BiomeConfig.BiomeDefinition();
        wooded_badlands.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "wooded_badlands");
        wooded_badlands.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        wooded_badlands.humidityList.addAll(List.of(BiomeConfig.Humidity.WET, BiomeConfig.Humidity.VERY_WET));
        wooded_badlands.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.COAST, BiomeConfig.Continentalness.NEAR_INLAND, BiomeConfig.Continentalness.MID_INLAND, BiomeConfig.Continentalness.FAR_INLAND));
        wooded_badlands.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW, BiomeConfig.Erosion.LOW));
        config.biomes.add(wooded_badlands);

        BiomeConfig.BiomeDefinition stony_peaks = new BiomeConfig.BiomeDefinition();
        stony_peaks.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "stony_peaks");
        stony_peaks.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        stony_peaks.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        stony_peaks.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));
        stony_peaks.erosionList.addAll(List.of(BiomeConfig.Erosion.VERY_LOW));
        config.biomes.add(stony_peaks);

        BiomeConfig.BiomeDefinition stony_shore = new BiomeConfig.BiomeDefinition();
        stony_shore.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "stony_shore");
        stony_shore.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        stony_shore.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        stony_shore.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.COAST, BiomeConfig.Continentalness.OCEAN));
        stony_shore.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(stony_shore);

        BiomeConfig.BiomeDefinition beach = new BiomeConfig.BiomeDefinition();
        beach.biome = ResourceLocation.fromNamespaceAndPath("minecraft", "beach");
        beach.temperaturesList.addAll(List.of(BiomeConfig.Temperature.values()));
        beach.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
        beach.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.COAST, BiomeConfig.Continentalness.OCEAN));
        beach.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
        config.biomes.add(beach);


         */
        return config;
    }
}
