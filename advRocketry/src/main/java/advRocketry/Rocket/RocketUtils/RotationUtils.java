package advRocketry.Rocket.RocketUtils;

import advRocketry.Rocket.EntityRocket;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RotationUtils {
    // made mostly by gemini, no idea how it works exactly, but it appears to work
    public static Quaternionf getCurrentRotation(EntityRocket rocket) {
        Vec3 myHeading = rocket.heading.normalize();
        Vec3 desiredFront = rocket.front.normalize();
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

// clamp before acos
        float cosA = (float) Math.max(-1.0, Math.min(1.0, rotatedForward.dot(desiredFront)));
        float rollAngle = (float) Math.acos(cosA);

// avoid cross on nearly identical or opposite vectors
        Vec3 cross = rotatedForward.cross(desiredFront);
        double crossLen = cross.length();
        float sign = (crossLen < 1e-8) ? 1f : Math.signum((float) cross.dot(myHeading));

        Quaternionf qRoll;
        if (Math.abs(rollAngle) < 1e-6f) {
            qRoll = new Quaternionf(); // no roll
        } else {
            qRoll = new Quaternionf().fromAxisAngleRad(
                    (float) myHeading.x, (float) myHeading.y, (float) myHeading.z, rollAngle * sign);
        }

// --- Final quaternion ---
        Quaternionf q = new Quaternionf(qRoll).mul(qTilt);
        q.normalize();
        return q;
    }

    // also written by gemini. or chatgpt idk.
    public static Vec3 localToWorld(EntityRocket rocket, Vec3 localPos) {
        // Get the same final rotation you used in rendering
        Quaternionf q = getCurrentRotation(rocket); // e.g. qRoll * qTilt

        // Convert to JOML Vector3f
        Vector3f lp = new Vector3f(
                (float) (localPos.x - rocket.size.getX() / 2.0),
                (float) (localPos.y - rocket.size.getY() / 2.0),
                (float) (localPos.z - rocket.size.getZ() / 2.0)
        );

        // Rotate local position by quaternion
        q.transform(lp);

        // Translate by entity position
        return rocket.position().add(new Vec3(lp.x, lp.y, lp.z));
    }
}
