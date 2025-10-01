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

        // --- 1. Convert Inputs to Radians ---
        final double latRad = Math.toRadians(observerLatitudeDegrees);
        // The time of day angle is also known as the Hour Angle in astronomy.
        // We convert it to the standard astronomical convention where 0 is noon.
        final double hourAngleRad = Math.toRadians(timeOfDayAngleDegrees - 90.0);

        // --- 2. Calculate Solar Declination ---
        // This is the angle of the sun relative to the planet's equatorial plane.
        // It's the astronomical equivalent of "latitude" for the sun.
        // We find it using the dot product between the light direction and the rotation axis.
        Vec3 lightDirection = lightSourceToPlanet.normalize();
        double sinOfDeclination = lightDirection.dot(rotationAxis);
        double declinationRad = Math.asin(sinOfDeclination);

        // --- 3. Calculate Sun's Altitude ---
        // This is the core formula to find the sun's angle above the horizon for the observer.
        // sin(altitude) = sin(latitude) * sin(declination) + cos(latitude) * cos(declination) * cos(hour_angle)
        double sinOfAltitude = Math.sin(latRad) * sinOfDeclination +
                Math.cos(latRad) * Math.cos(declinationRad) * Math.cos(hourAngleRad);

        // The altitude is the angle of the sun above the horizon, from -90 to +90 degrees.
        double altitudeRad = Math.asin(sinOfAltitude);

        // --- 4. Map Altitude to Brightness ---
        // We get a value from 0.0 (horizon) to 1.0 (directly overhead at 90 degrees).
        // We also add a twilight effect by allowing the brightness to start when the sun
        // is slightly below the horizon.
        final double twilightAltitudeRad = Math.toRadians(-12.0); // Civil twilight starts around -6°, let's give a bit more.

        // Map the altitude range [twilight, 90°] to the brightness range [0, 1]
        double brightness = (altitudeRad - twilightAltitudeRad) / ((Math.PI / 2.0) - twilightAltitudeRad);

        System.out.println(brightness);
        // Clamp the result to ensure it's always within the valid [0, 1] range.
        return Mth.clamp((float) brightness, 0.0F, 1.0F);
    }
}