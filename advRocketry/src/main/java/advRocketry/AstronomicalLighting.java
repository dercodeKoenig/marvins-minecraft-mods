package advRocketry;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;


/**
 * A utility class for calculating lighting based on celestial body positions.
 * This class contains the core astronomical math, decoupled from game-specific logic.
 */
public class AstronomicalLighting {

    // These classes are assumed to exist in your environment (e.g., from Minecraft or a math library)
    // - Vec3: A 3D vector class with methods like normalize, negate, dot, cross, scale, and add.
    // - Mth: A math helper class with functions like clamp.

    /**
     * Calculates the raw sky brightness based on astronomical parameters.
     * The calculation determines the angle of the star relative to the observer on the planet's surface.
     *
     * @param starToPlanet     A vector pointing from the star to the planet's center.
     * @param rotationAxis     The planet's axis of rotation (must be a normalized vector).
     * @param rotationAngle    The current rotation angle of the planet in radians (represents time of day).
     * @param observerLatitude The latitude of the observer on the planet's surface in radians.
     * @return A raw brightness value, from 0.0 (complete darkness) to 1.0 (star directly overhead).
     */
    public static float calculateAstronomicalBrightness(
            Vec3 starToPlanet, Vec3 rotationAxis, double rotationAngle, double observerLatitude) {

        // 1. Get the normalized direction of light coming FROM the star.
        Vec3 lightDirection = starToPlanet.normalize().scale(-1);

        // --- PHASE CORRECTION LOGIC START ---

        // 2. Establish the planet's fixed reference frame (its "Prime Meridian" and axes).
        // This is the same logic as in your calculateObserverNormal method.
        Vec3 equatorialX = findPerpendicular(rotationAxis);
        Vec3 equatorialY = rotationAxis.cross(equatorialX);

        // 3. Project the light direction vector onto the planet's equatorial plane.
        // This isolates the component of the light that determines the time of day.
        double dot = lightDirection.dot(rotationAxis);
        Vec3 projectedLightDir = lightDirection.subtract(rotationAxis.scale(dot)).normalize();

        // 4. Calculate the angle of "High Noon".
        // We use atan2 to find the angle of the projected light relative to our fixed reference axis (equatorialX).
        // This gives us the rotation angle at which the star is directly overhead on the equator.
        double noonAngle = Math.atan2(projectedLightDir.dot(equatorialY), projectedLightDir.dot(equatorialX));

        // 5. Define the offset for sunrise. Sunrise is 90 degrees (PI/2 radians) before noon.
        final double sunriseOffset = -Math.PI / 2.0;

        // 6. Calculate the effective rotation angle by applying the phase correction.
        // This orients our time of day (rotationAngle) so that 0.0 corresponds to sunrise.
        double effectiveRotationAngle = rotationAngle + noonAngle + sunriseOffset;

        // --- PHASE CORRECTION LOGIC END ---


        // 7. Calculate the observer's surface normal vector in world space using the corrected angle.
        // This part is now moved directly here from the old calculateObserverNormal method.
        Vec3 equatorialObserverPos = equatorialX.scale(Math.cos(effectiveRotationAngle))
                .add(equatorialY.scale(Math.sin(effectiveRotationAngle)));

        Vec3 observerNormal = equatorialObserverPos.scale(Math.cos(observerLatitude))
                .add(rotationAxis.scale(Math.sin(observerLatitude)))
                .normalize();

        // 8. The dot product gives the cosine of the angle between the observer's "up" and the light direction.
        double lightIntensityFactor = observerNormal.dot(lightDirection);

        // 9. Create a twilight effect.
        final double twilightHorizon = -0.2;

        // 10. Map the intensity factor to a brightness value [0.0, 1.0].
        float brightness = (float) ((lightIntensityFactor - twilightHorizon) / (1.0 - twilightHorizon));
System.out.println(brightness);
        // 11. Clamp the result to ensure it stays within the valid [0.0, 1.0] range.
        return Mth.clamp(brightness, 0.0F, 1.0F);
    }

    /**
     * Calculates the "up" vector for an observer on the planet's surface using a fixed planetary reference frame.
     * This prevents the time of day from drifting as the planet orbits its star.
     */
    private static Vec3 calculateObserverNormal(Vec3 rotationAxis, double rotationAngle, double observerLatitude) {
        // To solve orbital phase drift, we must establish a fixed reference frame for the planet's rotation
        // that is independent of the star's current position.

        // 1. Define a stable "X-axis" on the planet's equatorial plane. This acts as our "Prime Meridian".
        // We use findPerpendicular to get a consistent vector that is perpendicular to the axis of rotation.
        Vec3 equatorialX = findPerpendicular(rotationAxis);

        // 2. Define a stable "Y-axis" on the equatorial plane.
        // This is perpendicular to both the rotation axis and our new equatorial X-axis.
        Vec3 equatorialY = rotationAxis.cross(equatorialX);

        // 3. Calculate the observer's position on the equator based on the time of day (rotationAngle).
        // The rotation is now relative to our fixed planetary coordinate system. A rotation angle of 0
        // corresponds to the direction of our fixed equatorialX.
        Vec3 equatorialObserverPos = equatorialX.scale(Math.cos(rotationAngle))
                .add(equatorialY.scale(Math.sin(rotationAngle)));

        // 4. Finally, incorporate the observer's latitude to get the final normal vector.
        // This tilts the equatorial position towards the correct pole.
        Vec3 observerNormal = equatorialObserverPos.scale(Math.cos(observerLatitude))
                .add(rotationAxis.scale(Math.sin(observerLatitude)));

        return observerNormal.normalize();
    }

    /**
     * A robust method to find a vector that is perpendicular to a given vector.
     * It avoids issues when the input vector is aligned with the world axes.
     */
    private static Vec3 findPerpendicular(Vec3 v) {
        // If the vector is not aligned with the world's Y-axis, we can use Y-axis for the cross product.
        if (Math.abs(v.y) < 0.99) {
            return new Vec3(0, 1, 0).cross(v).normalize();
        }
        // Otherwise, it's too close to the Y-axis, so we use the X-axis instead.
        else {
            return new Vec3(1, 0, 0).cross(v).normalize();
        }
    }

    /**
     * Rotates a vector around an arbitrary axis using Rodrigues' rotation formula.
     * This is essential for handling planets with tilted axes.
     *
     * @param vec   The vector to rotate.
     * @param axis  The axis of rotation (must be normalized).
     * @param angle The angle of rotation in radians.
     * @return The new, rotated vector.
     */
    public static Vec3 rotateVector(Vec3 vec, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double oneMinusCos = 1 - cos;

        // Rodrigues' formula components
        Vec3 term1 = vec.scale(cos);
        Vec3 term2 = axis.cross(vec).scale(sin);
        Vec3 term3 = axis.scale(axis.dot(vec) * oneMinusCos);

        return term1.add(term2).add(term3);
    }
}




