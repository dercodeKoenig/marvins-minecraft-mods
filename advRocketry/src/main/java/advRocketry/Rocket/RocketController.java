package advRocketry.Rocket;

import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.Render.Particles.RocketParticle;
import advRocketry.Utils.Utils;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Objects;

public class RocketController {

    // the parent rocket
    EntityRocket rocket;
    double currentThrust = 0;
    double currentAcceleration = 0;
    double currentAirDrag = 0;
    boolean isReverseThrust = false; // on space station to allow for breaking
    Vec3 currentSecondaryThrustForce;

    // the target for the rocket to move towards
    Vec3 targetPosition = null;
    // enable in space for breaking and fine steering, can thrust in any direction without need for rotation
    boolean canUseSecondaryEngines = true;
    // main engines allowed?
    boolean canUseMainEngines = false;
    int mainEnginesBootup = 0;

    // modifier on how fast the rocket can change heading
    double rotationRateMultiplier = 1;
    // the heading that the controller calculates for correct thrust or set to default heading
    Vec3 targetHeading = new Vec3(0, 1, 0);
    // the current heading, should only be accessed in tickRotation and during save & load
    Vec3 heading = new Vec3(0, 1, 0);
    // the default heading when it does not need to rotate for main engine use
    Vec3 defaultTargetHeading = new Vec3(0, 1, 0);
    // the target front, it should rotate around heading to get closer to it
    Vec3 targetFront = new Vec3(0, 0, 1);
    // the current front, should only be accessed in tickRotation and during save & load
    Vec3 front = new Vec3(0, 0, 1);

    // for smooth render, add rotationRate so it can correctly use partial tick
    Vec3 headingRotationRate = Vec3.ZERO;
    Vec3 frontRotationRate = Vec3.ZERO;

    // for more smooth rotation, lazy heading and front are used
    Vec3 lazyHeading = heading;
    Vec3 lazyFront = front;

    // when the heading changes we might need to update the bounding box
    Direction.Axis lastBBAxis = null;

    public RocketController(EntityRocket rocket) {
        this.rocket = rocket;
    }

    Level level() {
        return rocket.level();
    }

    void sendToClients(CompoundTag tag) {
        rocket.sendToClients(tag);
    }


    public double getCurrentThrust() {
        return currentThrust;
    }
    public double getCurrentGForce() {
        return currentAcceleration / EntityRocket.G;
    }
    public double getCurrentAirDrag() {
        return currentAirDrag;
    }

    public void setHeadingAndFrontDirect(Vec3 heading, Vec3 front) {
        this.defaultTargetHeading = heading;
        this.heading = heading;
        this.lazyHeading = heading;
        this.targetFront = front;
        this.front = front;
        this.lazyFront = front;
    }

    public void setRotationRateMultiplier(double multiplier, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && this.rotationRateMultiplier != multiplier) {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("rotationRateMultiplier", multiplier);
            sendToClients(tag);
        }
        this.rotationRateMultiplier = multiplier;
    }

    public double getRotationRateMultiplier() {
        return rotationRateMultiplier;
    }

    public void enableMainEngines(boolean canUseMainEngines, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && this.canUseMainEngines != canUseMainEngines) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("canUseMainEngines", canUseMainEngines);
            tag.putInt("mainEnginesBootup", mainEnginesBootup);
            sendToClients(tag);
        }
        this.canUseMainEngines = canUseMainEngines;
    }

    public boolean canUseMainEngines() {
        return canUseMainEngines;
    }

    public void setMainEnginesBootup(int bootup, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && this.mainEnginesBootup != bootup) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("mainEnginesBootup", bootup);
            sendToClients(tag);
        }
        this.mainEnginesBootup = bootup;
    }

    public int getMainEnginesBootUp() {
        return this.mainEnginesBootup;
    }

    public void enableSecondaryEngines(boolean canUseSecondaryEngines, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && this.canUseSecondaryEngines != canUseSecondaryEngines) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("secondaryEngines", canUseSecondaryEngines);
            sendToClients(tag);
        }
        this.canUseSecondaryEngines = canUseSecondaryEngines;
    }

    public boolean canUseSecondaryEngines() {
        return canUseSecondaryEngines;
    }

    public void setDefaultTargetHeading(Vec3 target, boolean syncToClient) {
        target = target.normalize();
        if (!level().isClientSide && syncToClient && !Objects.equals(target, defaultTargetHeading)) {
            CompoundTag tag = new CompoundTag();
            tag.put("defaultTargetHeading", Utils.serializeVec3(target));
            sendToClients(tag);
        }
        defaultTargetHeading = target;
    }

    public Vec3 getDefaultTargetHeading() {
        return defaultTargetHeading;
    }

    public Vec3 getHeading() {
        return lazyHeading;
    }

    public void setTargetFront(Vec3 target, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && !Objects.equals(target, targetFront)) {
            CompoundTag tag = new CompoundTag();
            tag.put("targetFront", Utils.serializeVec3(target));
            sendToClients(tag);
        }
        targetFront = target;
    }

    public Vec3 getTargetFront() {
        return targetFront;
    }

    public Vec3 getFront() {
        return lazyFront;
    }

    public void setTargetPosition(@Nullable Vec3 target, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && !Objects.equals(target, targetPosition)) {
            CompoundTag tag = new CompoundTag();
            tag.put("targetPosition", Utils.serializeVec3(target));
            sendToClients(tag);
        }
        targetPosition = target;
    }

    public Vec3 getTargetPosition() {
        return targetPosition;
    }

    public void tick() {
        tickController();
        tickRotation();
        makeThrustParticles();


        // tick engine bootup / shutdown
        if (canUseMainEngines) {
            if (mainEnginesBootup < Config.INSTANCE.rocket_Engine_Boot_Ticks) {
                mainEnginesBootup++;
            }
        } else {
            if (mainEnginesBootup > 0) {
                mainEnginesBootup--;
            }
        }


        // simulate air friction
        if (DimensionManager.getDimensionManager(level().isClientSide).get(level().dimension().location()) instanceof PlanetDimension planet) {
            Vec3 movement = rocket.getDeltaMovement();
            double speed = movement.length();
            if (speed > 0.01) {
                double k = 0.01;
                double atmDensity = rocket.getAtmDensityAtCurrentHeight();
                double airBreakForce = rocket.size.getX() * rocket.size.getZ() * atmDensity * speed * speed * k;
                double airAcceleration = airBreakForce / rocket.getMass();
                currentAirDrag = airBreakForce;
                Vec3 breakMovement = movement.normalize().scale(-1 * airAcceleration);
                if (breakMovement.length() >= speed)
                    rocket.setDeltaMovement(0, 0, 0);
                else
                    rocket.setDeltaMovement(movement.add(breakMovement));
            }
        }
    }

    public void tickRotation() {
        // rotate heading first
        // Slowly interpolate the rocket's current 'heading' vector towards the 'targetHeading'.
        double maxRotationRate = 0.05f;
        double rotationRateHeading = maxRotationRate * getRotationRateMultiplier();
        Vec3 rotationCorrection;
        double dot = targetHeading.dot(heading);
        if (dot > -0.95) {
            if (dot < 0.4) {
                // TARGET IS BEHIND/SIDE: Use orthogonal vector
                // This prevents the "slow-down" seen when the target is almost 180 degrees away
                rotationCorrection = heading.cross(targetHeading).cross(heading).normalize();
            } else {
                // TARGET IS IN FRONT: Use simple subtraction
                // As targetHeading.subtract(heading) gets smaller, the rotation naturally slows
                rotationCorrection = targetHeading.subtract(heading);
            }
            if (rotationCorrection.length() > rotationRateHeading)
                rotationCorrection = rotationCorrection.normalize().scale(rotationRateHeading);
        } else
            rotationCorrection = front.subtract(heading).normalize().scale(rotationRateHeading);

        heading = heading.add(rotationCorrection).normalize();

        // now try to get the front align more toward the target front
        // 1: calculate the valid front from the target front so that it is orthogonal to heading
        // 2: interpolate front towards valid target front
        // 3: make sure new front is orthogonal and normalized
        Vec3 targetFrontValid = heading.cross(getTargetFront().cross(heading)).normalize();
        if (targetFrontValid.dot(front) < -0.95) // get some movement if it is directly on the other side
            targetFrontValid = heading.cross(front);
        rotationCorrection = targetFrontValid.subtract(front).scale(maxRotationRate * 0.5f);
        Vec3 newFront = front.add(rotationCorrection).normalize();
        // make sure the front is 100% always orthogonal, just for extra security
        Vec3 right = heading.cross(newFront).normalize();
        front = right.cross(heading).normalize();

        // tick the lazy heading & front
        // interpolate the lazy values
        double lerpFactor = 0.1;
        rotationCorrection = heading.subtract(lazyHeading).scale(lerpFactor);
        Vec3 newLazyHeading = lazyHeading.add(rotationCorrection).normalize();
        headingRotationRate = newLazyHeading.subtract(lazyHeading);
        lazyHeading = newLazyHeading;
        rotationCorrection = front.subtract(lazyFront).scale(lerpFactor);
        Vec3 lazyFrontInvalid = lazyFront.add(rotationCorrection);
        Vec3 newLazyFront = lazyHeading.cross(lazyFrontInvalid.cross(lazyHeading)).normalize();
        frontRotationRate = newLazyFront.subtract(lazyFront);
        lazyFront = newLazyFront;

        // update bounding box after rotation
        Direction.Axis newHeadingAxis = rocket.findClosestAxis(getHeading());
        if (newHeadingAxis != lastBBAxis) {
            rocket.setBoundingBox(rocket.makeBoundingBox(newHeadingAxis));
            lastBBAxis = newHeadingAxis;
        }

    }

    // pd controller mostly written by gemini
    public void tickController() {

        if (getTargetPosition() == null) {
            targetHeading = getDefaultTargetHeading();
            currentThrust = 0;
            currentSecondaryThrustForce = new Vec3(0, 0, 0);
            return;
        }

        Dimension rocketDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(rocket.level().dimension().location());

        // --- Configuration Parameters (Tune these for desired behavior) ---
        // Proportional Gain: How aggressively the rocket tries to close the distance.
        final double K_P = 0.001;
        // Damping Gain (Derivative-like): How aggressively the rocket slows down to prevent overshoot.
        double K_D = Math.sqrt(K_P) * 2 * 1.2;
        // Structural/Breakage Limit: This is the maximum acceleration the vehicle can withstand.
        final double MAX_STRUCTURAL_ACCEL = getMaxAcceleration();
        // secondary thruster force
        final double SECONDARY_THRUSTERS_FORCE = rocket.getThrustMax() / 1000;

        // --- 1. Calculate Required Acceleration (The PD Controller) ---

        // B. Position Error (p_target - p)
        Vec3 positionError = getTargetPosition().subtract(rocket.getPosition(0));

        // C. Damping (Velocity Error - using current velocity for simplicity)
        Vec3 currentVelocity = rocket.getDeltaMovement();

        // D. Desired Acceleration (a_desired)
        // Formula: a_desired = (K_P * Position_Error) - (K_D * Current_Velocity)
        // The result is the absolute acceleration vector the rocket *needs* to follow the path.
        Vec3 desiredAcceleration = positionError.scale(K_P).subtract(currentVelocity.scale(K_D));

        // factor in gravity
        Vec3 antiGravityAcceleration = new Vec3(0, 1, 0).scale(rocket.getGravity());
        desiredAcceleration = desiredAcceleration.add(antiGravityAcceleration);

        // --- 2. Calculate Thrust & Heading ---
        if (canUseSecondaryEngines()) {
            // use secondary thrusters for fine control
            Vec3 secondaryThrustersForce = desiredAcceleration.scale(rocket.getMass());
            if (secondaryThrustersForce.length() > SECONDARY_THRUSTERS_FORCE) {
                secondaryThrustersForce = secondaryThrustersForce.normalize().scale(SECONDARY_THRUSTERS_FORCE);
            }
            Vec3 secondaryThrustersAcceleration = secondaryThrustersForce.scale(1 / rocket.getMass());
            desiredAcceleration.subtract(secondaryThrustersAcceleration);
            rocket.setDeltaMovement(rocket.getDeltaMovement().add(secondaryThrustersAcceleration));

            currentSecondaryThrustForce = secondaryThrustersForce;

            // consume some fuel
            float ThrustMultiplier = (float) (currentSecondaryThrustForce.length() / rocket.getThrustMax());
            // secondary engines less efficient and burn double fuel
            float toBurn = (rocket.getFuelRateMax() * ThrustMultiplier * 2);
            int toBurnInt = (int) toBurn;
            if (toBurnInt == 0 && toBurn > 0 && Math.random() < toBurn)
                toBurnInt = 1;
            if (!rocket.level().isClientSide) {
                rocket.fuelTank.drain(toBurnInt, IFluidHandler.FluidAction.EXECUTE);
            }
        }

        // Determine if we are on a planet to apply gravity/tilt rules
        boolean isPlanet = rocketDim instanceof PlanetDimension;
        boolean isSpaceDim = rocketDim instanceof SpaceStationDimension;

        if (isPlanet) {
            // never thrust down
            // always keep some anti-gravity thrust, this should prevent it from too much tilt
            desiredAcceleration = new Vec3(desiredAcceleration.x, Math.max(antiGravityAcceleration.y * 0.05, desiredAcceleration.y), desiredAcceleration.z);
        }

        if (desiredAcceleration.length() > 0.0001 && canUseMainEngines()) {
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
            if (isSpaceDim && getHeading().dot(desiredAcceleration.normalize()) < -0.8) {
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
            double dotMultiplier = Math.max(0, getHeading().dot(targetHeading) - 0.9) * 10;
            accelerationMagnitude = accelerationMagnitude * dotMultiplier;

            // The Failsafe: Override the magnitude if we need to fight gravity while rotating
            if (isPlanet && rocket.position().y < 2000) {
                // Only apply the failsafe if we are pointing UP.
                // If we are pointing down or flat, thrusting won't help us fight gravity!
                if (getHeading().y > 0) {
                    // Calculate the total magnitude needed along our CURRENT heading to get 'requiredY' lift
                    double magnitudeForHover = requiredY / getHeading().y;

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
            currentAcceleration = accelerationMagnitude;

            // Thrust is applied along the current 'heading' direction.
            // We use the 'actualThrustAccel' determined by the PD control and the rotation limit.
            Vec3 thrustVector = getHeading().scale(accelerationMagnitude);
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
            targetHeading = getDefaultTargetHeading();
            isReverseThrust = false;
        }
    }



    public float getMaxAcceleration() {
        // this method usually runs when:
        // the controller ticks and a target position is given
        // when the rocket lands to calculate its target velocity
        // when there is no program running, this method should never run and the ground check should not be a concern
        Dimension myDim = DimensionManager.getDimensionManager(level().isClientSide).get(level().dimension().location());
        if (myDim instanceof SpaceStationDimension) {
            // on space station, lower acceleration for more fine controll
            // usually it should never demand this much but anyway....
            return 0.01f;
        }
        if (myDim instanceof PlanetDimension planet) {
            // lower acc near ground where there is probably more atmosphere and whatever it looks better
            int y = -1 + level().getHeight(Heightmap.Types.WORLD_SURFACE, rocket.blockPosition().getX(), rocket.blockPosition().getZ());
            double MAX_STRUCTURAL_ACC = EntityRocket.G * rocket.maxG;
            double h = rocket.position().y - y;
            double minH = 100;
            double minA = Math.min(MAX_STRUCTURAL_ACC, rocket.getGravity() * 1.03);
            double currentMaxA = minA + (MAX_STRUCTURAL_ACC - minA) * Math.clamp((h - 10) / minH, 0, 1);

            // next: limit by velocity, too fast = too much stress by atmosphere
            // if we go faster than target velocity, reduce acceleration
            double currentAtm = rocket.getAtmDensityAtCurrentHeight();
            double atmMultiplier = 1 - (currentAtm / (1 + currentAtm));
            double targetSpeedPerTick = 10 * atmMultiplier;
            double overspeedAllowance = 5;
            double currentSpeed = rocket.getDeltaMovement().y;
            // current ~ target -> 1
            // current >> target -> -inf (too fast, slow down)
            // current << target -> +inf (too slow or falling, no limit on acc)
            double accelerationModifier = 1 + (targetSpeedPerTick - currentSpeed) / overspeedAllowance;
            accelerationModifier = Math.clamp(accelerationModifier, 0, 1);

            return (float) (currentMaxA * accelerationModifier);
        }
        return 1;
    }



    public float getBootTimeThrustMultiplier(EntityRocket rocket) {
        int halfBootTime = Config.INSTANCE.rocket_Engine_Boot_Ticks / 2;
        if (getMainEnginesBootUp() < halfBootTime) return 0;
        return (float) Math.pow((float) (getMainEnginesBootUp() - halfBootTime) / halfBootTime, 2);
    }


    public void makeThrustParticles() {

        if (rocket.level().isClientSide) {
            if (getMainEnginesBootUp() != 0) {
                float relativeBootTimeLin = (float) getMainEnginesBootUp() / Config.INSTANCE.rocket_Engine_Boot_Ticks;
                float bootupParticleProb = (float) Math.sqrt(relativeBootTimeLin);
                int maxParticlesPerTick = 5;
                int maxParticlePerEngine = 2;

                double particleSpawnProb = (double) maxParticlesPerTick / (rocket.getEnginePositions().size() * maxParticlePerEngine);
                if (particleSpawnProb > 1)
                    particleSpawnProb = 1;

                double tooManyEnginesMultiplier = 1.0 / particleSpawnProb;

                double thrustMultiplier = Math.pow(currentThrust * 0.8 + 0.2, 0.5);

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
                        double reverseThrustInducedParticleSpread = isReverseThrust ? 2 : 1;

                        if (!rocket.level().dimension().location().equals(RocketTravelDimension.dimId)) {
                            // no smoke in space travel, looks very bad...
                            new RocketParticle(
                                    (ClientLevel) rocket.level(),
                                    worldPos.x + (Math.random() - 0.5) * 0.5,
                                    worldPos.y + (Math.random() - 0.5) * 0.5,
                                    worldPos.z + (Math.random() - 0.5) * 0.5,
                                    getHeading().x * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.2 * speedMultiplier * reverseThrustInducedParticleSpread,
                                    getHeading().y * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.2 * speedMultiplier * reverseThrustInducedParticleSpread,
                                    getHeading().z * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.2 * speedMultiplier * reverseThrustInducedParticleSpread,
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
                                    getHeading().x * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.1 * speedMultiplier * reverseThrustInducedParticleSpread,
                                    getHeading().y * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.1 * speedMultiplier * reverseThrustInducedParticleSpread,
                                    getHeading().z * speedMultiplier * reverseThrustMultiplier + (Math.random() - 0.5) * 0.1 * speedMultiplier * reverseThrustInducedParticleSpread,
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
