package advRocketry.Dimension;

import advRocketry.Config;
import advRocketry.Registry.GasRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;


public class PlanetDimensionProperties extends DimensionProperties {
    // TODO: add always rain / always thunder values or custom rain times
    // gas mining should mine relative to the planet mass
    // when decreasing or increasing pressure, calculate the new gas value as +- 1 / planet mass
    // so moon would require less gas to reach a value of 1 while jupyter provides lots of gas to mine
    // gas can be mined if config flag enables it until the relative value of a gas drops < 1

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
    public ResourceLocation customSeaFluid = null;
    public int customSeaFluidLevel = 0;
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

    public static Vector3f SKY_COLOR_OVERWORLD() {
        return new Vector3f(0.45f, 0.7f, 1f);
    }

    public static class GasProperty {
        public static double maxSeaLevel = 120;
        public double in_atm;
        public double frozen_surface;
        public double frozen_deep_below_surface;
        public double liquid;

        public int worldGenSeaLevel;

        public GasProperty(double in_atm, double liquid, double frozen_surface, double frozen_deep_below_surface) {
            this.in_atm = in_atm;
            this.liquid = liquid;
            this.frozen_surface = frozen_surface;
            this.frozen_deep_below_surface = frozen_deep_below_surface;
            maybeAdjustWorldgenSeaLevel();
        }

        public double getSeaLevel() {
            double surfaceValue = liquid + frozen_surface;
            if (surfaceValue == 0)
                return 0;

            double baseLevel = 40; // base value when there is any fluid
            double levelAt05 = 62; // normal value on 0.5 composition, equal to overworld sea level
            double seaLevel = baseLevel + surfaceValue * 2 * (levelAt05 - baseLevel);
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

        public boolean maybeRain(GasRegistry.Gas gas, PlanetDimension planet, double temp, double atmDensity, boolean simulate) {
            if (in_atm > 0) {
                if (temp < gas.getBoilingTemp(atmDensity) - 1 && temp > gas.getFreezeTemp(atmDensity)) {
                    if (!simulate) {
                        double toTransfer = getTransferSpeed(planet);
                        toTransfer = Math.min(in_atm, toTransfer);
                        in_atm -= toTransfer;
                        liquid += toTransfer;
                        planet.setRequiresSync();
                    }
                    return true;
                }
            }
            return false;
        }

        public boolean maybeSnow(GasRegistry.Gas gas, PlanetDimension planet, double temp, double atmDensity, boolean simulate) {
            if (in_atm > 0) {
                if (temp < gas.getBoilingTemp(atmDensity) - 1 && temp <= gas.getFreezeTemp(atmDensity)) {
                    if (!simulate) {
                        double toTransfer = getTransferSpeed(planet);
                        toTransfer = Math.min(in_atm, toTransfer);
                        in_atm -= toTransfer;
                        frozen_surface += toTransfer;
                        planet.setRequiresSync();
                    }
                    return true;
                }
            }
            return false;
        }

        public boolean maybeBoil(GasRegistry.Gas gas, PlanetDimension planet, double temp, double atmDensity, boolean simulate) {
            if (liquid > 0 || frozen_surface > 0 || frozen_deep_below_surface > 0) {
                if (temp > gas.getBoilingTemp(atmDensity) + 1) {
                    if (!simulate) {
                        double toTransfer = getTransferSpeed(planet);
                        if (liquid > 0) {
                            double toTransfer2 = Math.min(liquid, toTransfer);
                            in_atm += toTransfer2;
                            liquid -= toTransfer2;
                        }
                        if (frozen_surface > 0) {
                            double toTransfer2 = Math.min(frozen_surface, toTransfer);
                            in_atm += toTransfer2;
                            frozen_surface -= toTransfer2;
                        }
                        if (frozen_deep_below_surface > 0) {
                            double toTransfer2 = Math.min(frozen_deep_below_surface, toTransfer);
                            in_atm += toTransfer2;
                            frozen_deep_below_surface -= toTransfer2;
                        }
                        planet.setRequiresSync();
                    }
                    return true;
                }
            }
            return false;
        }

        public boolean maybeMeltSurface(GasRegistry.Gas gas, PlanetDimension planet, double temp, double atmDensity, boolean simulate) {
            if (frozen_surface > 0) {
                if (temp > gas.getFreezeTemp(atmDensity) + 1) {
                    if (!simulate) {
                        double toTransfer = getTransferSpeed(planet);
                        toTransfer = Math.min(frozen_surface, toTransfer);
                        liquid += toTransfer;
                        frozen_surface -= toTransfer;
                    }
                    return true;
                }
            }
            return false;
        }

        public boolean maybeFreezeSurface(GasRegistry.Gas gas, PlanetDimension planet, double temp, double atmDensity, boolean simulate) {
            if (liquid > 0) {
                if (temp < gas.getFreezeTemp(atmDensity) - 1) {
                    if (!simulate) {
                        double toTransfer = getTransferSpeed(planet);
                        toTransfer = Math.min(liquid, toTransfer);
                        liquid -= toTransfer;
                        frozen_surface += toTransfer;
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
