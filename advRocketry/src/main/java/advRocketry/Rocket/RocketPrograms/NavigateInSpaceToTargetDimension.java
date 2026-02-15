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

// just a helper program, is not actually a real full program
public class NavigateInSpaceToTargetDimension {

    // copied from rocketcontroller
    public static void tickUniverseRotation(EntityRocket rocket) {

        // todo shouldnt this be normalized before scale?
        Vec3 rotationCorrection;
        if (rocket.universeTargetHeading.dot(rocket.universeHeading) > -0.9) {
            rotationCorrection = rocket.universeTargetHeading.subtract(rocket.universeHeading);
            if (rotationCorrection.length() > Config.INSTANCE.rocketSpaceTravelRotationRate)
                rotationCorrection = rotationCorrection.normalize().scale(Config.INSTANCE.rocketSpaceTravelRotationRate);
        } else
            rotationCorrection = rocket.universeFront.subtract(rocket.universeHeading).scale(Config.INSTANCE.rocketSpaceTravelRotationRate);

        rocket.universeHeading = rocket.universeHeading.add(rotationCorrection).normalize();

        Vec3 targetFrontValid = rocket.universeHeading.cross(new Vec3(0, 1, 0).cross(rocket.universeHeading)).normalize();
        if (targetFrontValid.dot(rocket.universeFront) < -0.9) // get some movement if it is directly on the other side
            targetFrontValid = rocket.universeHeading.cross(rocket.universeFront);
        rotationCorrection = targetFrontValid.subtract(rocket.universeFront).scale(Config.INSTANCE.rocketSpaceTravelRotationRate);
        Vec3 newFront = rocket.universeFront.add(rotationCorrection).normalize();

        Vec3 right = rocket.universeHeading.cross(newFront).normalize();
        rocket.universeFront = right.cross(rocket.universeHeading).normalize();
    }

    public static boolean run(EntityRocket rocket, ResourceLocation target) {

        if (rocket.level().dimension().location().equals(target)) {
            return true;
        }

        // all movement is virtual
        rocket.enableMainEngines(true, false);
        rocket.enableSecondaryEngines(false, false);
        rocket.setTargetPosition(null, false);
        rocket.setDeltaMovement(0, 0, 0);
        rocket.setHeadingAndFrontDirect(new Vec3(0, 0, -1), new Vec3(0, 1, 0)); // default heading for space travel, rocket travel dim expects the rocket to head north for render


        // move to target dimension
        Dimension targetDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(target);
        if (targetDim == null) return true; // client might not have received the dimension sync packet
        Vec3 targetPosition = targetDim.getPosition(0);

        Vec3 nextTarget = SpaceNavigation.getNextTargetAvoidPlanetCollision(targetPosition, rocket.universePosition, DimensionManager.getDimensionManager(rocket.level().isClientSide), targetDim instanceof PlanetDimension p ? p : null);

        Vec3 nextTargetPositionRelative = nextTarget.subtract(rocket.universePosition);
        Vec3 nextTargetDirectiop = nextTargetPositionRelative.normalize();

        rocket.universeTargetHeading = nextTargetDirectiop;
        tickUniverseRotation(rocket);

        // move forward
        // targetSpeed is calculated so that for given distance and acceleration it will manage to accelerate to 0 when it reaches the target
        double targetSpeed = Math.sqrt(2 * Config.INSTANCE.rocketSpaceTravelAcceleration * nextTargetPositionRelative.length());
        targetSpeed = 0.000001 + targetSpeed * Math.max(0, nextTargetDirectiop.dot(rocket.universeHeading) - 0.9) * 10;
        double dspeed = targetSpeed - rocket.universeTravelSpeed;
        if (Math.abs(dspeed) > Config.INSTANCE.rocketSpaceTravelAcceleration) {
            dspeed = dspeed / Math.abs(dspeed) * Config.INSTANCE.rocketSpaceTravelAcceleration;
        }

        if (rocket.level().getGameTime() % 20 == 0) {
            System.out.println(rocket.universeTravelSpeed + ":" + dspeed + ":" + targetSpeed);
            System.out.println(nextTargetPositionRelative + ":" + nextTargetDirectiop.dot(rocket.universeHeading));
        }

        rocket.universeTravelSpeed += dspeed;
        rocket.universePosition = rocket.universePosition.add(rocket.universeHeading.scale(rocket.universeTravelSpeed));

        // TODO: make this much better with acceleration, also the pd controls might need tuning

        double entryDistance = Math.max(0.0001, CelestialUtils.toAU((targetDim instanceof PlanetDimension p ? p.getEarthRadiusMultiplier() : 1) * CelestialUtils.EARTH_RADIUS * Config.INSTANCE.planetRenderScaleMultiplier * 1.2));

        if (rocket.level() instanceof ServerLevel serverLevel && rocket.universePosition.distanceTo(targetPosition) < entryDistance) {
            // TODO: ifrocket.hasSatellites && shouldDeployThem -> deploy satellites shortly before dimension jump

            // get the teleportation target
            ServerLevel targetLevel = DimensionManager.getServerLevel(serverLevel.getServer(), target);
            Vec3 targetPos = new Vec3(rocket.getLastLaunchPosition().getX(), Config.INSTANCE.planetSkyHeight, rocket.getLastLaunchPosition().getZ());

            Vec3 entrySpeed = new Vec3(
                    (Math.random() * 2 - 1) * 1,
                    Config.INSTANCE.rocketPlanetEntrySpeedY,
                    (Math.random() * 2 - 1) * 1);

            EntityRocket newRocket = rocket.teleportTo(targetLevel, targetPos, entrySpeed);
        }


        return false;
    }
}
