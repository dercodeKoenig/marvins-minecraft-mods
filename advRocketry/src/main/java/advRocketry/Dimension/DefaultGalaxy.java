package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.Registry.GasRegistry;
import advRocketry.Worldgen.presets.*;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

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
        sun.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_8k_sun_adjusted.png");
        sun.emissiveLightColor = new Vector3f(1f, 1f, 0.8f).mul(1.2f);
        sun.radiationIntensity = 2;
        sun.emissiveTextureTintColor = new Vector3f(1,1,1).mul(20f);
        sun.isKnown = true;
        galaxy.add(sun);

        PlanetDimensionProperties overworld = new PlanetDimensionProperties();
        overworld.name = "Earth";
        overworld.description = "A nice green planet";
        overworld.dimensionId = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        overworld.parentDimensionId = sun.dimensionId;
        overworld.dayTimeReference = sun.dimensionId;
        overworld.isKnown = true;
        overworld.canVisit = true;
        overworld.currentTemp = 300;
        overworld.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_8k_earth_daymap.png");
        overworld.skyColor = SKY_COLOR_OVERWORLD();
        overworld.atmosphereComposition.put(GasRegistry.oxygen, new PlanetDimensionProperties.GasProperty(0.3f, 0, 0, 0));
        overworld.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.7f, 0,0, 0));
        overworld.atmosphereComposition.put(GasRegistry.co2, new PlanetDimensionProperties.GasProperty(0.001f, 0, 0,0));
        overworld.atmosphereComposition.put(GasRegistry.water, new PlanetDimensionProperties.GasProperty(0, 0.5, 0,0));
        galaxy.add(overworld);

        PlanetDimensionProperties moon = new PlanetDimensionProperties();
        moon.name = "Moon";
        moon.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon");
        moon.parentDimensionId = overworld.dimensionId;
        moon.dayTimeReference = sun.dimensionId;
        moon.orbitalDistanceToParent = 0.00257f;
        moon.orbitAxis = new Vec3(0.1, 1, 0.1);
        moon.earthRadiusMultiplier = 0.272f;
        moon.gravitationalMultiplier = 0.2f;
        moon.targetDayLength = 12000;
        moon.canVisit = true;
        moon.currentTemp = 260;
        moon.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_moon.png");
        moon.skyColor = SKY_COLOR_OVERWORLD();
        moon.hasRingSystem = true;
        moon.biomePreset = MOON.name;
        galaxy.add(moon);


        PlanetDimensionProperties venus = new PlanetDimensionProperties();
        venus.name = "Venus";
        venus.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "venus");
        venus.parentDimensionId = sun.dimensionId;
        venus.dayTimeReference = sun.dimensionId;
        venus.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_8k_venus_surface.png");
        venus.orbitalDistanceToParent = 0.7f;
        venus.cloudColor = new Vector3f(194, 155, 64).mul(1f / 255);
        venus.canVisit = true;
        venus.biomePreset = VENUS.name;
        venus.currentTemp = 500;
        venus.skyColor = new Vector3f(139, 69, 19).mul(1f / 255);
        venus.fogColor = new Vector3f(200, 130, 0).mul(1f / 255);
        venus.atmosphereComposition.put(GasRegistry.co2, new PlanetDimensionProperties.GasProperty(2, 0,0, 0));
        venus.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.1f, 0,0, 0));
        galaxy.add(venus);


        PlanetDimensionProperties jupiter = new PlanetDimensionProperties();
        jupiter.name = "Jupiter";
        jupiter.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "jupiter");
        jupiter.parentDimensionId = sun.dimensionId;
        jupiter.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_8k_jupiter.png");
        jupiter.orbitalDistanceToParent = 5f;
        jupiter.earthRadiusMultiplier = 10f;
        jupiter.gravitationalMultiplier = 30f;
        jupiter.canGasMine = true;
        jupiter.atmosphereComposition.put(GasRegistry.hydrogen, new PlanetDimensionProperties.GasProperty(3,0, 0, 0));
        jupiter.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.5f, 0,0, 0));
        galaxy.add(jupiter);

        PlanetDimensionProperties europa = new PlanetDimensionProperties();
        europa.name = "Europa";
        europa.dimensionId = ResourceLocation.fromNamespaceAndPath(Main.MODID, "europa");
        europa.parentDimensionId = jupiter.dimensionId;
        europa.dayTimeReference = sun.dimensionId;
        europa.currentTemp = 100;
        europa.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_europa.png");;
        europa.orbitalDistanceToParent = 0.02f;
        europa.orbitalBaseOffsetDegrees = 90;
        europa.earthRadiusMultiplier = 0.5f;
        europa.gravitationalMultiplier = 0.3f;
        europa.atmosphereComposition.put(GasRegistry.co2, new PlanetDimensionProperties.GasProperty(0,0,0.5f,0));
        europa.atmosphereComposition.put(GasRegistry.water, new PlanetDimensionProperties.GasProperty(0,0,0.5f,0));
        europa.atmosphereComposition.put(GasRegistry.oxygen, new PlanetDimensionProperties.GasProperty(0,0,0.05f,0));
        europa.canVisit = true;
        europa.biomePreset = MOON.name;
        galaxy.add(europa);

        PlanetDimensionProperties saturn = new PlanetDimensionProperties();
        saturn.name = "Saturn";
        saturn.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "saturn");
        saturn.parentDimensionId = sun.dimensionId;
        saturn.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_8k_saturn.png");
        saturn.orbitalDistanceToParent = 8f;
        saturn.earthRadiusMultiplier = 3f;
        saturn.gravitationalMultiplier = 8f;
        saturn.canGasMine = true;
        saturn.atmosphereComposition.put(GasRegistry.hydrogen, new PlanetDimensionProperties.GasProperty(3,0, 0, 0));
        saturn.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.1f,0, 0, 0));
        saturn.hasRingSystem = true;
        galaxy.add(saturn);

        PlanetDimensionProperties titan = new PlanetDimensionProperties();
        titan.name = "Titan";
        titan.dimensionId = ResourceLocation.fromNamespaceAndPath(Main.MODID, "titan");
        titan.parentDimensionId = saturn.dimensionId;
        titan.dayTimeReference = sun.dimensionId;
        titan.currentTemp = 100;
        titan.baseEnergyGain = 0.02f;
        titan.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_titan.png");;
        titan.orbitalDistanceToParent = 0.01f;
        titan.orbitalBaseOffsetDegrees = 180;
        titan.earthRadiusMultiplier = 0.5f;
        titan.gravitationalMultiplier = 0.3f;
        titan.atmosphereComposition.put(GasRegistry.methane, new PlanetDimensionProperties.GasProperty(0,0.5f,0,0));
        titan.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(1,0,0f,0));
        titan.canVisit = true;
        titan.biomePreset = MOON.name;
        galaxy.add(titan);

/*
        PlanetDimensionProperties kalos = new PlanetDimensionProperties();
        kalos.name = "Kalos";
        kalos.dimensionId = ResourceLocation.fromNamespaceAndPath(Main.MODID, "kalos");
        kalos.parentDimensionId = sun.dimensionId;
        kalos.dayTimeReference = sun.dimensionId;
        kalos.currentTemp = 300;
        kalos.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_alpine-1.png");;
        kalos.orbitalDistanceToParent = 2f;
        kalos.orbitalBaseOffsetDegrees = 0;
        kalos.earthRadiusMultiplier = 0.8f;
        kalos.gravitationalMultiplier = 0.7f;
        kalos.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.5,0,0f,0));
        kalos.atmosphereComposition.put(GasRegistry.oxygen, new PlanetDimensionProperties.GasProperty(0.3,0,0f,0));
        kalos.atmosphereComposition.put(GasRegistry.methane, new PlanetDimensionProperties.GasProperty(0.001,0,0f,0));
        kalos.atmosphereComposition.put(GasRegistry.co2, new PlanetDimensionProperties.GasProperty(0.003,0,0f,0));
        kalos.atmosphereComposition.put(GasRegistry.water, new PlanetDimensionProperties.GasProperty(0,0.48,0f,0));
        kalos.skyColor = SKY_COLOR_OVERWORLD();
        kalos.canVisit = true;
        kalos.biomePreset = OVERWORLD.name;
        galaxy.add(kalos);
 */



        PlanetDimensionProperties priate = new PlanetDimensionProperties();
        priate.name = "Priate";
        priate.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "priate");
        priate.gravitationalMultiplier = 150;
        priate.earthRadiusMultiplier = 300;
        priate.rotationAxis = new Vec3(0, 1, 0).normalize();
        priate.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_8k_sun_grayscale.png");
        priate.emissiveLightColor = new Vector3f(1f, 0.9f, 0.8f);
        priate.radiationIntensity = 5f;
        priate.emissiveTextureTintColor = new Vector3f(1,1,1).mul(20f);
        priate.position = new Vec3(500000, 1000, -90000);
        galaxy.add(priate);


        PlanetDimensionProperties jestefad = new PlanetDimensionProperties();
        jestefad.name = "Jestefad";
        jestefad.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "jestefad");
        jestefad.parentDimensionId = priate.dimensionId;
        jestefad.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_gaseous3.png");
        jestefad.orbitalDistanceToParent = 2f;
        jestefad.earthRadiusMultiplier = 10f;
        jestefad.gravitationalMultiplier = 10f;
        jestefad.canGasMine = true;
        jestefad.skyColor = new Vector3f(0.5f,0.5f,0.5f);
        jestefad.atmosphereComposition.put(GasRegistry.methane, new PlanetDimensionProperties.GasProperty(2,0, 0, 0));
        jestefad.atmosphereComposition.put(GasRegistry.hydrogen, new PlanetDimensionProperties.GasProperty(5,0, 0, 0));
        galaxy.add(jestefad);

        PlanetDimensionProperties mustafar = new PlanetDimensionProperties();
        mustafar.name = "Mustafar";
        mustafar.description = "The gravimetric duel between the gas giants Jestefad and Lefrani over Mustafar heated the planet's core, transforming the world into an imbalanced volcanic hellscape.";
        mustafar.dimensionId = ResourceLocation.fromNamespaceAndPath(Main.MODID, "mustafar");
        mustafar.parentDimensionId = jestefad.dimensionId;
        mustafar.dayTimeReference = priate.dimensionId;
        mustafar.currentTemp = 300;
        mustafar.baseEnergyGain = 0.11f;
        mustafar.orbitalDistanceToParent = 0.02f;
        mustafar.orbitalBaseOffsetDegrees = 0;
        mustafar.earthRadiusMultiplier = 0.9f;
        mustafar.gravitationalMultiplier = 0.9f;
        mustafar.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.5,0,0f,0));
        mustafar.atmosphereComposition.put(GasRegistry.co2, new PlanetDimensionProperties.GasProperty(0.02,0,0f,0));
        mustafar.atmosphereComposition.put(GasRegistry.oxygen, new PlanetDimensionProperties.GasProperty(0.2,0,0f,0));
        mustafar.canVisit = true;
        mustafar.biomePreset = MUSTAFAR.name;
        mustafar.customSeaFluid = ResourceLocation.parse("minecraft:lava");
        mustafar.customSeaFluidLevel = 52;
        mustafar.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_volcanic-1.png");;
        mustafar.cloudValueOverwrite = 0.7f;
        mustafar.skyDarken = 0.7f;
        mustafar.skyColor = SKY_COLOR_OVERWORLD(); // maybe a bit darker? but thats what extinction can do
        mustafar.fogColor = new Vector3f(0.7f, 0.33f, 0.25f);
        mustafar.cloudColor = new Vector3f(0.25f, 0.22f, 0.20f);
        mustafar.emissiveTextureTintColor = new Vector3f(1,1,1).mul(0.2f); // make lava glow
        galaxy.add(mustafar);

        PlanetDimensionProperties lefrani = new PlanetDimensionProperties();
        lefrani.name = "Lefrani";
        lefrani.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "lefrani");
        lefrani.parentDimensionId = priate.dimensionId;
        lefrani.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_2k_neptune.png");
        lefrani.orbitalDistanceToParent = 3f;
        lefrani.earthRadiusMultiplier = 20f;
        lefrani.gravitationalMultiplier = 20f;
        lefrani.canGasMine = true;
        lefrani.skyColor = new Vector3f(0.2f,0.3f,0.5f);
        lefrani.atmosphereComposition.put(GasRegistry.oxygen, new PlanetDimensionProperties.GasProperty(10,0, 0, 0));
        lefrani.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(5,0, 0, 0));
        galaxy.add(lefrani);






        PlanetDimensionProperties tatoo1 = new PlanetDimensionProperties();
        tatoo1.name = "Tatoo I";
        tatoo1.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "tatoo1");
        tatoo1.gravitationalMultiplier = 100;
        tatoo1.earthRadiusMultiplier = 100;
        tatoo1.rotationAxis = new Vec3(0, 1, 0).normalize();
        tatoo1.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_8k_sun_grayscale.png");
        tatoo1.emissiveLightColor = new Vector3f(1f, 0.9f, 0.8f);
        tatoo1.radiationIntensity = 2f;
        tatoo1.emissiveTextureTintColor = new Vector3f(1,1,1).mul(20f);
        tatoo1.position = new Vec3(-600000, -10000, -10000);
        galaxy.add(tatoo1);

        PlanetDimensionProperties tatoo2 = new PlanetDimensionProperties();
        tatoo2.name = "Tatoo II";
        tatoo2.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "tatoo2");
        tatoo2.gravitationalMultiplier = 80;
        tatoo2.earthRadiusMultiplier = 80;
        tatoo2.rotationAxis = new Vec3(0, 1, 0).normalize();
        tatoo2.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_8k_sun_grayscale.png");
        tatoo2.emissiveLightColor = new Vector3f(1f, 0.8f, 0.2f);
        tatoo2.radiationIntensity = 0.4f;
        tatoo2.emissiveTextureTintColor = new Vector3f(1.0f,0.3f,0.1f).mul(15f);
        tatoo2.parentDimensionId = tatoo1.dimensionId;
        tatoo2.orbitalDistanceToParent = 0.3f;
        tatoo2.orbitAxis = new Vec3(1,0.5,-1).normalize();
        galaxy.add(tatoo2);


        PlanetDimensionProperties tatooine = new PlanetDimensionProperties();
        tatooine.name = "Tatooine";
        tatooine.description = "";
        tatooine.dimensionId = ResourceLocation.fromNamespaceAndPath(Main.MODID, "tatooine");
        tatooine.parentDimensionId = tatoo1.dimensionId;
        tatooine.dayTimeReference = tatoo1.dimensionId;
        tatooine.currentTemp = 300;
        tatooine.orbitalDistanceToParent = 1.3f;
        tatooine.orbitalBaseOffsetDegrees = 90;
        tatooine.earthRadiusMultiplier = 0.95f;
        tatooine.gravitationalMultiplier = 0.9f;
        tatooine.atmosphereComposition.put(GasRegistry.nitrogen, new PlanetDimensionProperties.GasProperty(0.8,0,0,0));
        tatooine.atmosphereComposition.put(GasRegistry.co2, new PlanetDimensionProperties.GasProperty(0.003,0,0,0));
        tatooine.atmosphereComposition.put(GasRegistry.oxygen, new PlanetDimensionProperties.GasProperty(0.2,0,0,0));
        tatooine.atmosphereComposition.put(GasRegistry.water, new PlanetDimensionProperties.GasProperty(0,0.05,0,0));
        tatooine.canVisit = true;
        tatooine.biomePreset = DESERT_WASTELAND.name;
        tatooine.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/baked_4k_makemake_fictional.png");;
        tatooine.skyColor = SKY_COLOR_OVERWORLD();
        galaxy.add(tatooine);


        List<String> dimensionProperties = new ArrayList<>();
        for (DimensionProperties i : galaxy) {
            dimensionProperties.add(new GsonBuilder().setPrettyPrinting().create().toJson(i));
        }
        return dimensionProperties;
    }
}
