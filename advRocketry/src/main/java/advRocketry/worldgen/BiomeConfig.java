package advRocketry.worldgen;

import advRocketry.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/*
maybe select like this:
temperature super heated? -> volcanic biomes
temperature too hot? -> generate too hot & very dry biomes - no water can exist
too little co2 / o2 -> {
    is it warm? -> generate low pressure warm biomes without vegetation
    is it cold? -> generate low pressure cold biomes without vegetation
}
pressure too high? {
    // like normal pressure but with less vegetation
    warm + humid?
    warm + dry?
    cold? ice spikes and stuff
}
is it frozen? -> generate frozen biomes, can have small frozen vegetation
// normal pressure / temp
is it hot and dry?
is it cold and dry?
is it warm and humid?
is it cold and humid
is it midwarm and dry?
is it midwarm and humid?
 */

public class BiomeConfig {
    public List<BiomeDefinition> biomes = new ArrayList<>();

    public List<Pair<Climate.ParameterPoint, Holder<Biome>>> createBiomesFor(temperature temp, humidity humidity, erosion erosion, continentalness continentalness) {
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
            Holder<Biome> biomeHolder = ServerLifecycleHooks.getCurrentServer().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME, i.biome));
            biomes.add(Pair.of(point, biomeHolder));
            //System.out.println(ServerLifecycleHooks.getCurrentServer().registryAccess().registryOrThrow(Registries.BIOME).getKey(biomeHolder.value()) +":"+weirdnessStepSize+":"+(-1 + weirdnessStepSize * n)+":"+(-1 + weirdnessStepSize * (n + 1)));
            n += 1;
        }
        return biomes;
    }

    public List<Pair<Climate.ParameterPoint, Holder<Biome>>> createBiomeConfig() {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomes = new ArrayList<>();
        for (temperature temp : temperature.values()) {
            for (BiomeConfig.continentalness continentalness : continentalness.values()) {
                for (BiomeConfig.humidity humidity : humidity.values()) {
                    for (BiomeConfig.erosion erosion : erosion.values()) {
                        biomes.addAll(createBiomesFor(temp, humidity, erosion, continentalness));
                    }
                }
            }
        }
        return biomes;
    }


    public static BiomeConfig fromConfig(String configStr) {
        return new Gson().fromJson(configStr, BiomeConfig.class);
    }

    public static BiomeConfig fromConfig(Path configPath) {
        String configStr = null;
        try {
            configStr = Files.readString(configPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fromConfig(configStr);
    }

    public static BiomeConfig loadPreset(String presetName) {
        return BiomeConfig.fromConfig(Path.of(Main.myConfigDir.toString(), presetName));
    }

    public static void makePresetIfNotExist(String presetName, BiomeConfig biomeConfig) {
        Path presetPath = Path.of(Main.myConfigDir.toString(), presetName);
        if (Files.exists(presetPath)) return;
        String configStr = new GsonBuilder().setPrettyPrinting().create().toJson(biomeConfig);
        try {
            Files.writeString(presetPath, configStr, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (
                IOException e) {
            throw new RuntimeException(e);
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

    public enum humidity {
        VERY_WET(BiomeDefinition.overworldBiomeBuilder.getHumidityThresholds()[0]),
        WET(BiomeDefinition.overworldBiomeBuilder.getHumidityThresholds()[1]),
        MID(BiomeDefinition.overworldBiomeBuilder.getHumidityThresholds()[2]),
        DRY(BiomeDefinition.overworldBiomeBuilder.getHumidityThresholds()[3]),
        VERY_DRY(BiomeDefinition.overworldBiomeBuilder.getHumidityThresholds()[4]);

        public final Climate.Parameter value;

        humidity(Climate.Parameter value) {
            this.value = value;
        }
    }

    public enum temperature {
        FROZEN(BiomeDefinition.overworldBiomeBuilder.getTemperatureThresholds()[0]),
        LOW(BiomeDefinition.overworldBiomeBuilder.getTemperatureThresholds()[1]),
        MID(BiomeDefinition.overworldBiomeBuilder.getTemperatureThresholds()[2]),
        WARM(BiomeDefinition.overworldBiomeBuilder.getTemperatureThresholds()[3]),
        HOT(BiomeDefinition.overworldBiomeBuilder.getTemperatureThresholds()[4]);

        public final Climate.Parameter value;

        temperature(Climate.Parameter value) {
            this.value = value;
        }
    }


    public static class BiomeDefinition {
        public ResourceLocation biome;
        public List<BiomeConfig.temperature> temperaturesList = new ArrayList<>();
        public List<BiomeConfig.humidity> humidityList = new ArrayList<>();
        public List<BiomeConfig.continentalness> continentalnessList = new ArrayList<>();
        public List<BiomeConfig.erosion> erosionList = new ArrayList<>();

        public static OverworldBiomeBuilder overworldBiomeBuilder = new OverworldBiomeBuilder();

    }

}