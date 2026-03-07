package advRocketry.Worldgen;

import advRocketry.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static advRocketry.Utils.Utils.getBiomeHolder;

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
    public static String PRESET_DIRECTORY = "biomePresets";
    public static float VALLEY = 0.05f;
    public static float PEAK_START = 0.56666666f;
    public static float PEAK_END = 0.7666667f;

    public List<BiomeDefinition> biomes = new ArrayList<>();

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
        return BiomeConfig.fromConfig(Path.of(Main.myConfigDir.toString(), PRESET_DIRECTORY, presetName));
    }

    public static void makePresetIfNotExist(String presetName, BiomeConfig biomeConfig) {
        Path presetDir = Path.of(Main.myConfigDir.toString(),  PRESET_DIRECTORY);
        Path presetPath = Path.of(presetDir.toString(), presetName);
        if (Files.exists(presetPath)) return;
        String configStr = new GsonBuilder().setPrettyPrinting().create().toJson(biomeConfig);
        try {
            Files.createDirectories(presetDir);
            Files.writeString(presetPath, configStr, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Pair<Climate.ParameterPoint, Holder<Biome>>> createBiomesFor(Temperature temp, Humidity humidity, Erosion erosion, Continentalness continentalness) {
        BiomeDefinition definition = null;
        for (BiomeDefinition i : biomes) {
            if (i.continentalnessList.contains(continentalness)) {
                if (i.temperaturesList.contains(temp)) {
                    if (i.humidityList.contains(humidity)) {
                        if (i.erosionList.contains(erosion)) {
                            definition = i; // overwrite early definitions with later defined definitions
                        }
                    }
                }
            }
        }

        if (definition == null) {
            String values = "";
            values += "temp:" + temp + "\n";
            values += "humid:" + humidity + "\n";
            values += "erosion:" + erosion + "\n";
            values += "continentalness:" + continentalness + "\n";
            throw new RuntimeException("There is no biome definition for the following values: \n" + values);
        }

        if(definition.biome2 == null)
            definition.biome2 = definition.biome1;
        if(definition.river1 == null)
            definition.river1 = definition.biome1;
        if(definition.peak1 == null)
            definition.peak1 = definition.biome1;
        if(definition.peak2 == null)
            definition.peak2 = definition.biome1;

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomes = new ArrayList<>();

        biomes.add(Pair.of(
                new Climate.ParameterPoint(
                        temp.value, humidity.value, continentalness.value, erosion.value, Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-2, -PEAK_END),
                        0
                ),
                getBiomeHolder(definition.biome1))
        );

        biomes.add(Pair.of(
                new Climate.ParameterPoint(
                        temp.value, humidity.value, continentalness.value, erosion.value, Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-PEAK_END, -PEAK_START),
                        0
                ),
                getBiomeHolder(definition.peak1))
        );

        biomes.add(Pair.of(
                new Climate.ParameterPoint(
                        temp.value, humidity.value, continentalness.value, erosion.value, Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-PEAK_START, -VALLEY),
                        0
                ),
                getBiomeHolder(definition.biome1))
        );

        biomes.add(Pair.of(
                new Climate.ParameterPoint(
                        temp.value, humidity.value, continentalness.value, erosion.value, Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-VALLEY, VALLEY),
                        0
                ),
                getBiomeHolder(definition.river1))
        );

        biomes.add(Pair.of(
                new Climate.ParameterPoint(
                        temp.value, humidity.value, continentalness.value, erosion.value, Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(VALLEY, PEAK_START),
                        0
                ),
                getBiomeHolder(definition.biome2))
        );

        biomes.add(Pair.of(
                new Climate.ParameterPoint(
                        temp.value, humidity.value, continentalness.value, erosion.value, Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(PEAK_START, PEAK_END),
                        0
                ),
                getBiomeHolder(definition.peak2))
        );

        biomes.add(Pair.of(
                new Climate.ParameterPoint(
                        temp.value, humidity.value, continentalness.value, erosion.value, Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(PEAK_END, 2),
                        0
                ),
                getBiomeHolder(definition.biome2))
        );


        return biomes;
    }

    public List<Pair<Climate.ParameterPoint, Holder<Biome>>> createBiomeConfig() {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomes = new ArrayList<>();
        for (Temperature temp : Temperature.values()) {
            for (Continentalness continentalness : Continentalness.values()) {
                for (Humidity humidity : Humidity.values()) {
                    for (Erosion erosion : Erosion.values()) {
                        biomes.addAll(createBiomesFor(temp, humidity, erosion, continentalness));
                    }
                }
            }
        }
        return biomes;
    }

    public enum Erosion {
        VERY_LOW(Climate.Parameter.span(-2F, -0.5F)),
        LOW(Climate.Parameter.span(-0.5F, 0)),
        HIGH(Climate.Parameter.span(0, 0.5F)),
        VERY_HIGH(Climate.Parameter.span(0.5f, 2));

        public final Climate.Parameter value;

        Erosion(Climate.Parameter value) {
            this.value = value;
        }
    }

    public enum Continentalness {
        DEEP_OCEAN(Climate.Parameter.span(-2.00F, -0.455F)),
        OCEAN(Climate.Parameter.span(-0.455F, -0.19F)),
        COAST(Climate.Parameter.span(-0.19F, -0.11F)),
        NEAR_INLAND(Climate.Parameter.span(-0.11F, 0.03F)),
        MID_INLAND(Climate.Parameter.span(0.03F, 0.3F)),
        FAR_INLAND(Climate.Parameter.span(0.3F, 2.0F));

        public final Climate.Parameter value;

        Continentalness(Climate.Parameter value) {
            this.value = value;
        }
    }

    public enum Humidity {
        VERY_WET(BiomeDefinition.overworldBiomeBuilder.getHumidityThresholds()[0]),
        WET(BiomeDefinition.overworldBiomeBuilder.getHumidityThresholds()[1]),
        MID(BiomeDefinition.overworldBiomeBuilder.getHumidityThresholds()[2]),
        DRY(BiomeDefinition.overworldBiomeBuilder.getHumidityThresholds()[3]),
        VERY_DRY(BiomeDefinition.overworldBiomeBuilder.getHumidityThresholds()[4]);

        public final Climate.Parameter value;

        Humidity(Climate.Parameter value) {
            this.value = value;
        }
    }

    public enum Temperature {
        FROZEN(BiomeDefinition.overworldBiomeBuilder.getTemperatureThresholds()[0]),
        LOW(BiomeDefinition.overworldBiomeBuilder.getTemperatureThresholds()[1]),
        MID(BiomeDefinition.overworldBiomeBuilder.getTemperatureThresholds()[2]),
        WARM(BiomeDefinition.overworldBiomeBuilder.getTemperatureThresholds()[3]),
        HOT(BiomeDefinition.overworldBiomeBuilder.getTemperatureThresholds()[4]);

        public final Climate.Parameter value;

        Temperature(Climate.Parameter value) {
            this.value = value;
        }
    }


    public static class BiomeDefinition {
        public static OverworldBiomeBuilder overworldBiomeBuilder = new OverworldBiomeBuilder();
        public String biome1;
        public String biome2;
        public String river1;
        public String peak1;
        public String peak2;
        public List<Temperature> temperaturesList = new ArrayList<>();
        public List<Humidity> humidityList = new ArrayList<>();
        public List<Continentalness> continentalnessList = new ArrayList<>();
        public List<Erosion> erosionList = new ArrayList<>();

    }

}