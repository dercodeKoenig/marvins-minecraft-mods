package advRocketry.Rocket;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RotationUtils {
    // made mostly by gemini, no idea how it works exactly, but it appears to work
    public static Quaternionf getCurrentRotation(EntityRocket rocket, float partialTick) {
        Vec3 myHeading = rocket.controller.getHeading().subtract(rocket.controller.headingRotationRate.scale(1-partialTick)).normalize();
        Vec3 desiredFront = rocket.controller.getFront().subtract(rocket.controller.frontRotationRate.scale(1-partialTick)).normalize();
        Vec3 worldUp = new Vec3(0, 1, 0);

        // --- Step A: tilt (worldUp -> heading) ---
        Vec3 axisTilt = worldUp.cross(myHeading);
        double axisLen = axisTilt.length();
        float dot = (float) Math.max(-1.0, Math.min(1.0, worldUp.dot(myHeading)));

        Quaternionf qTilt;

        if (axisLen < 1e-8) {
            // heading nearly parallel or anti-parallel to up
            if (dot > 0.9999f) {
                qTilt = new Quaternionf(); // no tilt
            } else {
                // 180° rotation around X (safe fallback axis)
                qTilt = new Quaternionf().fromAxisAngleRad(1f, 0f, 0f, (float) Math.PI);
            }
        } else {
            axisTilt = axisTilt.scale(1.0 / axisLen); // normalize safely
            float angleTilt = (float) Math.acos(dot);
            qTilt = new Quaternionf().fromAxisAngleRad(
                    (float) axisTilt.x, (float) axisTilt.y, (float) axisTilt.z, angleTilt);
        }

        // --- Step B: roll (align front) ---
        Vector3f modelForward = rocket.initialFront.toVector3f();
        qTilt.transform(modelForward); // rotated forward after tilt
        Vec3 rotatedForward = new Vec3(modelForward.x(), modelForward.y(), modelForward.z()).normalize();

        float dotA = (float) rotatedForward.dot(desiredFront);
        float cosA = (float) Math.max(-1.0, Math.min(1.0, dotA));

        Quaternionf qRoll = new Quaternionf();

        if (cosA < -0.9999f) {
            // --- EXACT 180 FLIP CASE ---
            // If we are exactly opposite, rotate by PI (180°) around the heading axis.
            // The sign doesn't matter here because 180 and -180 are the same orientation.
            qRoll.fromAxisAngleRad(
                    (float) myHeading.x, (float) myHeading.y, (float) myHeading.z, (float) Math.PI
            );
        } else if (cosA < 0.9999f) {
            // --- STANDARD CASE ---
            float rollAngle = (float) Math.acos(cosA);
            Vec3 cross = rotatedForward.cross(desiredFront);

            // We check if the cross product (the rotation direction)
            // is aligned with or against our "spine" (myHeading)
            float sign = (float) Math.signum(cross.dot(myHeading));

            // If for some reason the cross product is zero but we aren't 180°, sign is 0.
            // We default to 1.0 to ensure a rotation happens.
            if (sign == 0) sign = 1.0f;

            qRoll.fromAxisAngleRad(
                    (float) myHeading.x, (float) myHeading.y, (float) myHeading.z, rollAngle * sign
            );
        }
        // else: cosA > 0.9999, meaning we are already aligned. qRoll stays Identity.

        // --- Final quaternion ---
        Quaternionf q = new Quaternionf(qRoll).mul(qTilt);
        q.normalize();
        return q;
    }

    // also written by gemini. or chatgpt idk.
    // rotates around the rocket center. the render code also rotates around the center. should be fine
    public static Vec3 localToWorld(EntityRocket rocket, Vec3 localPos) {
        // Get final rotation quaternion
        Quaternionf q = getCurrentRotation(rocket, 1);

        // Offset local position to rotate around rocket center
        Vector3f lp = new Vector3f(
                (float) (localPos.x - rocket.size.getX() / 2.0),
                (float) (localPos.y - rocket.size.getY() / 2.0),
                (float) (localPos.z - rocket.size.getZ() / 2.0)
        );

        // Rotate around center
        q.transform(lp);

        // Translate to entity position (bottom of rocket)
        // Add half Y back to move to center if needed for rendering
        return rocket.position().add(new Vec3(lp.x, lp.y + rocket.size.getY() / 2.0, lp.z));
    }

}
