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

        // 2. Calculate the observer's surface normal vector in world space.
        // This vector points "straight up" from the surface where the observer is standing.
        Vec3 observerNormal = calculateObserverNormal(rotationAxis, rotationAngle, observerLatitude, lightDirection);

        // 3. The dot product between the light direction and the observer's normal gives the cosine of the angle.
        // This value is a direct measure of how directly the star is shining on the observer.
        double lightIntensityFactor = observerNormal.dot(lightDirection);

        // 4. Create a twilight effect by considering the star to still provide light
        // when it's slightly below the horizon. -0.2 is a good starting value.
        final double twilightHorizon = -0.2;

        // 5. Map the intensity factor to a brightness value [0.0, 1.0].
        // We map the intensity range [twilightHorizon, 1.0] to the brightness range [0.0, 1.0].
        float brightness = (float) ((lightIntensityFactor - twilightHorizon) / (1.0 - twilightHorizon));

        // 6. Clamp the result to ensure it stays within the valid [0.0, 1.0] range.
        System.out.println(brightness);
        return Mth.clamp(brightness, 0.0F, 1.0F);
    }

    /**
     * Calculates the "up" vector for an observer on the planet's surface, correctly oriented to the light source.
     */
    private static Vec3 calculateObserverNormal(Vec3 rotationAxis, double rotationAngle, double observerLatitude, Vec3 lightDirection) {
        // Project the light direction onto the planet's equatorial plane.
        // This gives us a vector that points towards the "sub-stellar point" on the equator, our reference for "noon".
        Vec3 noonDirection = lightDirection.subtract(rotationAxis.scale(lightDirection.dot(rotationAxis)));

        // Handle the edge case where the star is directly over a pole.
        // In this case, the projection is a zero vector, so we pick an arbitrary perpendicular vector.
        if (noonDirection.lengthSqr() < 1.0E-6) {
            noonDirection = findPerpendicular(rotationAxis);
        } else {
            noonDirection = noonDirection.normalize();
        }

        // Create a second basis vector on the equatorial plane, perpendicular to the noon vector.
        // This vector points towards the "morning terminator" (sunrise line).
        Vec3 morningDirection = rotationAxis.cross(noonDirection);

        // Calculate the observer's position on the equator based on the time of day (rotationAngle).
        // This is a 2D rotation on the equatorial plane, starting from the noon direction.
        Vec3 equatorialObserverPos = noonDirection.scale(Math.cos(rotationAngle))
                .add(morningDirection.scale(Math.sin(rotationAngle)));

        // Finally, incorporate the observer's latitude to get the final normal vector.
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

