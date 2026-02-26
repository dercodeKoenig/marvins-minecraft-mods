package advRocketry.Rocket;

import advRocketry.Dimension.*;
import advRocketry.Particles.RocketParticle;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3f;

public class RocketController {

    // Rotation Speed: How quickly the rocket can turn its heading towards the target acceleration vector.
    final double maxRotationRate = 0.05;
    EntityRocket rocket;
    Vec3 targetHeading = new Vec3(0, 1, 0);
    double currentThrust;
    Vec3 currentSecondaryThrust;
    boolean isReverseThrust = false; // on space station to allow for breaking

    public RocketController(EntityRocket rocket) {
        this.rocket = rocket;
    }

    public double getCurrentThrust() {
        return currentThrust;
    }

    public void tick() {
        tickController();
        tickRotation();
        makeThrustParticles();
    }

    public void tickRotation() {
        // rotate heading first
        // Slowly interpolate the rocket's current 'heading' vector towards the 'targetHeading'.
        // This simulates the actual rotational speed limit of the rocket.
        double rotationRate = maxRotationRate * rocket.getRotationRateMultiplier();
        Vec3 rotationCorrection;
        if (targetHeading.dot(rocket.heading) > -0.99) {
            rotationCorrection = targetHeading.subtract(rocket.heading).scale(rotationRate); //  scale makes it more smooth so i like to keep it
            if (rotationCorrection.length() > rotationRate)
                rotationCorrection = rotationCorrection.normalize().scale(rotationRate);
        } else
            rotationCorrection = rocket.front.subtract(rocket.heading).normalize().scale(rotationRate);

        rocket.heading = rocket.heading.add(rotationCorrection).normalize();

        // now try to get the front align more toward the target front
        // the front is not always valid because of the heading
        Vec3 targetFrontValid = rocket.heading.cross(rocket.getTargetFront().cross(rocket.heading)).normalize();
        if (targetFrontValid.dot(rocket.front) < -0.9) // get some movement if it is directly on the other side
            targetFrontValid = rocket.heading.cross(rocket.front);
        rotationCorrection = targetFrontValid.subtract(rocket.front).scale(rotationRate * 0.5f);
        Vec3 newFront = rocket.front.add(rotationCorrection).normalize();
        // make sure the front is 100% always orthogonal, just for extra security
        Vec3 right = rocket.heading.cross(newFront).normalize();
        rocket.front = right.cross(rocket.heading).normalize();
    }

    // pd controller mostly written by gemini
    public void tickController() {

        if (rocket.getTargetPosition() == null) {
            targetHeading = rocket.getDefaultTargetHeading();
            currentThrust = 0;
            currentSecondaryThrust = new Vec3(0, 0, 0);
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
        Vec3 antiGravityAcceleration = new Vec3(0, 1, 0).scale(rocket.getGravity());
        desiredAcceleration = desiredAcceleration.add(antiGravityAcceleration);


        // --- 2. Calculate Thrust & Heading ---
        if (rocket.canUseSecondaryEngines()) {
            // use secondary thrusters for fine control
            Vec3 secondaryThrustersForce = desiredAcceleration.scale(rocket.getMass());
            if (secondaryThrustersForce.length() > SECONDARY_THRUSTERS_FORCE) {
                secondaryThrustersForce = secondaryThrustersForce.normalize().scale(SECONDARY_THRUSTERS_FORCE);
            }
            Vec3 secondaryThrustersAcceleration = secondaryThrustersForce.scale(1 / rocket.getMass());
            desiredAcceleration.subtract(secondaryThrustersAcceleration);
            rocket.setDeltaMovement(rocket.getDeltaMovement().add(secondaryThrustersAcceleration));

            currentSecondaryThrust = secondaryThrustersForce;

            // burn a fixed fuel amount if secondary engines are on for space navigation
            double fuelToBurn = (double) rocket.getFuelRateMax() / 1000;
            int fuelToBurnInt = (int) fuelToBurn;
            if (fuelToBurnInt == 0 && fuelToBurn > 0) {
                fuelToBurnInt = Math.random() < fuelToBurn ? 1 : 0;
            }
            rocket.fuelTank.drain(fuelToBurnInt, IFluidHandler.FluidAction.EXECUTE);
        }

        // Determine if we are on a planet to apply gravity/tilt rules
        Dimension rocketDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(rocket.level().dimension().location());
        boolean isPlanet = rocketDim instanceof PlanetDimension;
        boolean isSpaceDim = rocketDim instanceof SpaceStationDimension;

        if (isPlanet) {
            // never thrust down
            desiredAcceleration = new Vec3(desiredAcceleration.x, Math.max(0, desiredAcceleration.y), desiredAcceleration.z);
        }

        if (desiredAcceleration.length() > 0.0001 && rocket.canUseMainEngines()) {
            // 1. Max Acceleration the engine can *possibly* deliver. ( including scale for bootup time )
            final double MAX_PHYSICAL_ACCEL = rocket.getThrustMax() / rocket.getMass() * getBootTimeThrustMultiplier(rocket);
            // 2. The absolute maximum acceleration we are allowed to use this frame.
            // This ensures we never break the rocket (MAX_STRUCTURAL_ACCEL) AND never demand more thrust than the engine can provide (MAX_PHYSICAL_ACCEL).
            final double MAX_ALLOWED_ACCEL = Math.min(MAX_PHYSICAL_ACCEL, MAX_STRUCTURAL_ACCEL);
            // The heading the rocket *needs* to point towards to achieve the desired acceleration.

            double requiredY = 0;
            // --- TILT LIMITING LOGIC (Prioritize Y-axis thrust on planets) ---
            if (isPlanet && MAX_ALLOWED_ACCEL > 0 && rocket.position().y < 2000) {
                // 1. Determine the vertical acceleration we need (capped by our absolute max engine limit)
                requiredY = Math.min(desiredAcceleration.y, MAX_ALLOWED_ACCEL);
                // Limit to anti-gravity + 1% to hover/climb slowly
                requiredY = Math.min(requiredY, antiGravityAcceleration.y * 1.01);

                // 2. Calculate the remaining acceleration budget for the XZ plane (a^2 + b^2 = c^2)
                double maxXZ_sq = (MAX_ALLOWED_ACCEL * MAX_ALLOWED_ACCEL) - (requiredY * requiredY);
                double maxXZ = maxXZ_sq > 0 ? Math.sqrt(maxXZ_sq) : 0;

                // 3. Calculate how much horizontal acceleration the PD controller is asking for
                double currentXZ = Math.sqrt(desiredAcceleration.x * desiredAcceleration.x + desiredAcceleration.z * desiredAcceleration.z);

                // 4. If the PD controller wants more horizontal movement than our remaining budget, scale the XZ axes down
                if (currentXZ > maxXZ) {
                    double scaleXZ = maxXZ / currentXZ;
                    desiredAcceleration = new Vec3(desiredAcceleration.x * scaleXZ, requiredY, desiredAcceleration.z * scaleXZ);
                }
            }
            // -----------------------------------------------------------------
            if (isSpaceDim && rocket.heading.dot(desiredAcceleration) < 0) {
                // use reverse thrust
                targetHeading = desiredAcceleration.normalize().scale(-1);
                isReverseThrust = true;
            } else {
                // normal rotation toward desired acceleration vector
                targetHeading = desiredAcceleration.normalize();
                isReverseThrust = false;
            }

            // Calculate the magnitude of acceleration needed from the PD controller.
            double accelerationMagnitude = desiredAcceleration.length();

            // This ensures we only thrust if we point towards the target direction.
            double dotMultiplier = Math.max(0, rocket.heading.dot(targetHeading) - 0.9) * 10;
            accelerationMagnitude = accelerationMagnitude * dotMultiplier;

            // The Failsafe: Override the magnitude if we need to fight gravity while rotating
            if (isPlanet && rocket.position().y < 2000) {
                // Only apply the failsafe if we are pointing UP.
                // If we are pointing down or flat, thrusting won't help us fight gravity!
                if (rocket.heading.y > 0) {
                    // Calculate the total magnitude needed along our CURRENT heading to get 'requiredY' lift
                    double magnitudeForHover = requiredY / rocket.heading.y;

                    // Use the hover magnitude if it's higher than our dot-scaled magnitude
                    accelerationMagnitude = Math.max(accelerationMagnitude, magnitudeForHover);
                } else {
                    // If pointing downwards/horizontally, cut thrust entirely so we don't accelerate into the ground
                    // Just let gravity pull us while the tickRotation() method tries to point us back up.
                    accelerationMagnitude = 0;
                }
            }

            // Cap the needed acceleration by the final allowed limit.
            accelerationMagnitude = Math.min(accelerationMagnitude, MAX_ALLOWED_ACCEL);

            // Thrust is applied along the current 'heading' direction.
            // We use the 'actualThrustAccel' determined by the PD control and the rotation limit.
            Vec3 thrustVector = rocket.heading.scale(accelerationMagnitude);
            if (isReverseThrust)
                thrustVector = thrustVector.scale(-1);

            rocket.setDeltaMovement(rocket.getDeltaMovement().add(thrustVector));

            // Calculate the Thrust Multiplier (0.0 to 1.0)
            // This is the current actually delivered thrust relative to the max possible thrust for rendering and fuel consumption
            double ThrustMultiplier = (accelerationMagnitude * rocket.getMass()) / rocket.getThrustMax();
            currentThrust = ThrustMultiplier;

            float toBurn = (float) ((float) rocket.getFuelRateMax() * ThrustMultiplier);
            int toBurnInt = (int) toBurn;
            if (toBurnInt == 0 && toBurn > 0 && Math.random() < toBurn)
                toBurnInt = 1;
            if (!rocket.level().isClientSide) {
                rocket.fuelTank.drain(toBurnInt, IFluidHandler.FluidAction.EXECUTE);
            }

        } else {
            targetHeading = rocket.getDefaultTargetHeading();
            isReverseThrust = false;
        }
    }


    public float getBootTimeThrustMultiplier(EntityRocket rocket) {
        int halfBootTime = EntityRocket.ENGINE_BOOT_TIME / 2;
        if (rocket.getMainEnginesBootUp() < halfBootTime) return 0;
        return (float) Math.pow((float) (rocket.getMainEnginesBootUp() - halfBootTime) / halfBootTime, 2);
    }


    public void makeThrustParticles() {

        if (rocket.level().isClientSide) {
            if (rocket.getMainEnginesBootUp() != 0) {
                float relativeBootTimeLin = (float) rocket.getMainEnginesBootUp() / EntityRocket.ENGINE_BOOT_TIME;
                float bootupParticleProb = (float) Math.sqrt(relativeBootTimeLin);
                int maxParticlesPerTick = 5;
                int maxParticlePerEngine = 2;

                double particleSpawnProb = (double) maxParticlesPerTick / (rocket.getEnginePositions().size() * maxParticlePerEngine);
                if (particleSpawnProb > 1)
                    particleSpawnProb = 1;

                double tooManyEnginesMultiplier = 1.0 / particleSpawnProb;

                double thrustMultiplier = (currentThrust * 0.7 + 0.3);

                for (BlockPos i : rocket.getEnginePositions()) {
                    Vec3 worldPos = RotationUtils.localToWorld(rocket, new Vec3(i.getX() + 0.5, i.getY() + 0.02, i.getZ() + 0.5));
                    for (int j = 0; j < maxParticlePerEngine; j++) {

                        if (relativeBootTimeLin < 0.99) {
                            if (Math.random() > bootupParticleProb) {
                                continue;
                            }
                        }

                        // not spawn too many particles. if we have too many, increase particle size / speed and not spawn many new
                        if (particleSpawnProb < 1) {
                            if (Math.random() > particleSpawnProb) {
                                continue;
                            }
                        }


                        double speedMultiplier;
                        float sizeMultiplier;

                        speedMultiplier = -1 * thrustMultiplier * relativeBootTimeLin * Math.pow(tooManyEnginesMultiplier, 0.4) * (1 + j * 0.1f);

                        sizeMultiplier = (float) (thrustMultiplier * Math.pow(tooManyEnginesMultiplier, 0.3) * relativeBootTimeLin);

                        double reverseThrustMultiplier = isReverseThrust ? -1 : 1;
                        double reverseThrustInducedParticleSpread = isReverseThrust ? 5 : 1;

                        if (!rocket.level().dimension().location().equals(RocketTravelDimension.dimId)) {
                            // no smoke in space travel, looks very bad...
                            new RocketParticle(
                                    (ClientLevel) rocket.level(),
                                    worldPos.x + (Math.random() - 0.5) * 0.5,
                                    worldPos.y + (Math.random() - 0.5) * 0.5,
                                    worldPos.z + (Math.random() - 0.5) * 0.5,
                                    rocket.heading.x * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.2 * speedMultiplier * reverseThrustInducedParticleSpread,
                                    rocket.heading.y * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.2 * speedMultiplier * reverseThrustInducedParticleSpread,
                                    rocket.heading.z * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.2 * speedMultiplier * reverseThrustInducedParticleSpread,
                                    new Vector3f(0.5f, 0.5f, 0.5f).mul(1f),
                                    0.2f,
                                    sizeMultiplier * 2,
                                    500,
                                    false
                            );
                        }

                        sizeMultiplier = (float) (thrustMultiplier * Math.pow(tooManyEnginesMultiplier, 0.8) * relativeBootTimeLin);

                        for (int p = 0; p < 2; p++) {
                            new RocketParticle(
                                    (ClientLevel) rocket.level(),
                                    worldPos.x + (Math.random() - 0.5) * 0.5,
                                    worldPos.y + (Math.random() - 0.5) * 0.5,
                                    worldPos.z + (Math.random() - 0.5) * 0.5,
                                    rocket.heading.x * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.1 * speedMultiplier * reverseThrustInducedParticleSpread,
                                    rocket.heading.y * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.1 * speedMultiplier * reverseThrustInducedParticleSpread,
                                    rocket.heading.z * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.1 * speedMultiplier * reverseThrustInducedParticleSpread,
                                    new Vector3f(0.5f, 0.8f, 1.0f).mul(1f),
                                    // we not use additive blending in fabulous because it doesnt work so make it more bright
                                    (Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FABULOUS) ? 1 : 0.1f,
                                    sizeMultiplier * 0.5f,
                                    20,
                                    true
                            );
                        }

                    }
                }
                //System.out.println(particles);
                //System.out.println(currentThrust);
            }
        }
    }
}
