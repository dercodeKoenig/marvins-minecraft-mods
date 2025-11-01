package advRocketry.Rocket.RocketUtils;

import ARLib.network.PacketEntity;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Registry;
import advRocketry.Rocket.EntityRocket;
import advRocketry.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public class RocketController {

    public static double getGravityMultiplier(EntityRocket rocket){
        ResourceLocation dimensionId = rocket.level().dimension().location();
        Dimension dimension = DimensionManager.get(dimensionId);
        double massMultiplier = 1;
        if (dimension != null) { // registered in DimensionManager
            massMultiplier = dimension.getEarthMassMultiplier();
        }
        return massMultiplier;
    }

    public static void tickRotation(EntityRocket rocket) {
        // Rotation Speed: How quickly the rocket can turn its heading towards the target acceleration vector.
        final double ROTATION_RATE = 0.05;// / rocket.size.getY();
        // rotate heading first
        // Slowly interpolate the rocket's current 'heading' vector towards the 'targetHeading'.
        // This simulates the actual rotational speed limit of the rocket.
        Vec3 rotationCorrection = rocket.targetHeading.subtract(rocket.heading).scale(ROTATION_RATE);
        Vec3 newHeading = rocket.heading.add(rotationCorrection).normalize();
        if (!newHeading.equals(rocket.heading)) {
            rocket.heading = newHeading;
            CompoundTag headingUpdate = new CompoundTag();
            headingUpdate.put("heading", Utils.serializeVec3(rocket.heading));
            PacketDistributor.sendToPlayersTrackingEntity(rocket, PacketEntity.getEntityPacket(rocket, headingUpdate));
        }

        // now try to get the front align more toward the target front
        // the front is not always valid because of the heading
        Vec3 targetFrontValid = rocket.heading.cross(rocket.targetFront.cross(rocket.heading)).normalize();
        if (targetFrontValid.dot(rocket.front) < -0.9) // get some movement if it is directly on the other side
            targetFrontValid = rocket.heading.cross(rocket.front);
        rotationCorrection = targetFrontValid.subtract(rocket.front).scale(ROTATION_RATE);
        Vec3 newFront = rocket.front.add(rotationCorrection).normalize();
        // make sure the front is 100% always orthogonal, just for extra security
        Vec3 right = rocket.heading.cross(newFront).normalize();
        newFront = right.cross(rocket.heading).normalize();

        if (!newFront.equals(rocket.front)) {
            rocket.front = newFront;
            CompoundTag headingUpdate = new CompoundTag();
            headingUpdate.put("front", Utils.serializeVec3(rocket.front));
            PacketDistributor.sendToPlayersTrackingEntity(rocket, PacketEntity.getEntityPacket(rocket, headingUpdate));
        }
    }

    // pd controller mostly written by gemini should be used to have the rocket spawn at some offset and find its way down to the landing area
    // it should also scan (if no launchpad structure) to land at some area where there is a flat area
    public static void tickController(EntityRocket rocket) {

        if(rocket.targetPosition == null){
            rocket.enableMainEngines(false);
            rocket.enableSecondaryEngines(false);
            return;
        }

        // --- Configuration Parameters (Tune these for desired behavior) ---
        // Proportional Gain: How aggressively the rocket tries to close the distance.
        final double K_P = 0.005;
        // Damping Gain (Derivative-like): How aggressively the rocket slows down to prevent overshoot.
        final double K_D = 0.4 * rocket.controllerKDMultiplier;
        // Structural/Breakage Limit: This is the maximum acceleration the vehicle can withstand.
        final double MAX_STRUCTURAL_ACCEL = rocket.getMaxAcceleration();
        // secondary thruster force
        final double SECONDARY_THRUSTERS_FORCE = rocket.getThrustMax() / 1000;

        // --- 1. Calculate Required Acceleration (The PD Controller) ---

        // B. Position Error (p_target - p)
        Vec3 positionError = rocket.targetPosition.subtract(rocket.getPosition(0));

        // C. Damping (Velocity Error - using current velocity for simplicity)
        Vec3 currentVelocity = rocket.getDeltaMovement();

        // D. Desired Acceleration (a_desired)
        // Formula: a_desired = (K_P * Position_Error) - (K_D * Current_Velocity)
        // The result is the absolute acceleration vector the rocket *needs* to follow the path.
        Vec3 desiredAcceleration = positionError.scale(K_P).subtract(currentVelocity.scale(K_D));

        // NOTE: If you needed to factor in gravity/other external forces, you would
        // add an opposing vector here: desiredAcceleration = ... .add(Vec3.GRAVITY.scale(-1));
        desiredAcceleration = desiredAcceleration.add(new Vec3(0, 1, 0).scale(rocket.getGravity() * getGravityMultiplier(rocket)));

        // --- 2. Calculate Thrust & Heading ---
// TODO: calculate main thrusters first and use the secondary only for part of the force that was not applied ( sideways/ break )
        if (rocket.canUseSecondaryEngines()) {
            // use secondary thrusters in space for fine controll
            Vec3 secondaryThrustersForce = desiredAcceleration.scale(rocket.getMass());
            if (secondaryThrustersForce.length() > SECONDARY_THRUSTERS_FORCE) {
                secondaryThrustersForce = secondaryThrustersForce.normalize().scale(SECONDARY_THRUSTERS_FORCE);
            }
            // TODO: render secondaryThrustersForce particles
            Vec3 secondaryThrustersAcceleration = secondaryThrustersForce.scale(1 / rocket.getMass());
            desiredAcceleration.subtract(secondaryThrustersAcceleration);
            rocket.setDeltaMovement(rocket.getDeltaMovement().add(secondaryThrustersAcceleration));

            rocket.setCurrentSecondaryThrustAndSync(secondaryThrustersForce);
        }

        if (desiredAcceleration.length() > 0.0001 && rocket.shouldEnableMainEngines()) {
            // 1. Max Acceleration the engine can *possibly* deliver. ( including scale for bootup time )
            final double MAX_PHYSICAL_ACCEL = rocket.getThrustMax() / rocket.getMass() * rocket.getBootTimeThrustMultiplier();
            // 2. The absolute maximum acceleration we are allowed to use this frame.
            // This ensures we never break the rocket (MAX_STRUCTURAL_ACCEL) AND never demand more thrust than the engine can provide (MAX_PHYSICAL_ACCEL).
            final double MAX_ALLOWED_ACCEL = Math.min(MAX_PHYSICAL_ACCEL, MAX_STRUCTURAL_ACCEL);
            // The heading the rocket *needs* to point towards to achieve the desired acceleration.
            Vec3 targetHeading = desiredAcceleration.normalize();
            rocket.setTargetHeading(targetHeading);
            // Calculate the magnitude of acceleration needed from the PD controller.
            double neededAcceleration = desiredAcceleration.length();
            // Cap the needed acceleration by the final allowed limit.
            // We only need to use the MAX_ALLOWED_ACCEL cap here.
            double effectiveAcceleration = Math.min(neededAcceleration, MAX_ALLOWED_ACCEL);
            // The component of the effective acceleration that aligns with the current (limited) heading.
            // This ensures we only thrust in the direction we are currently pointing.
            double actualThrustAccel = effectiveAcceleration * Math.max(0, rocket.heading.dot(targetHeading) * 3 - 2);
            // Thrust is applied along the current 'heading' direction.
            // We use the 'actualThrustAccel' determined by the PD control and the rotation limit.
            Vec3 thrustVector = rocket.heading.scale(actualThrustAccel);
            rocket.setDeltaMovement(rocket.getDeltaMovement().add(thrustVector));
            // Calculate the Thrust Multiplier (0.0 to 1.0)
            // This is the current actually delivered thrust relative to the max possible thrust for rendering and fuel consumption
            double ThrustMultiplier = (actualThrustAccel * rocket.getMass()) / rocket.getThrustMax();
            rocket.setCurrentThrustAndSync(ThrustMultiplier);
            // TODO: burn fuel

        } else {
            rocket.setTargetHeading(rocket.defaultTargetHeading);
        }
    }

}
