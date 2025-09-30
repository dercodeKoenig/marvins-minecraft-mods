package advRocketry;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Locale;

public class CelestialUtils {

    public static Matrix4f fromAxisAngle(Vec3 axis, double angleDeg) {
        double angleRad = Math.toRadians(angleDeg);
        double c = Math.cos(angleRad);
        double s = Math.sin(angleRad);
        double t = 1.0 - c;
        Vec3 a = axis.normalize();

        Matrix4f mat = new Matrix4f();
        mat.set(0, 0, (float)(c + a.x * a.x * t));
        mat.set(1, 1, (float)(c + a.y * a.y * t));
        mat.set(2, 2, (float)(c + a.z * a.z * t));

        mat.set(0, 1, (float)(a.x * a.y * t - a.z * s));
        mat.set(1, 0, (float)(a.x * a.y * t + a.z * s));

        mat.set(0, 2, (float)(a.x * a.z * t + a.y * s));
        mat.set(2, 0, (float)(a.x * a.z * t - a.y * s));

        mat.set(1, 2, (float)(a.y * a.z * t - a.x * s));
        mat.set(2, 1, (float)(a.y * a.z * t + a.x * s));

        return mat;
    }

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

    /**
     * Helper function to build a robust observer reference frame.
     * Returns an array of 3 vectors: [East, Zenith, North].
     */
    private static Vec3[] getObserverFrame(Vec3 planetAxis, double timeOfDayAngle, double observerLatitude) {
        Vec3 planetNorth = planetAxis.normalize();

        // Find a vector perpendicular to the planet's axis to serve as a reference on the equator.
        // This is more robust than using a fixed vector like (1,0,0).
        Vec3 worldReference = new Vec3(0, 1, 0);
        if (Math.abs(worldReference.dot(planetNorth)) > 0.99) {
            worldReference = new Vec3(1, 0, 0); // Use a different reference if axis is aligned
        }

        Vec3 equatorRef = planetNorth.cross(worldReference).normalize();

        // This vector points from the planet center to the observer's longitude on the equator plane.
        Vec3 obsLongitudeVec = rotate(equatorRef, planetNorth, timeOfDayAngle);

        // Handle the poles as a special case to avoid gimbal lock and undefined directions.
        if (Math.abs(observerLatitude) > 89.999) {
            // At the pole, Zenith is aligned with the planet's axis.
            Vec3 observerZenith = planetNorth.scale(Math.signum(observerLatitude));

            // "North" along the surface is ambiguous. We define it as pointing opposite
            // to the observer's longitude vector to provide a stable reference direction.
            // For the South Pole, it points along the longitude vector.
            Vec3 observerNorth = obsLongitudeVec.scale(-Math.signum(observerLatitude));

            // East is perpendicular to Zenith and North, forming a right-handed system (E = Z x N).
            Vec3 observerEast = observerZenith.cross(observerNorth);

            return new Vec3[]{observerEast, observerZenith, observerNorth};
        }

        // For non-polar latitudes, calculate the frame normally.
        // The observer's "Up" (Zenith) is found using spherical coordinate interpolation.
        // This is more direct and stable than the previous rotation method.
        double latRad = Math.toRadians(observerLatitude);
        Vec3 observerZenith = obsLongitudeVec.scale(Math.cos(latRad)).add(planetNorth.scale(Math.sin(latRad))).normalize();

        // The observer's "North" on the tangent plane is the component of the planet's North axis
        // that is perpendicular to the Zenith.
        Vec3 observerNorth = planetNorth.subtract(observerZenith.scale(planetNorth.dot(observerZenith))).normalize();

        // The observer's "East" is perpendicular to Zenith and North (E = Z x N).
        Vec3 observerEast = observerZenith.cross(observerNorth);

        return new Vec3[]{observerEast, observerZenith, observerNorth};
    }


    /**
     * [REVISED] Calculates the direction vector of a celestial body from the
     * perspective of an observer on a rotating planet. This version is more robust.
     *
     * @param bodyPos The position of the celestial body in world coordinates.
     * @param planetPos The position of the planet in world coordinates.
     * @param planetAxis The rotational axis of the planet (e.g., from South to North pole).
     * @param timeOfDayAngle The rotation of the planet in degrees.
     * @param observerLatitude The latitude of the observer on the planet's surface in degrees.
     * @return A normalized Vec3 representing the body's direction in the observer's
     * local coordinate system (x=East, y=Up/Zenith, z=North).
     */
    public static Vec3 getBodyDirectionLocal(Vec3 bodyPos, Vec3 planetPos, Vec3 planetAxis, double timeOfDayAngle, double observerLatitude) {
        // Step 1: Get the direction to the body in the fixed world frame.
        Vec3 bodyDirWorld = bodyPos.subtract(planetPos).normalize();

        // Step 2: Get the observer's local coordinate system (East, Zenith, North) in world space.
        Vec3[] frame = getObserverFrame(planetAxis, timeOfDayAngle, observerLatitude);
        Vec3 observerEast = frame[0];
        Vec3 observerZenith = frame[1];
        Vec3 observerNorth = frame[2];

        // Step 3: Project the world direction onto the observer’s local frame axes.
        double eastComp = bodyDirWorld.dot(observerEast);
        double upComp = bodyDirWorld.dot(observerZenith);
        double northComp = bodyDirWorld.dot(observerNorth);

        // Final sun direction in local sky coordinates.
        return new Vec3(eastComp, upComp, northComp).normalize();
    }

    /**
     * [NEW & FIXED] Calculates the orientation matrix for a target planet as seen from an observer's local sky.
     *
     * @param planetAxis The observer's planet's rotational axis.
     * @param timeOfDayAngle The rotation of the observer's planet in degrees.
     * @param observerLatitude The latitude of the observer in degrees.
     * @param targetPlanetAxis The rotational axis of the target planet in world coordinates.
     * @param targetPlanetSelfRotation The self-rotation of the target planet around its axis in degrees.
     * @return A Matrix4f representing the rotation of the target planet in the observer's local sky frame.
     */
    public static Matrix4f getBodyOrientationMatrix(Vec3 planetAxis, double timeOfDayAngle, double observerLatitude, Vec3 targetPlanetAxis, double targetPlanetSelfRotation) {
        // Step 1: Get the observer's local coordinate system (East, Zenith, North) in world space.
        Vec3[] frame = getObserverFrame(planetAxis, timeOfDayAngle, observerLatitude);
        Vec3 observerEast = frame[0];
        Vec3 observerZenith = frame[1];
        Vec3 observerNorth = frame[2];

        // Step 2: Create a rotation matrix that transforms from world space to the observer's local sky space.
        // This coordinate system must be consistent with getBodyDirectionLocal.
        // We define the local axes as: X -> East, Y -> Zenith(Up), Z -> North.
        // This is a right-handed system because Zenith.cross(North) = East, which corresponds to Y x Z = X.
        Matrix4f worldToSkyRotation = new Matrix4f();
        // Row 0 (local X axis = East)
        worldToSkyRotation.set(0, 0, (float)observerEast.x);
        worldToSkyRotation.set(0, 1, (float)observerEast.y);
        worldToSkyRotation.set(0, 2, (float)observerEast.z);
        // Row 1 (local Y axis = Zenith)
        worldToSkyRotation.set(1, 0, (float)observerZenith.x);
        worldToSkyRotation.set(1, 1, (float)observerZenith.y);
        worldToSkyRotation.set(1, 2, (float)observerZenith.z);
        // Row 2 (local Z axis = North)
        worldToSkyRotation.set(2, 0, (float)observerNorth.x);
        worldToSkyRotation.set(2, 1, (float)observerNorth.y);
        worldToSkyRotation.set(2, 2, (float)observerNorth.z);

        // Step 3: Create the target planet's complete orientation in world space.
        // This is a combination of its axial tilt and its own rotation around that axis.

        // 3a. First, create the rotation for the planet's own spin around its default model axis (e.g., Y-axis).
        Matrix4f selfRotation = fromAxisAngle(new Vec3(0, 1, 0), targetPlanetSelfRotation);

        // 3b. Then, create the rotation to perform the axial tilt. This aligns the model's default pole
        // with the target planet's actual rotation axis in world space.
        Vec3 modelUp = new Vec3(0, 1, 0);
        Vec3 targetNorth = targetPlanetAxis.normalize();
        Vec3 rotAxis = modelUp.cross(targetNorth);

        Matrix4f axisTilt = new Matrix4f(); // Identity
        if (rotAxis.length() > 1e-9) {
            double rotAngleRad = Math.asin(rotAxis.length());
            axisTilt = fromAxisAngle(rotAxis.normalize(), Math.toDegrees(rotAngleRad));
        } else if (modelUp.dot(targetNorth) < 0) { // Handle 180-degree case (anti-parallel)
            axisTilt = fromAxisAngle(new Vec3(1,0,0), 180);
        }

        // 3c. Combine them: transformations apply right-to-left, so we first spin, then tilt.
        Matrix4f targetWorldRotation = axisTilt.mul(selfRotation);

        // Step 4: Combine the matrices.
        // The final orientation is the target's world orientation transformed into the observer's sky space.
        return worldToSkyRotation.mul(targetWorldRotation);
    }

    // custom set Gravitational constant2
    public static final double G = 1;

    /**
     * Calculates real distance from value where 100 = 1 astronomical unit
     * @param value 100 equals 1 astronomical unit
     * @return real distance in m
     */
    public static  double getRealDistanceFromValue(double value){
        return value / 100f * 1.496 * Math.pow(10, 11);
    }

    /**
     * Calculates real mass from value where 100 = 1 earth mass
     * @param value 100 = 1 earth mass
     * @return real math in kg
     */
    public static double getRealMassFromValue(double value){
        double massEarth = 5.972 * Math.pow(10, 24); // kg
        return value / 100f * massEarth;
    }

    /**
     * Calculate the orbital period of two bodies in seconds
     * @param mass1 Mass of the first body (kg)
     * @param mass2 Mass of the second body (kg)
     * @param distance Distance between the centers of the two bodies (meters)
     * @return Orbital period in seconds
     */
    public static double calculateOrbitalPeriodTicks(double mass1, double mass2, double distance) {
        double combinedMass = getRealMassFromValue(mass1)  + getRealDistanceFromValue(mass2);
        return 2 * Math.PI * Math.sqrt(Math.pow(getRealDistanceFromValue(distance), 3) / (G * combinedMass)) * 20;
    }
}

