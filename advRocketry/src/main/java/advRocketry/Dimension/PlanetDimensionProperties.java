package advRocketry.Dimension;

import advRocketry.Config;
import advRocketry.Registry.GasRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;


public class PlanetDimensionProperties extends DimensionProperties {

    public String description = ""; // a custom text to show on the space map

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
    public float radiationIntensity = 0; // radiation strength, used for terrain shading, and temperature calculation, and shading other planets
    public int latitude_len = 400000;// how much you have to move in z direction to "go around the planet" 0% = equator, 25% = South Pole, 50% = equator again, 75% = North Pole
    public int targetDayLength = 24000; // set negative or 0 for fixed time
    public HashMap<String, GasProperty> atmosphereComposition = new HashMap<>();
    public float baseEnergyGain = 0; // the base energy gain, maybe by hot core or gravity force pulling on the planet
    public boolean canVisit = false;
    public boolean isKnown = false; // if false it has to be discovered and unlocked in observatory
    public int dataRequiredForUnlock = 2000; // how much data of any type is required to unlock it on the planet
    public ResourceLocation artifactItem = null; // TODO: artifact allows for discovery in observatory
    public HashMap<String, Double> laserOres = DEFAULT_ORES(); // what a laser drill can mine here and the probability of it

    // world gen related configs
    // when selecting a modded dimension, all this is ignored
    public ResourceLocation customSeaFluid = null;
    public int customSeaFluidLevel = 0;
    public boolean generateStructures = false;
    public String biomePreset = null;

    // TODO (maybe)
    // when terraforming picks a new preset, it iterates every climate point and when original preset has a frozen biome at this point,
    // it should be injected into the new preset so you can pin biomes to never terraform.
    // For example maybe you want to keep hot springs biome from original preset no matter what generates next.
    // public ArrayList<ResourceLocation> frozenBiomes = new ArrayList<>();

    // mostly render related configs
    public boolean hasCustomSky = true;
    public ResourceLocation texture = null;
    public Vector3f skyColor = new Vector3f(1, 1, 1); // also used in atm shading when looking from a distant planet to this one
    public float cloudValueOverwrite = -1; // can overwrite cloud value, 0 to 1 or negative to disable
    public float skyDarken = 0; // how much the sky / stars darken (think of volcanic ash in the air making it dark)
    public Vector3f cloudColor = new Vector3f(1, 1, 1); // TODO: calculate based on atm when null
    public Vector3f fogColor = new Vector3f(0.8f, 0.9f, 1.1f); // fog color
    public Vector3f sunRiseColor = new Vector3f(3f, 1.6f, 0.2f); // the atm shading on sunrise
    public Vector3f reflectiveTextureTintColor = new Vector3f(1f, 1f, 1f); // reflective texture is multiplied by th
    public Vector3f emissiveTextureTintColor = new Vector3f(0, 0, 0); // the color that the planets emissive texture is tinted with
    public Vector3f emissiveLightColor = new Vector3f(0, 0, 0); // the color that the planet radiates with to shade other planets
    public boolean hasRingSystem = false;

    public float dayTime; // do not set yourself
    public double currentTemp = 300; // do set yourself to have a starting value

    public PlanetDimensionProperties() {
        this.type = DimensionType.PLANET;
    }

    public static Vector3f SKY_COLOR_OVERWORLD() {
        return new Vector3f(0.45f, 0.7f, 1f);
    }

    public static HashMap<String, Double> DEFAULT_ORES() {
        HashMap<String, Double> map = new HashMap<>();
        map.put("minecraft:gold_ore", 0.005);
        map.put("minecraft:iron_ore", 0.02);
        map.put("minecraft:coal_ore", 0.05);
        map.put("minecraft:lapis_ore", 0.002);
        map.put("minecraft:diamond_ore", 0.01);
        map.put("minecraft:redstone_ore", 0.003);
        map.put("minecraft:emerald_ore", 0.01);
        map.put("minecraft:nether_quartz_ore", 0.001);
        return map;
    }

    public static class GasProperty {
        public static double maxSeaLevel = 120;
        public double in_atm;
        public double frozen_surface;
        public double underground;
        public double liquid;

        public int worldGenSeaLevel;

        public GasProperty(double in_atm, double liquid, double frozen_surface, double underground) {
            this.in_atm = in_atm;
            this.liquid = liquid;
            this.frozen_surface = frozen_surface;
            this.underground = underground;
        }

        public double getSeaLevel() {
            double surfaceValue = liquid + frozen_surface;
            if (surfaceValue == 0)
                return -100;


            // 62 around 0.5, grows slower when high water composition and drops quickly on low comosition
            double seaLevel = Math.sqrt(surfaceValue) * 87.7;
            seaLevel = Math.min(maxSeaLevel, seaLevel);
            return seaLevel;
        }

        public void maybeAdjustWorldgenSeaLevel() {
            double seaLevel = getSeaLevel();
            if (Math.abs(worldGenSeaLevel - seaLevel) > 0.6) {
                worldGenSeaLevel = (int) Math.round(seaLevel);
            }
        }

        private double getTransferSpeed(PlanetDimension planetDimension) {
            return (Config.INSTANCE.gas_Atm_Transition_Speed / (1 + planetDimension.getGravitationalMultiplier()));
        }

        public void tick(GasRegistry.Gas gas, PlanetDimension planet, double temp, double atmDensity) {
            double totalMass = in_atm + liquid + frozen_surface;
            if (totalMass <= 0) return;

            double freezeTemp = gas.getFreezeTemp(atmDensity);
            double boilTemp = gas.getBoilingTemp(atmDensity);

            // Dynamically scale the transition zone to planetary temperature.
            // x=90: +-2.9
            // x=200: +-9.5
            // x=300: +-15.86
            double x = (temp - 200.0) / 40.0;
            double scale = 1.0 / (1.0 + Math.exp(-x));
            double ZONE = 2.0 + 15.0 * scale;

            // 1. CALCULATE RAW TARGET FRACTIONS
            double rawTargetSolidFrac = Math.clamp((freezeTemp + ZONE - temp) / (2.0 * ZONE), 0.0, 1.0);
            double rawTargetGasFrac = Math.clamp((temp - (boilTemp - ZONE)) / (2.0 * ZONE), 0.0, 1.0);

            // 2. DISCRETIZE INTO 100 STEPS
            double targetSolidFrac = Math.round(rawTargetSolidFrac * 100.0) / 100.0;
            double targetGasFrac = Math.round(rawTargetGasFrac * 100.0) / 100.0;
            double targetLiquidFrac = Math.max(0.0, 1.0 - targetSolidFrac - targetGasFrac);

            // 3. CONVERT TO TARGET MASSES
            double targetSolid = totalMass * targetSolidFrac;
            double targetLiquid = totalMass * targetLiquidFrac;
            double targetGas = totalMass * targetGasFrac;

            // 4. STEP CURRENT VALUES
            double speed = getTransferSpeed(planet);

            double solidDelta = stepValue(frozen_surface, targetSolid, speed);
            double liquidDelta = stepValue(liquid, targetLiquid, speed);
            double gasDelta = stepValue(in_atm, targetGas, speed);

            if (solidDelta != 0 || liquidDelta != 0 || gasDelta != 0) {
                // Apply deltas and snap directly to target if within precision threshold
                frozen_surface = applyAndSnap(frozen_surface, targetSolid, solidDelta);
                liquid = applyAndSnap(liquid, targetLiquid, liquidDelta);
                in_atm = applyAndSnap(in_atm, targetGas, gasDelta);

                maybeAdjustWorldgenSeaLevel();
                planet.setRequiresSync();
            }
        }

        // Pure clamp for step speed
        private double stepValue(double current, double target, double maxStep) {
            return Math.clamp(target - current, -maxStep, maxStep);
        }

        // Applies delta and forces exact equality when close enough
        private double applyAndSnap(double current, double target, double delta) {
            double newValue = current + delta;
            if (Math.abs(target - newValue) < 1e-6) {
                return target; // Direct assignment forces (target - current = 0.0) on next tick
            }
            return newValue;
        }
    }
}
