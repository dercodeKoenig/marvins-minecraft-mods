package advRocketry;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class AstronomicalLighting {

    public static float calculateAstronomicalBrightness(
            Vec3 lightSourceToPlanet, Vec3 rotationAxis, double timeOfDayAngleDegrees, double observerLatitudeDegrees) {

        Vec3 axis = rotationAxis.normalize();

// 1. Pick the correct perpendicular vector to axis
        Vec3 equatorRef = Math.abs(axis.z) < 0.99 ? new Vec3(0,0,-1).cross(axis).normalize() : new Vec3(1,0,0).cross(axis).normalize();

// 2. Rotate the equatorRef by raw self-rotation
        Vec3 rotatedEquator = CelestialUtils.rotate(equatorRef, axis, -timeOfDayAngleDegrees);

// 3. Mix rotatedEquator with axis to get local normal at latitude
        double latRad = Math.toRadians(observerLatitudeDegrees);
        Vec3 localNormal = axis.scale(Math.sin(latRad)).add(rotatedEquator.scale(Math.cos(latRad)));

// 4. Dot with star direction to get altitude
        Vec3 starDir = lightSourceToPlanet.normalize().scale(-1.0); // planet -> star
        double cosAlt = Mth.clamp(localNormal.dot(starDir), -1.0, 1.0);
        double altitude = Math.asin(cosAlt);

        double brightness = (altitude) / ((Math.PI/2.0));
        System.out.printf("lat=%.1f°, alt=%.1f°%n",
                observerLatitudeDegrees,
                Math.toDegrees(altitude));
        //System.out.println(brightness);

        // 7) final clamp and return
        return Mth.clamp((float) brightness, 0.0F, 1.0F);
    }
}
