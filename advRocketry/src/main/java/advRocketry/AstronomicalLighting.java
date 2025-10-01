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
        // 1. Hour Angle: shift so 0° = noon
        final double hourAngleRad = Math.toRadians(timeOfDayAngleDegrees - 90.0);

// 2. Solar declination (same as before)
        Vec3 lightDirection = lightSourceToPlanet.normalize();
        double sinOfDeclination = lightDirection.dot(rotationAxis);
        double declinationRad = Math.asin(sinOfDeclination);

// 3. Sun altitude
        double sinOfAltitude = Math.sin(latRad) * Math.sin(declinationRad) +
                Math.cos(latRad) * Math.cos(declinationRad) * Math.cos(hourAngleRad);
        double altitudeRad = Math.asin(sinOfAltitude);

// 4. Map altitude to brightness with twilight
        final double twilightAltitudeRad = Math.toRadians(-6.0); // more realistic civil twilight
        double brightness = (altitudeRad - twilightAltitudeRad) / ((Math.PI / 2.0) - twilightAltitudeRad);

// 5. Clamp
        System.out.println(brightness+":"+observerLatitudeDegrees);
        return Mth.clamp((float) brightness, 0.0F, 1.0F);

    }
}