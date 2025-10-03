package advRocketry;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A utility class for celestial mechanics calculations, specifically for converting
 * world-space coordinates into an observer's local sky frame for rendering.
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

    public static class AxisDirections {
        public AxisDirections(Vec3 north, Vec3 east, Vec3 up){
            this.north = north;
            this.up = up;
            this.east = east;
        }
        Vec3 north;
        Vec3 east;
        Vec3 up;
    }
    public static AxisDirections getGlobalAxisDirections(DimensionProperties myPlanet, float partialTick){
        // 1. Pick the correct perpendicular vector to axis
        Vec3 equatorRef = myPlanet.getEquatorReference(partialTick);

        Vec3 rotationAxis = myPlanet.rotationAxis;

        // 2. Rotate the equatorRef by raw self-rotation
        Vec3 rotatedEquator = CelestialUtils.rotate(equatorRef, rotationAxis, myPlanet.getRotationAngle(partialTick));

        // 3. Get east vector
        Vec3 east = rotationAxis.cross(rotatedEquator).normalize();

        // 4 rotate equator reference around east by latitude
        double lat = myPlanet.getLatitude();
        Vec3 localUp = CelestialUtils.rotate(rotatedEquator, east, lat).normalize();

        // 5 calculate new north
        Vec3 north = localUp.cross(east).normalize();

        return new AxisDirections(north, east, localUp);
    }

    public static float getSunAltitudeDegrees(DimensionProperties myPlanet, DimensionProperties lightSource, float partialTick) {
        double altitude = Math.asin(getSurfaceDotToPlanet(myPlanet, lightSource, partialTick));
        return (float) Math.toDegrees(altitude);
    }

    public static double getSurfaceDotToPlanet(DimensionProperties myPlanet, DimensionProperties targetPlanet, float partialTick){
        Vec3 localUp = getGlobalAxisDirections(myPlanet, partialTick).up;
        // 5 Dot with star direction to get altitude
        Vec3 targetDirection = targetPlanet.getPosition(partialTick).subtract(myPlanet.getPosition(partialTick)).normalize();
        double dot = localUp.dot(targetDirection);
        return dot;
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

