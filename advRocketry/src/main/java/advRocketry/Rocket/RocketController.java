package advRocketry.Rocket;

import ARLib.network.PacketEntity;
import advRocketry.Registry;
import advRocketry.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class RocketController {

    EntityRocket rocket;

    Vec3 targetHeading = new Vec3(0, 0, 0);
    double currentThrust;
    Vec3 currentSecondaryThrust;

    public RocketController(EntityRocket rocket){
        this.rocket = rocket;
    }

    public void tick(){
        tickRotation();
        tickController();
        makeThrustParticles();
    }

    public void tickRotation() {
        // Rotation Speed: How quickly the rocket can turn its heading towards the target acceleration vector.
        final double ROTATION_RATE = 0.05;// / rocket.size.getY();
        // rotate heading first
        // Slowly interpolate the rocket's current 'heading' vector towards the 'targetHeading'.
        // This simulates the actual rotational speed limit of the rocket.
        Vec3 rotationCorrection = targetHeading.subtract(rocket.heading).scale(ROTATION_RATE);
        rocket.heading = rocket.heading.add(rotationCorrection).normalize();

        // now try to get the front align more toward the target front
        // the front is not always valid because of the heading
        Vec3 targetFrontValid = rocket.heading.cross(rocket.getTargetFront().cross(rocket.heading)).normalize();
        if (targetFrontValid.dot(rocket.front) < -0.9) // get some movement if it is directly on the other side
            targetFrontValid = rocket.heading.cross(rocket.front);
        rotationCorrection = targetFrontValid.subtract(rocket.front).scale(ROTATION_RATE);
        Vec3 newFront = rocket.front.add(rotationCorrection).normalize();
        // make sure the front is 100% always orthogonal, just for extra security
        Vec3 right = rocket.heading.cross(newFront).normalize();
        rocket.front = right.cross(rocket.heading).normalize();
    }

    // pd controller mostly written by gemini should be used to have the rocket spawn at some offset and find its way down to the landing area
    // it should also scan (if no launchpad structure) to land at some area where there is a flat area
    public void tickController() {

        if(rocket.getTargetPosition() == null){
            targetHeading = rocket.getDefaultTargetHeading();
            currentThrust = 0;
            currentSecondaryThrust = new Vec3(0,0,0);
            return;
        }

        // --- Configuration Parameters (Tune these for desired behavior) ---
        // Proportional Gain: How aggressively the rocket tries to close the distance.
        final double K_P = 0.001;
        // Damping Gain (Derivative-like): How aggressively the rocket slows down to prevent overshoot.
        final double K_D = Math.sqrt(K_P) * 2;
        // Structural/Breakage Limit: This is the maximum acceleration the vehicle can withstand.
        final double MAX_STRUCTURAL_ACCEL = rocket.getMaxAcceleration();
        // secondary thruster force
        final double SECONDARY_THRUSTERS_FORCE = rocket.getThrustMax() / 1000;

        // --- 1. Calculate Required Acceleration (The PD Controller) ---

        // B. Position Error (p_target - p)
        Vec3 positionError = rocket.getTargetPosition().subtract(rocket.getPosition(0));

        // C. Damping (Velocity Error - using current velocity for simplicity)
        Vec3 currentVelocity = rocket.getDeltaMovement();

        // D. Desired Acceleration (a_desired)
        // Formula: a_desired = (K_P * Position_Error) - (K_D * Current_Velocity)
        // The result is the absolute acceleration vector the rocket *needs* to follow the path.
        Vec3 desiredAcceleration = positionError.scale(K_P).subtract(currentVelocity.scale(K_D));

        // NOTE: If you needed to factor in gravity/other external forces, you would
        // add an opposing vector here: desiredAcceleration = ... .add(Vec3.GRAVITY.scale(-1));
        desiredAcceleration = desiredAcceleration.add(new Vec3(0, 1, 0).scale(rocket.getGravity()));

        // --- 2. Calculate Thrust & Heading ---
        // TODO: calculate main thrusters first and use the secondary only for part of the force that was not applied ( sideways/ break )
        if (rocket.canUseSecondaryEngines()) {
            // use secondary thrusters in space for fine controll
            Vec3 secondaryThrustersForce = desiredAcceleration.scale(rocket.getMass());
            if (secondaryThrustersForce.length() > SECONDARY_THRUSTERS_FORCE) {
                secondaryThrustersForce = secondaryThrustersForce.normalize().scale(SECONDARY_THRUSTERS_FORCE);
            }
            Vec3 secondaryThrustersAcceleration = secondaryThrustersForce.scale(1 / rocket.getMass());
            desiredAcceleration.subtract(secondaryThrustersAcceleration);
            rocket.setDeltaMovement(rocket.getDeltaMovement().add(secondaryThrustersAcceleration));

            currentSecondaryThrust = secondaryThrustersForce;
            // TODO: render secondaryThrustersForce particles based on secondaryThrustersForce
        }

        if (desiredAcceleration.length() > 0.0001 && rocket.canUseMainEngines()) {
            // 1. Max Acceleration the engine can *possibly* deliver. ( including scale for bootup time )
            final double MAX_PHYSICAL_ACCEL = rocket.getThrustMax() / rocket.getMass() * getBootTimeThrustMultiplier(rocket);
            // 2. The absolute maximum acceleration we are allowed to use this frame.
            // This ensures we never break the rocket (MAX_STRUCTURAL_ACCEL) AND never demand more thrust than the engine can provide (MAX_PHYSICAL_ACCEL).
            final double MAX_ALLOWED_ACCEL = Math.min(MAX_PHYSICAL_ACCEL, MAX_STRUCTURAL_ACCEL);
            // The heading the rocket *needs* to point towards to achieve the desired acceleration.
            targetHeading = desiredAcceleration.normalize();
            // Calculate the magnitude of acceleration needed from the PD controller.
            double neededAcceleration = desiredAcceleration.length();
            // Cap the needed acceleration by the final allowed limit.
            // We only need to use the MAX_ALLOWED_ACCEL cap here.
            double effectiveAcceleration = Math.min(neededAcceleration, MAX_ALLOWED_ACCEL);
            // The component of the effective acceleration that aligns with the current (limited) heading.
            // This ensures we only thrust in the direction we are currently pointing.
            double actualThrustAccel = effectiveAcceleration * Math.max(0, rocket.heading.dot(targetHeading) - 0.9)*10;
            // Thrust is applied along the current 'heading' direction.
            // We use the 'actualThrustAccel' determined by the PD control and the rotation limit.
            Vec3 thrustVector = rocket.heading.scale(actualThrustAccel);
            rocket.setDeltaMovement(rocket.getDeltaMovement().add(thrustVector));
            // Calculate the Thrust Multiplier (0.0 to 1.0)
            // This is the current actually delivered thrust relative to the max possible thrust for rendering and fuel consumption
            double ThrustMultiplier = (actualThrustAccel * rocket.getMass()) / rocket.getThrustMax();
            currentThrust = ThrustMultiplier;
            // TODO: burn rocket fuel

        } else {
            targetHeading = rocket.getDefaultTargetHeading();
        }
    }


    public float getBootTimeThrustMultiplier(EntityRocket rocket) {
        int halfBootTime = EntityRocket.ENGINE_BOOT_TIME / 2;
        if (rocket.getMainEnginesBootUp() < halfBootTime) return 0;
        return (float) Math.pow((float) (rocket.getMainEnginesBootUp() - halfBootTime) / halfBootTime, 2);
    }


    public void makeThrustParticles(){

        if (rocket.level().isClientSide) {
            if (rocket.getMainEnginesBootUp() != 0) {
                float relativeBootTimeLin = (float) rocket.getMainEnginesBootUp() / EntityRocket. ENGINE_BOOT_TIME;
                float bootupParticleProb = (float)Math.sqrt(relativeBootTimeLin);
                int maxParticlesPerTick = 50;
                int maxParticlePerEngine = 3;
                for (BlockPos i : rocket.getEnginePositions()) {
                    Vec3 worldPos = RotationUtils.localToWorld(rocket, new Vec3(i.getX() + 0.5, i.getY() + 0.02, i.getZ() + 0.5));
                    for (int j = 0; j < maxParticlePerEngine; j++) {
                        if (relativeBootTimeLin < 0.99) {
                            if (rocket.level().random.nextFloat() > bootupParticleProb) {
                                continue;
                            }
                        }

                        float engineNumSpeedMultiplier = 1;
                        if (rocket.getEnginePositions().size() * maxParticlePerEngine > maxParticlesPerTick) {
                            if (rocket.level().random.nextFloat() > (float) maxParticlesPerTick / (rocket.getEnginePositions().size() * maxParticlePerEngine)) {
                                continue;
                            }
                            engineNumSpeedMultiplier = (float) (rocket.getEnginePositions().size() * maxParticlePerEngine) / maxParticlesPerTick;
                        }

                        rocket.level().addParticle(
                                Registry.ROCKET_FLAME.get(),
                                worldPos.x,
                                worldPos.y,
                                worldPos.z,
                                rocket.heading.x * -1 * (currentThrust + 0.2) * relativeBootTimeLin * engineNumSpeedMultiplier + rocket.getDeltaMovement().x,
                                rocket.heading.y * -1 * (currentThrust + 0.2) * relativeBootTimeLin * engineNumSpeedMultiplier + rocket.getDeltaMovement().y,
                                rocket.heading.z * -1 * (currentThrust + 0.2) * relativeBootTimeLin * engineNumSpeedMultiplier + rocket.getDeltaMovement().z
                        );
                    }
                }
                //System.out.println(currentThrust);
            }
        }
    }
}
