package advRocketry;

import net.minecraft.world.phys.Vec3;

public class AstronomicalLighting {

    /**
     * Calculate sky darken value based on astronomical position
     *
     * @param starToPlanet Vector from star to planet (unnormalized)
     * @param rotationAxis Planet's rotation axis (normalized)
     * @param rotationAngle Planet's rotation angle in radians (0 to 2π)
     * @param observerLatitude Observer's latitude in radians (-π/2 to π/2)
     * @return skyDarken value (0 = bright day, 11 = full night)
     */
    public static int calculateSkyDarken(
            Vec3 starToPlanet,
            Vec3 rotationAxis,
            double rotationAngle,
            double observerLatitude) {

        // 1. Get sun direction (planet to star)
        Vec3 sunDirection = starToPlanet.normalize().reverse();

        // 2. Calculate observer's position on planet surface
        Vec3 observerPosition = calculateObserverPosition(
                rotationAxis, rotationAngle, observerLatitude
        );

        // 3. Calculate solar altitude angle
        double solarAltitude = calculateSolarAltitude(sunDirection, observerPosition);

        // 4. Convert to sky darken value
        return solarAltitudeToSkyDarken(solarAltitude);
    }

    /**
     * Calculate observer's position vector on planet surface
     */
    private static Vec3 calculateObserverPosition(
            Vec3 rotationAxis,
            double rotationAngle,
            double observerLatitude) {

        // Start with north pole direction (along rotation axis)
        Vec3 northPole = rotationAxis.normalize();

        // Get perpendicular vector for equator (any perpendicular will do)
        Vec3 equatorDirection = getPerpendicularVector(northPole);

        // Rotate equator direction by rotation angle around axis
        Vec3 localEast = rotateAroundAxis(equatorDirection, northPole, rotationAngle);

        // Calculate observer position using latitude
        double cosLat = Math.cos(observerLatitude);
        double sinLat = Math.sin(observerLatitude);

        // Observer is at: cosLat * (rotated equator position) + sinLat * (pole direction)
        return localEast.scale(cosLat).add(northPole.scale(sinLat)).normalize();
    }

    /**
     * Calculate solar altitude angle
     * Returns angle in radians: π/2 = directly overhead, 0 = horizon, -π/2 = nadir
     */
    private static double calculateSolarAltitude(Vec3 sunDirection, Vec3 observerUp) {
        // Dot product gives us cos(angle from zenith)
        double cosZenithAngle = sunDirection.dot(observerUp);

        // Solar altitude = π/2 - zenith angle
        return Math.asin(cosZenithAngle);
    }

    /**
     * Convert solar altitude to Minecraft sky darken value
     */
    private static int solarAltitudeToSkyDarken(double solarAltitude) {
        // Solar altitude in degrees for easier understanding
        double altitudeDegrees = Math.toDegrees(solarAltitude);

        // Minecraft uses 0 (brightest) to 11 (darkest)
        // Twilight occurs from about -18° to 0° (civil, nautical, astronomical twilight)
        // Full day is above 0°

        double brightness;
        if (altitudeDegrees > 0) {
            // Daytime: full brightness when sun is above horizon
            brightness = 1.0;
        } else if (altitudeDegrees > -18) {
            // Twilight: linear interpolation from 0° to -18°
            brightness = 1.0 - (Math.abs(altitudeDegrees) / 18.0);
        } else {
            // Night: completely dark below -18°
            brightness = 0.0;
        }

        // Convert to skyDarken (inverted: 0 = bright, 11 = dark)
        return (int)Math.round((1.0 - brightness) * 11.0);
    }

    /**
     * Get any vector perpendicular to the given vector
     */
    private static Vec3 getPerpendicularVector(Vec3 v) {
        // Choose axis least aligned with v
        Vec3 axis = Math.abs(v.x) < 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        return v.cross(axis).normalize();
    }

    /**
     * Rotate vector around axis by angle (Rodrigues' rotation formula)
     */
    private static Vec3 rotateAroundAxis(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        Vec3 vParallel = axis.scale(axis.dot(v));
        Vec3 vPerp = v.subtract(vParallel);
        Vec3 w = axis.cross(v);

        return vParallel.add(vPerp.scale(cos)).add(w.scale(sin));
    }
}