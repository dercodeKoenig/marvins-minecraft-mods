package advRocketry.Rocket.RocketPrograms;

import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Rocket.EntityRocket;
import advRocketry.utils.CelestialUtils;
import advRocketry.utils.SpaceNavigation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

// just a helper program, is not actually a real full program
public class NavigateInSpaceToTargetDimension {

    // copied from rocketcontroller
    public static void tickUniverseRotation(EntityRocket rocket) {

        Vec3 rotationCorrection;
        if (rocket.universeTargetHeading.dot(rocket.universeHeading) > -0.99) {
            rotationCorrection = rocket.universeTargetHeading.subtract(rocket.universeHeading).scale(Config.INSTANCE.rocket_SpaceTravel_Rotation_Rate);
            ; // scale makes it more smooth
            if (rotationCorrection.length() > Config.INSTANCE.rocket_SpaceTravel_Rotation_Rate)
                rotationCorrection = rotationCorrection.normalize().scale(Config.INSTANCE.rocket_SpaceTravel_Rotation_Rate);
        } else
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

    public static boolean run(EntityRocket rocket, ResourceLocation target, @Nullable ResourceLocation origin) {

        if (rocket.level().dimension().location().equals(target)) {
            return true;
        }

        // all movement is virtual
        rocket.enableMainEngines(true, false);
        rocket.enableSecondaryEngines(false, false);
        rocket.setTargetPosition(null, false);
        rocket.setDeltaMovement(0, 0, 0);
        rocket.setHeadingAndFrontDirect(new Vec3(0, 0, -1), new Vec3(0, 1, 0)); // default heading for space travel, rocket travel dim expects the rocket to head north for render


        /// So this is a bit hacky because we want to be able to travel to far away planets like venus in reasonable time
        /// but we also do not want it to "teleport" from earth to moon.
        /// So i make it about like this:
        /// we have a target travel speed, but if we are close to origin or target, we use these distances to decrease the travel speed
        /// we use a helper program to navigate around planets in the way
        /// we scale the target speed by dot(heading, target) to slow down when off target


        // move to target dimension
        Dimension targetDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(target);
        if (targetDim == null) return true; // client might not have received the dimension sync packet
        Vec3 targetPosition = targetDim.getPosition(0);
        Vec3 finalTargetPositionRelative = targetPosition.subtract(rocket.universePosition);
        Vec3 finalTargetDirection = finalTargetPositionRelative.normalize();
        double distanceToFinalTarget = finalTargetPositionRelative.length();

        Vec3 nextTarget = SpaceNavigation.getNextTargetAvoidPlanetCollision(targetPosition, rocket.universePosition, DimensionManager.getDimensionManager(rocket.level().isClientSide), targetDim instanceof PlanetDimension p ? p : null);
        Vec3 nextTargetPositionRelative = nextTarget.subtract(rocket.universePosition);
        Vec3 nextTargetDirectiop = nextTargetPositionRelative.normalize();

        Dimension originDim = null;
        Vec3 originPos;
        double distanceToOrigin = -1;
        double entryDistanceOrigin = -1;
        if (origin != null) {
            originDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(origin);
            originPos = originDim.getPosition(0);
            distanceToOrigin = originPos.subtract(rocket.universePosition).length();
            entryDistanceOrigin = Math.max(0.0001, CelestialUtils.toAU((originDim instanceof PlanetDimension p ? p.getEarthRadiusMultiplier() : 1) * CelestialUtils.EARTH_RADIUS * Config.INSTANCE.planet_Render_Scale_Multiplier * 1.2));
        }


        rocket.universeTargetHeading = nextTargetDirectiop;
        tickUniverseRotation(rocket);

        double entryDistance = Math.max(0.0001, CelestialUtils.toAU((targetDim instanceof PlanetDimension p ? p.getEarthRadiusMultiplier() : 1) * CelestialUtils.EARTH_RADIUS * Config.INSTANCE.planet_Render_Scale_Multiplier * 1.2));

        // move forward
        double maxSpeed = (double) 1 / 20 / 60; // so this should mean in 60s we move 1 AU, 60(s) * 20(tps) * 1(au)
        double distanceForMaxSpeed = 0.1; // we reach this speed only if we are 0.1 AU away from home / origin

        double nearTargetMultiplier = Math.min(1, Math.max(0,distanceToFinalTarget-entryDistance) / distanceForMaxSpeed);
        maxSpeed *= nearTargetMultiplier; // slow down when near target
        System.out.println("near target: "+nearTargetMultiplier);
        if (originDim != null) {
            // do not go too fast initially, move slowly away first before crazy acceleration
            // slowly enable the target speed as we move away from origin
            double nearOriginMultiplier = Math.min(1, Math.max(0,distanceToOrigin-entryDistanceOrigin) / distanceForMaxSpeed);
            maxSpeed *= nearOriginMultiplier;
            System.out.println("near origin: "+nearOriginMultiplier);
        }
        // base speed
        double e = 0.000005;
        double offTargetMultiplier = Math.max(0, finalTargetDirection.dot(rocket.universeHeading) - 0.9) * 10;

        // the origin/target can move in space too, so i will add the origin/target dimension speed to the final target speed in case e is too small to catch up with planet movement
        // check if close to target
        Vec3 targetMovement = targetDim.getMovement(0);
        double targetDimensionMovementConsideration = 0;
        if (targetMovement.length() > 0) {
            targetDimensionMovementConsideration = getSpeedAdjustment(
                    entryDistance * 5,
                    entryDistance * 3,
                    distanceToFinalTarget,
                    targetMovement,
                    rocket
            );
            //System.out.println("extra speed for target: " + targetDimensionMovementConsideration);
        }
        // check if close to origin
        if (originDim != null) {
            Vec3 originMovement = originDim.getMovement(0);
            if (originMovement.length() > 0 && distanceToOrigin < distanceToFinalTarget) {
                targetDimensionMovementConsideration = getSpeedAdjustment(
                        entryDistanceOrigin * 5,
                        entryDistanceOrigin * 3,
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

        if (rocket.level() instanceof ServerLevel serverLevel && rocket.universePosition.distanceTo(targetPosition) < entryDistance) {
            // TODO: ifrocket.hasSatellites && shouldDeployThem -> deploy satellites shortly before dimension jump

            // get the teleportation target
            ServerLevel targetLevel = DimensionManager.getServerLevel(serverLevel.getServer(), target);
            Vec3 targetPos = new Vec3(rocket.getLastLaunchPosition().getX(), Config.INSTANCE.planet_Sky_Height, rocket.getLastLaunchPosition().getZ());

            Vec3 entrySpeed = new Vec3(
                    (Math.random() * 2 - 1) * 0.3,
                    Config.INSTANCE.rocket_Planet_Entry_Speed_Y,
                    (Math.random() * 2 - 1) * 0.3);

            EntityRocket newRocket = rocket.teleportTo(targetLevel, targetPos, entrySpeed);
            System.out.println("dx:" + newRocket.getDeltaMovement());
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
