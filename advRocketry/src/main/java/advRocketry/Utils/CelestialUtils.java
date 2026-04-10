package advRocketry.Utils;

import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

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


    public static double getGravityMultiplier(Object entity){
        Entity p_entity = (Entity) entity;
        ResourceLocation dimensionId = p_entity.level().dimension().location();
        Dimension dimension = DimensionManager.getDimensionManager(p_entity.level().isClientSide).get(dimensionId);
        double massMultiplier = 1.0;
        if (dimension != null) {
            massMultiplier = dimension.getGravitationalMultiplier();
        }
        return massMultiplier;
    }

    // --- Other physics calculations ---
    public static final double G = 0.0000005;
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

    // mass in kg, distance in m
    public static double calculateOrbitalPeriodTicks(double mass1, double mass2, double distance) {
        double combinedMass = mass1 + mass2;
        return 2 * Math.PI * Math.sqrt(Math.pow(distance, 3) / (G * combinedMass)) * 20;
    }
    // mass in kg, distance in m, returns m/s
    public static double getSpeedForOrbit(double mass1, double distance){
        return Math.sqrt(G * mass1 / distance);
    }


    public static double getPlanetRenderRadiusAU(PlanetDimension planet) {
        return CelestialUtils.toAU(CelestialUtils.fromEarthRadius(planet.getEarthRadiusMultiplier())) * Config.INSTANCE.planet_Render_Scale_Multiplier;
    }
}

