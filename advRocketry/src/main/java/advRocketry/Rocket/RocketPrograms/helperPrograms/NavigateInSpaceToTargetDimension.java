package advRocketry.Rocket.RocketPrograms.helperPrograms;

import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.RocketTravelDimension;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Utils.CelestialUtils;
import advRocketry.Utils.SpaceNavigation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

// just a helper program, is not actually a real full program
public class NavigateInSpaceToTargetDimension {

    // copied from rocketcontroller
    public static void tickUniverseRotation(EntityRocket rocket) {

        Vec3 rotationCorrection;
        if (rocket.universeTargetHeading.dot(rocket.universeHeading) > -0.99)
            rotationCorrection = rocket.universeTargetHeading.subtract(rocket.universeHeading).scale(Config.INSTANCE.rocket_SpaceTravel_Rotation_Rate);
        else
            rotationCorrection = rocket.universeFront.subtract(rocket.universeHeading).scale(Config.INSTANCE.rocket_SpaceTravel_Rotation_Rate);

        rocket.universeHeading = rocket.universeHeading.add(rotationCorrection).normalize();

        Vec3 targetFrontValid = rocket.universeHeading.cross(new Vec3(0, 1, 0).cross(rocket.universeHeading)).normalize();
        if (targetFrontValid.dot(rocket.universeFront) < -0.99) // get some movement if it is directly on the other side
            targetFrontValid = rocket.universeHeading.cross(rocket.universeFront);
        rotationCorrection = targetFrontValid.subtract(rocket.universeFront).scale(Config.INSTANCE.rocket_SpaceTravel_Rotation_Rate);
        Vec3 newFront = rocket.universeFront.add(rotationCorrection).normalize();

        Vec3 right = rocket.universeHeading.cross(newFront).normalize();
        rocket.universeFront = right.cross(rocket.universeHeading).normalize();
    }

    public static boolean run(EntityRocket rocket, ResourceLocation target, @Nullable ResourceLocation origin, Runnable onTargetReached) {

        if (rocket.level().dimension().location().equals(target)) {
            // this should never happen!
            return true;
        }
        if (!rocket.level().dimension().location().equals(RocketTravelDimension.dimId)) {
            // this should never happen!
            return true;
        }

        // all movement is virtual, but enable main engines for particles
        rocket.controller.enableMainEngines(true, false);
        rocket.controller.enableSecondaryEngines(false, false);
        rocket.controller.setTargetPosition(null, false);
        rocket.setDeltaMovement(0, 0, 0);
        rocket.controller.setHeadingAndFrontDirect(new Vec3(0, 0, -1), new Vec3(0, 1, 0)); // default heading for space travel, rocket travel dim expects the rocket to head north for render


        /// So this is a bit hacky because we want to be able to travel to far away planets like venus in reasonable time
        /// but we also do not want it to "teleport" from earth to moon.
        /// So i make it about like this:
        /// we have a target travel speed, but if we are close to origin or target, we use these distances to decrease the travel speed
        /// we use a helper program to navigate around planets in the way
        /// we scale the target speed by dot(heading, target) to slow down when off target


        Dimension targetDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(target);
        if (targetDim == null) return true;

        Vec3 targetPosition = targetDim.getPosition(0);
        Vec3 finalTargetPositionRelative = targetPosition.subtract(rocket.universePosition);
        Vec3 finalTargetDirection = finalTargetPositionRelative.normalize();
        double distanceToFinalTarget = finalTargetPositionRelative.length();

        double entryDistance = Math.max(0.0001, CelestialUtils.toAU((targetDim instanceof PlanetDimension p ? p.getEarthRadiusMultiplier() : 1) * CelestialUtils.EARTH_RADIUS * Config.INSTANCE.planet_Render_Scale_Multiplier * 1.2));

        Vec3 nextTarget = SpaceNavigation.getNextTargetAvoidPlanetCollision(targetPosition, rocket.universePosition, DimensionManager.getDimensionManager(rocket.level().isClientSide), targetDim instanceof PlanetDimension p ? p : null);
        if (targetPosition == nextTarget) { // the function will return the exact same input vector so on == the next target is final target
            if(false) { // disabled because i dont like it!
                // do not fly directly in center, fly to the edge
                if (targetDim instanceof PlanetDimension planetDimension) {
                    // find a possible edge direction
                    Vec3 side = planetDimension.getOrbitAxis().normalize().cross(finalTargetDirection);
                    // offset the target to fly to the edge of the planet
                    nextTarget = nextTarget.add(side.normalize().scale(entryDistance));
                    // this looks like it is risky to target the entry distance when we require it to get < entry distance to teleport,
                    // but as we get closer the side vector will shift behind the planet and it will surely get within tp distance
                }
            }
        }
        Vec3 nextTargetPositionRelative = nextTarget.subtract(rocket.universePosition);
        Vec3 nextTargetDirection = nextTargetPositionRelative.normalize();

        Dimension originDim = null;
        Vec3 originPos = null;
        double distanceToOrigin = -1;
        double entryDistanceOrigin = -1;
        if (origin != null && !origin.equals(RocketTravelDimension.dimId)) {
            originDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(origin);
        }
        if (originDim != null) {
            originPos = originDim.getPosition(0);
            distanceToOrigin = originPos.subtract(rocket.universePosition).length();
            entryDistanceOrigin = Math.max(0.0001, CelestialUtils.toAU((originDim instanceof PlanetDimension p ? p.getEarthRadiusMultiplier() : 1) * CelestialUtils.EARTH_RADIUS * Config.INSTANCE.planet_Render_Scale_Multiplier * 1.2));
        }


        rocket.universeTargetHeading = nextTargetDirection;
        tickUniverseRotation(rocket);

        // move forward
        double maxSpeed = Config.INSTANCE.rocket_SpaceTravel_AU_Per_Second / 20; // the maximum travel speed defined as in config adjusted for per tick
        double distanceForMaxSpeed = Config.INSTANCE.rocket_SpaceTravel_Distance_For_Max_Speed; // we reach this speed only if we are 0.1 AU away from home / origin

        double nearTargetMultiplier = Math.min(1, Math.max(0, distanceToFinalTarget - entryDistance) / distanceForMaxSpeed);
        maxSpeed *= nearTargetMultiplier; // slow down when near target
        if (originDim != null) {
            double nearOriginMultiplier = Math.min(1, Math.max(0, distanceToOrigin - entryDistanceOrigin) / distanceForMaxSpeed);
            maxSpeed *= nearOriginMultiplier; // slow down near origin
        }
        // base speed
        double e = Config.INSTANCE.rocket_SpaceTravel_Min_Speed;
        double offTargetMultiplier = Math.max(0, finalTargetDirection.dot(rocket.universeHeading) - 0.9) * 10;

        // the origin/target can move in space too, so i will add the origin/target dimension speed to the final target speed in case e is too small to catch up with planet movement
        // check if close to target
        Vec3 targetMovement = targetDim.getMovement();
        double targetDimensionMovementConsideration = 0;
        if (targetMovement.length() > 0) {
            targetDimensionMovementConsideration = getSpeedAdjustment(
                    entryDistance * 10,
                    entryDistance * 8,
                    distanceToFinalTarget,
                    targetMovement,
                    rocket
            );
            //System.out.println("extra speed for target: " + targetDimensionMovementConsideration);
        }
        // check if close to origin
        if (originDim != null) {
            Vec3 originMovement = originDim.getMovement();
            if (originMovement.length() > 0 && distanceToOrigin < distanceToFinalTarget) {
                targetDimensionMovementConsideration = getSpeedAdjustment(
                        entryDistanceOrigin * 10,
                        entryDistanceOrigin * 8,
                        distanceToOrigin,
                        originMovement,
                        rocket
                );
                //System.out.println("extra speed for origin: " + targetDimensionMovementConsideration);
            }
        }


        // calculate acceleration
        double targetSpeed = maxSpeed * offTargetMultiplier + e + targetDimensionMovementConsideration;

        rocket.universeTravelSpeed = targetSpeed;

        rocket.universePosition = rocket.universePosition.add(rocket.universeHeading.scale(rocket.universeTravelSpeed));

        if (rocket.universePosition.distanceTo(targetPosition) < entryDistance) {
            onTargetReached.run();
        }

        if (originPos != null && !rocket.level().isClientSide) {
            double maxDistance = originPos.distanceTo(targetPosition);
            double progress = 1 - (distanceToFinalTarget / maxDistance);
            double progressPercent = (double) Math.round(progress * 100 * 100) / 100;
            rocket.infoText.setTextAndSync("progress: " + progressPercent + "%");
            rocket.temporaryInfoTimeout = 10;
        }

        return false;
    }

    private static double getSpeedAdjustment(double planetMovementConsiderationStartDistance, double planetMovementConsiderationEndDistance, double distanceToTarget, Vec3 targetMovement, EntityRocket rocket) {
        double vectorscale = Math.pow(10, 8); // scale the vector up before normalize to avoid numerical errors because movement in AU is small;
        //  planetMovementConsiderationStartDistance   f(x) = 0, at this distance we start to consider target dimension movement
        //  planetMovementConsiderationEndDistance     f(x) = 1, at this distance we fully consider target dimension movement
        double planetMovementConsiderationSlope = -1 / (planetMovementConsiderationStartDistance - planetMovementConsiderationEndDistance); // (dy / dx)
        double planetMovementConsiderationOffset = -(planetMovementConsiderationSlope * planetMovementConsiderationStartDistance); // f(StartDistance * slope + offset) = 0 -> offset = -StartDistance * slope
        double planetMovementConsideration = planetMovementConsiderationSlope * distanceToTarget + planetMovementConsiderationOffset; // run the function
        planetMovementConsideration = Math.clamp(planetMovementConsideration, 0, 1); // ensure it is  between 0 and 1
        planetMovementConsideration *= targetMovement.scale(vectorscale).normalize().dot(rocket.universeHeading); // only care about using the movement when it aligns with our heading to slow down or speed up
        return targetMovement.length() * planetMovementConsideration / vectorscale; // the final scaled value to add, can be negative if the planet moves toward us
    }
}
