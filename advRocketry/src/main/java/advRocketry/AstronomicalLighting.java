package advRocketry;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A utility class for calculating lighting based on celestial body positions.
 * This class contains the core astronomical math, decoupled from game-specific logic.
 */
public class AstronomicalLighting {

    /**
     * Calculates the raw sky brightness based on astronomical parameters.
     * This is a robust implementation based on spherical trigonometry.
     *
     * @param lightSourceToPlanet     A vector pointing from the light source (star) to the planet's center.
     * @param rotationAxis            The planet's axis of rotation (must be a normalized vector).
     * @param timeOfDayAngleDegrees   The planet's self-rotation in degrees. Convention:
     * 0 = sunrise, 90 = noon, 180 = sunset, 270 = midnight.
     * @param observerLatitudeDegrees The latitude of the observer on the planet's surface in degrees.
     * @return A raw brightness value, from 0.0 (darkness) to 1.0 (star directly overhead).
     */
    public static float calculateAstronomicalBrightness(
            Vec3 lightSourceToPlanet, Vec3 rotationAxis, double timeOfDayAngleDegrees, double observerLatitudeDegrees) {

        // Inputs
        double mcAngleDeg = timeOfDayAngleDegrees;  // 0=sunrise, 90=noon, etc.
        double mcAngleRad = Math.toRadians(mcAngleDeg);

// Latitude and declination
        double latRad = Math.toRadians(observerLatitudeDegrees);
        Vec3 lightDirection = lightSourceToPlanet.normalize();
        double declinationRad = Math.asin(lightDirection.dot(rotationAxis));

// Sunrise/sunset hour angle
        double cosH0 = -Math.tan(latRad) * Math.tan(declinationRad);

// Clamp to [-1,1] for polar cases
        cosH0 = Mth.clamp(cosH0, -1.0, 1.0);
        double H0 = Math.acos(cosH0);  // radians

// Map Minecraft angle → true hour angle
        double hourAngleRad;
        if (mcAngleDeg <= 180.0) {
            // Morning → Evening maps [0..180] → [-H0..+H0]
            hourAngleRad = -H0 + (mcAngleRad / Math.PI) * (2*H0);
        } else {
            // Night: map [180..360] → [+H0..(π+H0)] and wrap around
            hourAngleRad = +H0 + ((mcAngleRad - Math.PI) / Math.PI) * (Math.PI - 2*H0);
            if (hourAngleRad > Math.PI) {
                hourAngleRad -= 2*Math.PI; // keep in [-π..+π]
            }
        }

// Altitude
        double sinAlt = Math.sin(latRad) * Math.sin(declinationRad) +
                Math.cos(latRad) * Math.cos(declinationRad) * Math.cos(hourAngleRad);
        double altitudeRad = Math.asin(sinAlt);

// Brightness
        final double twilightAlt = Math.toRadians(-6.0);
        double brightness = (altitudeRad - twilightAlt) / ((Math.PI/2.0) - twilightAlt);


        System.out.printf("lat=%.1f°, dec=%.1f°, alt=%.1f°%n",
                observerLatitudeDegrees,
                Math.toDegrees(declinationRad),
                Math.toDegrees(altitudeRad));
System.out.println(brightness);
        return Mth.clamp((float) brightness, 0.0F, 1.0F);

    }
}