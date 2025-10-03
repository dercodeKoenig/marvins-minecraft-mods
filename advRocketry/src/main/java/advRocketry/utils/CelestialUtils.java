package advRocketry.utils;

import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.DimensionProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A utility class for celestial mechanics calculations
 */
public class CelestialUtils {

    /**
     * Rotates a vector 'v' around an 'axis' by 'angleDeg' degrees
     * using Rodrigues' rotation formula.
     */
    public static Vec3 rotate(Vec3 v, Vec3 axis, double angleDeg) {
        double angleRad = Math.toRadians(angleDeg);
        Vec3 k = axis.normalize();
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);

        Vec3 term1 = v.scale(cosA);
        Vec3 term2 = k.cross(v).scale(sinA);
        Vec3 term3 = k.scale(k.dot(v) * (1 - cosA));

        return term1.add(term2).add(term3);
    }


    // --- Other physics calculations ---
    public static final double G = 0.000001;
    public static final double ASTRONOMICAL_UNIT = 1.496 * Math.pow(10, 11);
    public static final double EARTH_MASS = 5.972 * Math.pow(10, 24);
    public static final double EARTH_RADIUS = 6_000_000;

    public static double fromEarthMasses(double earthMassMultiplier){
        return EARTH_MASS * earthMassMultiplier;
    }
    public static double fromAU(double AUMultiplier){
        return AUMultiplier * ASTRONOMICAL_UNIT;
    }
    public static double toAU(double distance){
        return distance / ASTRONOMICAL_UNIT;
    }
    public static double fromEarthRadius(double earthRadiusMultiplier){
        return earthRadiusMultiplier * EARTH_RADIUS;
    }

    public static double calculateOrbitalPeriodTicks(double mass1, double mass2, double distance) {
        double combinedMass = mass1 + mass2;
        return 2 * Math.PI * Math.sqrt(Math.pow(distance, 3) / (G * combinedMass)) * 20;
    }
}

