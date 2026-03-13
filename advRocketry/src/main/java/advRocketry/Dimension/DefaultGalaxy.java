package advRocketry.Dimension;

import advRocketry.Worldgen.presets.HOT;
import advRocketry.Worldgen.presets.MOON;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static advRocketry.Dimension.PlanetDimensionProperties.SEA_LEVEL_OVERWORLD;
import static advRocketry.Dimension.PlanetDimensionProperties.SKY_COLOR_OVERWORLD;

public class DefaultGalaxy {

    public static List<String> createDefaultGalaxy() {

        ArrayList<DimensionProperties> galaxy = new ArrayList<>();

        PlanetDimensionProperties sun = new PlanetDimensionProperties();
        sun.name = "Sun";
        sun.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "sun");
        sun.gravitationalMultiplier = 200;
        sun.earthRadiusMultiplier = 100;
        sun.rotationAxis = new Vec3(0, 1, 0).normalize();
        sun.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/sun_grayscale_ico_1k.png");
        sun.emissiveColor = new Vector3f(1f, 1f, 0.8f);
        sun.radiationIntensity = 2;
        sun.isKnown = true;
        galaxy.add(sun);

        PlanetDimensionProperties overworld = new PlanetDimensionProperties();
        overworld.name = "Earth";
        overworld.dimensionId = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        overworld.parentDimensionId = sun.dimensionId;
        overworld.dayTimeReference = sun.dimensionId;
        overworld.isKnown = true;
        overworld.canVisit = true;
        overworld.currentTemp = 300;
        overworld.seaLevel = SEA_LEVEL_OVERWORLD();
        overworld.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/earth_ico_1k.png");
        overworld.skyColor = SKY_COLOR_OVERWORLD();
        overworld.atmosphereComposition.put(GasRegistry.oxygen, new PlanetDimensionProperties.GasProperty(0.3f, 0));
        overworld.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.7f, 0));
        overworld.atmosphereComposition.put(GasRegistry.co2, new PlanetDimensionProperties.GasProperty(0.05f, 0));
        galaxy.add(overworld);

        PlanetDimensionProperties moon = new PlanetDimensionProperties();
        moon.name = "moon";
        moon.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon");
        moon.parentDimensionId = overworld.dimensionId;
        moon.dayTimeReference = sun.dimensionId;
        moon.orbitalDistanceToParent = 0.00257f;
        moon.orbitAxis = new Vec3(0.1, 1, 0.1);
        moon.earthRadiusMultiplier = 0.272f;
        moon.gravitationalMultiplier = 0.3f;
        moon.targetDayLength = 12000;
        moon.canVisit = true;
        moon.currentTemp = 300;
        moon.seaLevel = SEA_LEVEL_OVERWORLD();
        moon.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_1k.png");
        moon.skyColor = SKY_COLOR_OVERWORLD();
        moon.hasRingSystem = true;
        moon.biomePreset = MOON.name;
        galaxy.add(moon);


        PlanetDimensionProperties moon2 = new PlanetDimensionProperties();
        moon2.name = "little moon";
        moon2.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "little_moon");
        moon2.parentDimensionId = overworld.dimensionId;
        moon2.dayTimeReference = sun.dimensionId;
        moon2.orbitalDistanceToParent = 0.0032f;
        moon2.orbitAxis = new Vec3(-0.1, 1, 0.2);
        moon2.earthRadiusMultiplier = 0.1f;
        moon2.gravitationalMultiplier = 0.1f;
        moon2.targetDayLength = -1000;
        moon2.seaLevel = 50;
        moon2.currentTemp = 300;
        moon2.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_1k.png");
        moon2.canVisit = true;
        moon2.biomePreset = HOT.name;
        galaxy.add(moon2);


        PlanetDimensionProperties venus = new PlanetDimensionProperties();
        venus.name = "Venus";
        venus.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "venus");
        venus.parentDimensionId = sun.dimensionId;
        venus.dayTimeReference = sun.dimensionId;
        venus.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/venus_atm_ico_1k.png");
        venus.orbitalDistanceToParent = 0.5f;
        venus.cloudColor = new Vector3f(194, 155, 64).mul(1f / 255);
        venus.canVisit = true;
        venus.biomePreset = HOT.name;
        venus.seaLevel = 10;
        venus.currentTemp = 500;
        venus.skyColor = new Vector3f(139, 69, 19).mul(1f / 255);
        venus.fogColor = new Vector3f(200, 130, 0).mul(1f / 255);
        venus.atmosphereComposition.put(GasRegistry.co2, new PlanetDimensionProperties.GasProperty(1, 0));
        venus.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.1f, 0));
        galaxy.add(venus);


        PlanetDimensionProperties jupyter = new PlanetDimensionProperties();
        jupyter.name = "Jupyter";
        jupyter.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "jupyter");
        jupyter.parentDimensionId = sun.dimensionId;
        jupyter.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/jupyter_ico_1k.png");
        jupyter.orbitalDistanceToParent = 2f;
        jupyter.earthRadiusMultiplier = 10f;
        jupyter.gravitationalMultiplier = 30f;
        jupyter.atmosphereComposition.put(GasRegistry.hydrogen, new PlanetDimensionProperties.GasProperty(3, 0));
        jupyter.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.1f, 0));
        galaxy.add(jupyter);


        PlanetDimensionProperties saturn = new PlanetDimensionProperties();
        saturn.name = "Saturn";
        saturn.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "saturn");
        saturn.parentDimensionId = sun.dimensionId;
        saturn.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/saturn_ico_1k.png");
        saturn.orbitalDistanceToParent = 3f;
        saturn.earthRadiusMultiplier = 3f;
        saturn.gravitationalMultiplier = 10f;
        saturn.hasRingSystem = true;
        saturn.atmosphereComposition.put(GasRegistry.hydrogen, new PlanetDimensionProperties.GasProperty(3, 0));
        saturn.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.1f, 0));
        galaxy.add(saturn);


        PlanetDimensionProperties distantStar = new PlanetDimensionProperties();
        distantStar.name = "Blue Star";
        distantStar.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "blue_star");
        distantStar.gravitationalMultiplier = 300;
        distantStar.earthRadiusMultiplier = 300;
        distantStar.rotationAxis = new Vec3(0, 1, 0).normalize();
        distantStar.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/sun_grayscale_ico_1k.png");
        distantStar.emissiveColor = new Vector3f(0.5f, 0.8f, 4f);
        distantStar.radiationIntensity = 1;
        distantStar.position = new Vec3(50, 2, 0);
        //galaxy.add(distantStar);


        List<String> dimensionProperties = new ArrayList<>();
        for (DimensionProperties i : galaxy) {
            dimensionProperties.add(new GsonBuilder().setPrettyPrinting().create().toJson(i));
        }
        return dimensionProperties;
    }
}
