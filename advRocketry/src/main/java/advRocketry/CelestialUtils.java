package advRocketry;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Locale;

/**
 * A utility class for celestial mechanics calculations, specifically for converting
 * world-space coordinates into an observer's local sky frame for rendering.
 */
public class CelestialUtils {

    /**
     * Creates a rotation matrix from an axis and an angle in degrees.
     * @param axis The axis of rotation.
     * @param angleDeg The angle of rotation in degrees.
     * @return A Matrix4f representing the rotation.
     */
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
     * Helper function to build a robust observer reference frame on a planet's surface.
     * The frame is right-handed: +X = East, +Y = Zenith (Up), +Z = North.
     * @return An array of 3 vectors: [East, Zenith, North].
     */
    private static Vec3[] getObserverFrame(Vec3 planetAxis, double timeOfDayAngle, double observerLatitude) {
        Vec3 planetNorth = planetAxis.normalize();

        Vec3 worldReference = new Vec3(0, 1, 0);
        if (Math.abs(worldReference.dot(planetNorth)) > 0.99) {
            worldReference = new Vec3(1, 0, 0);
        }
        Vec3 equatorRef = planetNorth.cross(worldReference).normalize();
        Vec3 obsLongitudeVec = rotate(equatorRef, planetNorth, timeOfDayAngle);

        if (Math.abs(observerLatitude) > 89.999) {
            Vec3 observerZenith = planetNorth.scale(Math.signum(observerLatitude));
            Vec3 observerNorth = obsLongitudeVec.scale(-Math.signum(observerLatitude));
            Vec3 observerEast = observerZenith.cross(observerNorth);
            return new Vec3[]{observerEast, observerZenith, observerNorth};
        }

        double latRad = Math.toRadians(observerLatitude);
        Vec3 observerZenith = obsLongitudeVec.scale(Math.cos(latRad)).add(planetNorth.scale(Math.sin(latRad))).normalize();
        Vec3 observerNorth = planetNorth.subtract(observerZenith.scale(planetNorth.dot(observerZenith))).normalize();
        Vec3 observerEast = observerZenith.cross(observerNorth);
        return new Vec3[]{observerEast, observerZenith, observerNorth};
    }

    /**
     * [NEW] Creates a view matrix that transforms world coordinates into the observer's
     * local sky frame, also incorporating the player's head rotation.
     * This matrix should be used as the 'view' matrix for rendering celestial bodies.
     *
     * @param playerViewMatrix The original view matrix from the game's camera.
     * @param planetAxis The rotational axis of the observer's planet.
     * @param timeOfDayAngle The rotation of the observer's planet in degrees.
     * @param observerLatitude The latitude of the observer in degrees.
     * @return A Matrix4f representing the combined sky view transformation.
     */
    public static Matrix4f createSkyViewMatrix(Matrix4f playerViewMatrix, Vec3 planetAxis, double timeOfDayAngle, double observerLatitude) {
        // 1. Get the player's head rotation by removing the translation from the view matrix.
        Matrix4f playerHeadRotation = new Matrix4f(playerViewMatrix);
        playerHeadRotation.setTranslation(0, 0, 0);

        // 2. Get the observer's local coordinate system (East, Zenith, North) in world space.
        Vec3[] frame = getObserverFrame(planetAxis, timeOfDayAngle, observerLatitude);
        Vec3 east = frame[0];
        Vec3 zenith = frame[1];
        Vec3 north = frame[2];

        // 3. Create the view matrix, which is the inverse of the observer's frame transformation matrix.
        // Since the frame is an orthonormal basis (a pure rotation), the inverse is simply the transpose.
        // This matrix transforms FROM world space TO the observer's static sky frame.
        Matrix4f worldToObserverFrame = new Matrix4f();
        worldToObserverFrame.set(0, 0, (float)east.x); worldToObserverFrame.set(0, 1, (float)east.y); worldToObserverFrame.set(0, 2, (float)east.z);
        worldToObserverFrame.set(1, 0, (float)zenith.x); worldToObserverFrame.set(1, 1, (float)zenith.y); worldToObserverFrame.set(1, 2, (float)zenith.z);
        worldToObserverFrame.set(2, 0, (float)north.x); worldToObserverFrame.set(2, 1, (float)north.y); worldToObserverFrame.set(2, 2, (float)north.z);

        // 4. Combine the player's head rotation with the observer frame transformation.
        // The final view takes a point in the world, moves it into the static observer frame,
        // and then rotates it according to where the player is looking.
        return playerHeadRotation.mul(worldToObserverFrame);
    }

    // --- Other physics calculations ---
    public static final double G = 1;

    public static double getRealDistanceFromValue(double value){
        return value / 100f * 1.496 * Math.pow(10, 11);
    }

    public static double getRealMassFromValue(double value){
        double massEarth = 5.972 * Math.pow(10, 24);
        return value / 100f * massEarth;
    }

    public static double calculateOrbitalPeriodTicks(double mass1, double mass2, double distance) {
        double combinedMass = getRealMassFromValue(mass1)  + getRealDistanceFromValue(mass2);
        return 2 * Math.PI * Math.sqrt(Math.pow(getRealDistanceFromValue(distance), 3) / (G * combinedMass)) * 20;
    }
}

