package advRocketry.Worldgen.Presets;

import advRocketry.Worldgen.BiomeConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.*;

public class OVERWORLD {
    public static String name = "overworld.json";

    public static BiomeConfig create() {

        BiomeConfig config = new BiomeConfig();

        // 1. Grab Vanilla's public Overworld climate parameter map (returns ResourceKeys!)
        Climate.ParameterList<ResourceKey<Biome>> overworldKeys =
                MultiNoiseBiomeSourceParameterList.knownPresets().get(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD);

        if (overworldKeys == null) {
            throw new RuntimeException("Failed to load vanilla Overworld preset parameters.");
        }

        // 2. Pre-calculate midpoints for your weirdness zones mapped to float coordinates
        long wBiome1 = Climate.quantizeCoord((-BiomeConfig.PEAK_START - BiomeConfig.VALLEY) / 2.0f);
        long wPeak1  = Climate.quantizeCoord((-BiomeConfig.PEAK_END - BiomeConfig.PEAK_START) / 2.0f);
        long wRiver  = Climate.quantizeCoord(0.0f); // Dead center of valley
        long wBiome2 = Climate.quantizeCoord((BiomeConfig.VALLEY + BiomeConfig.PEAK_START) / 2.0f);
        long wPeak2  = Climate.quantizeCoord((BiomeConfig.PEAK_START + BiomeConfig.PEAK_END) / 2.0f);

        long depth = Climate.quantizeCoord(0.0f); // Default surface depth

        // 3. Brute force grid sweep through all possible combinations
        for (BiomeConfig.Temperature temp : BiomeConfig.Temperature.values()) {
            long t = (temp.value.min() + temp.value.max()) / 2L;

            for (BiomeConfig.Humidity humidity : BiomeConfig.Humidity.values()) {
                long h = (humidity.value.min() + humidity.value.max()) / 2L;

                for (BiomeConfig.Continentalness continentalness : BiomeConfig.Continentalness.values()) {
                    long c = (continentalness.value.min() + continentalness.value.max()) / 2L;

                    for (BiomeConfig.Erosion erosion : BiomeConfig.Erosion.values()) {
                        long e = (erosion.value.min() + erosion.value.max()) / 2L;

                        // Create a unique 1-to-1 definition mapping for this grid intersection
                        BiomeConfig.BiomeDefinition def = new BiomeConfig.BiomeDefinition();
                        def.temperaturesList.add(temp);
                        def.humidityList.add(humidity);
                        def.continentalnessList.add(continentalness);
                        def.erosionList.add(erosion);

                        // 4. Query Vanilla to see what biome ResourceKey it would place here
                        def.biome1 = getVanillaBiomeAt(overworldKeys, t, h, c, e, depth, wBiome1);
                        def.peak1  = getVanillaBiomeAt(overworldKeys, t, h, c, e, depth, wPeak1);
                        def.river1 = getVanillaBiomeAt(overworldKeys, t, h, c, e, depth, wRiver);
                        def.biome2 = getVanillaBiomeAt(overworldKeys, t, h, c, e, depth, wBiome2);
                        def.peak2  = getVanillaBiomeAt(overworldKeys, t, h, c, e, depth, wPeak2);

                        config.biomes.add(def);
                    }
                }
            }
        }

        return config;
    }

    private static String getVanillaBiomeAt(Climate.ParameterList<ResourceKey<Biome>> params, long t, long h, long c, long e, long d, long w) {
        // Construct the TargetPoint using quantized longs
        Climate.TargetPoint target = new Climate.TargetPoint(t, h, c, e, d, w);
        ResourceKey<Biome> biomeKey = params.findValue(target);

        // Directly convert the resource key location into a clean identifier string (e.g., "minecraft:desert")
        return biomeKey.location().toString();
    }
}
