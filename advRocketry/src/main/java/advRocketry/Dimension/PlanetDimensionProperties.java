package advRocketry.Dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class PlanetDimensionProperties extends DimensionProperties {
    // TODO: add always rain / always thunder values or custom rain times
    // gas mining should mine relative to the planet mass
    // when decreasing or increasing pressure, calculate the new gas value as +- 1 / planet mass
    // so moon would require less gas to reach a value of 1 while jupyter provides lots of gas to mine
    // gas can be mined if config flag enables it until the relative value of a gas drops < 1

    public static Vector3f SKY_COLOR_OVERWORLD() {
        return new Vector3f(0.45f, 0.7f, 1f);
    }
    public static int SEA_LEVEL_OVERWORLD() {
        return 63;
    }


    // planet related configs
    public Vec3 position = new Vec3(0, 0, 0);
    public float gravitationalMultiplier = 1;
    public float earthRadiusMultiplier = 1;
    public Vec3 rotationAxis = new Vec3(0.2, 1, 0);
    public ResourceLocation parentDimensionId = null;       // optional, overwrites position
    public Vec3 orbitAxis = new Vec3(0, 1, 0);
    public float orbitalDistanceToParent = 1;
    public float orbitalBaseOffsetDegrees = 0;
    public ResourceLocation dayTimeReference = null;  // required reference for day start
    public float radiationIntensity = 0; // radiation strength, used for terrain shading, and temperature calculation and to scale emissive light in planet render

    public int latitude_len = 400000;// how much you have to move in z direction to "go around the planet" 0% = equator, 25% = South Pole, 50% = equator again, 75% = North Pole
    public int targetDayLength = 24000; // set negative or 0 for fixed time

    public boolean canVisit = false;
    public boolean canGasMine = false;
    public boolean isKnown = false; // if false it has to be discovered and unlocked in observatory
    public int dataRequiredForUnlock = 2000; // how much data of any type is required to unlock it on the planet
    public ResourceLocation artifactItem = null; // TODO: artifact allows for discovery in observatory

    // world gen related configs
    public int seaLevel = 0;
    public boolean generateStructures = false;
    public String biomePreset = null;
    public boolean generateVolcanos = false;


    // mostly render related configs
    public boolean hasCustomSky = true;
    public ResourceLocation texture = null;
    public Vector3f skyColor = new Vector3f(1, 1, 1); // also used in atm shading when looking from a distant planet to this one
    public Vector3f cloudColor = new Vector3f(1, 1, 1); // TODO: calculate based on atm when null
    public Vector3f fogColor = new Vector3f(0.89f, 0.95f, 1.0f); // base fog color to calculate actual color
    public Vector3f sunRiseColor = new Vector3f(3f, 2f, 0.2f); // the atm shading on sunrise
    public Vector3f reflectiveTextureTintColor = new Vector3f(1f, 1f, 1f); // maybe reflect only green light? or red?
    public Vector3f emissiveColor = new Vector3f(0, 0, 0); // the color that the planet radiates with for render
    public boolean hasRingSystem = false;

    public HashMap<String, GasProperty> atmosphereComposition = new HashMap<>();

    public float dayTime; // do not set yourself
    public double currentTemp = 300; // do set yourself to have a starting value

    public PlanetDimensionProperties() {
        this.type = DimensionType.PLANET;
    }

    public static class GasProperty {
        public float in_atm;
        public float frozen_surface;
        public float frozen_deep_below_surface;

        public GasProperty(float in_atm, float frozen_surface, float frozen_deep_below_surface) {
            this.in_atm = in_atm;
            this.frozen_surface = frozen_surface;
            this.frozen_deep_below_surface = frozen_deep_below_surface;
        }
    }
}
