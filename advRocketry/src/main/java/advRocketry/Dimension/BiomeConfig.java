package advRocketry.Dimension;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

public class BiomeConfig {
    List<BiomeDefinition> biomes = new ArrayList<>();

    public List<Pair<Climate.ParameterPoint, Holder<Biome>>> createBiomesFor(BiomeDefinition.temperature temp, BiomeDefinition.humidity humidity, BiomeDefinition.erosion erosion, BiomeDefinition.continentalness continentalness) {
        List<BiomeDefinition> matchingDefinitions = new ArrayList<>();
        for (BiomeDefinition i : biomes) {
            if (i.continentalnessList.contains(continentalness)) {
                if (i.temperaturesList.contains(temp)) {
                    if (i.humidityList.contains(humidity)) {
                        if (i.erosionList.contains(erosion)) {
                            matchingDefinitions.add(i);
                        }
                    }
                }
            }
        }

        int num_matching = matchingDefinitions.size();
        float weirdnessStepSize = 2f / num_matching;
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomes = new ArrayList<>();
        int n = 0;
        for (BiomeDefinition i : matchingDefinitions) {
            Climate.ParameterPoint point = new Climate.ParameterPoint(
                    temp.value,
                    humidity.value,
                    continentalness.value,
                    erosion.value,
                    Climate.Parameter.span(-1, 1),
                    Climate.Parameter.span(-1 + weirdnessStepSize * n, -1 + weirdnessStepSize * (n + 1)),
                    0
            );
            Holder<Biome> biomeHolder = ServerLifecycleHooks.getCurrentServer().registryAccess().registry(Registries.BIOME).get().getHolder(ResourceKey.create(Registries.BIOME, i.biome)).get();
            biomes.add(Pair.of(point, biomeHolder));
        }
        return biomes;
    }

    public List<Pair<Climate.ParameterPoint, Holder<Biome>>> createBiomeConfig(){
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomes = new ArrayList<>();
        for (BiomeDefinition.temperature temp : BiomeDefinition.temperature.values()){
            for (BiomeDefinition.continentalness continentalness : BiomeDefinition.continentalness.values()){
                for (BiomeDefinition.humidity humidity : BiomeDefinition.humidity.values()){
                    for (BiomeDefinition.erosion erosion : BiomeDefinition.erosion.values()){
                        biomes.addAll(createBiomesFor(temp,humidity,erosion,continentalness));
                    }
                }
            }
        }
        return biomes;
    }


    public static class BiomeDefinition {
        ResourceLocation biome;
        List<temperature> temperaturesList;
        List<humidity> humidityList;
        List<continentalness> continentalnessList;
        List<erosion> erosionList;
        long offset;

        public static OverworldBiomeBuilder overworldBiomeBuilder;

        public enum temperature {
            FROZEN(overworldBiomeBuilder.getTemperatureThresholds()[0]),
            LOW(overworldBiomeBuilder.getTemperatureThresholds()[1]),
            MID(overworldBiomeBuilder.getTemperatureThresholds()[2]),
            WARM(overworldBiomeBuilder.getTemperatureThresholds()[3]),
            HOT(overworldBiomeBuilder.getTemperatureThresholds()[4]);

            public final Climate.Parameter value;

            temperature(Climate.Parameter value) {
                this.value = value;
            }
        }

        public enum humidity {
            VERY_WET(overworldBiomeBuilder.getHumidityThresholds()[0]),
            WET(overworldBiomeBuilder.getHumidityThresholds()[1]),
            MID(overworldBiomeBuilder.getHumidityThresholds()[2]),
            DRY(overworldBiomeBuilder.getHumidityThresholds()[3]),
            VERY_DRY(overworldBiomeBuilder.getHumidityThresholds()[4]);

            public final Climate.Parameter value;

            humidity(Climate.Parameter value) {
                this.value = value;
            }
        }

        public enum continentalness {
            DEEP_OCEAN(Climate.Parameter.span(-2.00F, -0.455F)),
            OCEAN(Climate.Parameter.span(-0.455F, -0.19F)),
            COAST(Climate.Parameter.span(-0.19F, -0.11F)),
            NEAR_INLAND(Climate.Parameter.span(-0.11F, 0.03F)),
            MID_INLAND(Climate.Parameter.span(0.03F, 0.3F)),
            FAR_INLAND(Climate.Parameter.span(0.3F, 2.0F));

            public final Climate.Parameter value;

            continentalness(Climate.Parameter value) {
                this.value = value;
            }
        }

        public enum erosion {
            VERY_LOW(Climate.Parameter.span(-2F, -0.5F)),
            LOW(Climate.Parameter.span(-0.5F, 0)),
            HIGH(Climate.Parameter.span(0, 0.5F)),
            VERY_HIGH(Climate.Parameter.span(-0.5f, 2));

            public final Climate.Parameter value;

            erosion(Climate.Parameter value) {
                this.value = value;
            }
        }
    }

}