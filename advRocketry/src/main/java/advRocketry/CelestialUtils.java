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

