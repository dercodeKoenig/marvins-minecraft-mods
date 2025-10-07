package advRocketry.Dimension;

import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;

public class DefaultGalaxy {

    public static String createDefaultGalaxy() {

        ArrayList<DimensionProperties> galaxy = new ArrayList<>();

        DimensionProperties sun = new DimensionProperties();
        sun.name = "Sun";
        sun.type = DimensionProperties.PlanetType.STAR;
        sun.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "sun");
        sun.earthMassMultiplier = 200;
        sun.earthRadiusMultiplier = 100;
        sun.rotationAxis = new Vec3(0, 1, 0).normalize();
        sun.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/sun_grayscale_ico_1k.png");
        sun.emissiveColor = new Vector4f(0.9f, 0.9f, 0.7f, 1f);
        galaxy.add(sun);

        DimensionProperties overworld = new DimensionProperties();
        overworld.name = "Earth";
        overworld.dimensionId = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        overworld.parentDimensionId = sun.dimensionId;
        overworld.dayTimeReference = sun.dimensionId;
        overworld.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/earth_ico_1k.png");
        galaxy.add(overworld);

        DimensionProperties moon = new DimensionProperties();
        moon.name = "moon";
        moon.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon");
        moon.parentDimensionId = overworld.dimensionId;
        moon.dayTimeReference = sun.dimensionId;
        moon.orbitalDistanceToParent = 0.00257;
        moon.orbitAxis = new Vec3(0.1, 1, 0);
        moon.earthRadiusMultiplier = 0.272;
        moon.earthMassMultiplier = 0.3;
        moon.targetDayLength = 12000;
        moon.atmosphereDensity = 0;
        moon.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_512.png");
        galaxy.add(moon);



        DimensionProperties venus = new DimensionProperties();
        venus.name = "Venus";
        venus.dimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "venus");
        venus.parentDimensionId = sun.dimensionId;
        venus.dayTimeReference = sun.dimensionId;
        venus.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/earth_ico_1k.png");
        venus.orbitalDistanceToParent = 0.5f;
        venus.atmosphereDensity = 2;
        galaxy.add(venus);

        return new GsonBuilder().setPrettyPrinting().create().toJson(galaxy);
    }
}
