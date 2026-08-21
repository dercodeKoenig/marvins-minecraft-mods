package advRocketry.Worldgen.Presets;

import advRocketry.Worldgen.BiomeConfig;

import java.util.List;

public class BiomeConfigCreator {
    public static BiomeConfig create(
            String[][] biomesByTemperatureAndHumidity,
            String[][] peaksByTemperatureAndHumidity,
            String[] riversByTemperature,
            String[] beachesByTemperature,
            String[] oceansByTemperature,
            String[] deepOceansByTemperature
    ) {
        // add temperature / humidity table first
        BiomeConfig config = new BiomeConfig();
        for (int temperature = 0; temperature < BiomeConfig.Temperature.values().length; temperature++) {
            for (int humidity = 0; humidity < BiomeConfig.Humidity.values().length; humidity++) {
                BiomeConfig.BiomeDefinition definition = new BiomeConfig.BiomeDefinition();
                definition.biome1 = biomesByTemperatureAndHumidity[temperature][humidity];
                definition.biome2 = biomesByTemperatureAndHumidity[temperature][humidity];

                // the lists are valid for all erosions
                definition.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));

                // add all definitions for now, if oceans and beaches exist they will overwrite
                definition.continentalnessList.addAll(List.of(BiomeConfig.Continentalness.values()));


                BiomeConfig.Temperature T = BiomeConfig.Temperature.values()[temperature];
                BiomeConfig.Humidity H = BiomeConfig.Humidity.values()[humidity];
                definition.temperaturesList.add(T);
                definition.humidityList.add(H);

                String peak = peaksByTemperatureAndHumidity[temperature][humidity];
                if (peak != null && !peak.equals("null")) {
                    definition.peak1 = peak;
                    definition.peak2 = peak;
                }

                String river = riversByTemperature[temperature];
                if (river != null && !river.equals("null")) {
                    definition.river1 = river;
                }


                config.biomes.add(definition);
            }
        }

        // now add beaches and oceans
        // later defined biome definitions will overwrite previous entries
        for (int temperature = 0; temperature < BiomeConfig.Temperature.values().length; temperature++) {
            String beach = beachesByTemperature[temperature];
            String ocean = oceansByTemperature[temperature];
            String deepOcean = deepOceansByTemperature[temperature];
            BiomeConfig.Temperature T = BiomeConfig.Temperature.values()[temperature];

            if (beach != null && !beach.equals("null")) {
                BiomeConfig.BiomeDefinition definition = new BiomeConfig.BiomeDefinition();
                definition.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
                definition.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
                definition.temperaturesList.add(T);
                definition.continentalnessList.add(BiomeConfig.Continentalness.COAST);
                definition.biome1 = beach;
                config.biomes.add(definition);
            }
            if (ocean != null && !ocean.equals("null")) {
                BiomeConfig.BiomeDefinition definition = new BiomeConfig.BiomeDefinition();
                definition.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
                definition.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
                definition.temperaturesList.add(T);
                definition.continentalnessList.add(BiomeConfig.Continentalness.OCEAN);
                definition.biome1 = ocean;
                config.biomes.add(definition);
            }
            if (deepOcean != null && !deepOcean.equals("null")) {
                BiomeConfig.BiomeDefinition definition = new BiomeConfig.BiomeDefinition();
                definition.erosionList.addAll(List.of(BiomeConfig.Erosion.values()));
                definition.humidityList.addAll(List.of(BiomeConfig.Humidity.values()));
                definition.temperaturesList.add(T);
                definition.continentalnessList.add(BiomeConfig.Continentalness.DEEP_OCEAN);
                definition.biome1 = deepOcean;
                config.biomes.add(definition);
            }

        }


        return config;
    }
}
