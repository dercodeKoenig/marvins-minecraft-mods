package advRocketry.Rocket.RocketPrograms;

import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Rocket.EntityRocket;
import advRocketry.utils.CelestialUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

// just a helper program, is not actually a real full program
public class NavigateInSpaceToTargetDimension {

    // copied from rocketcontroller
    public static void tickUniverseRotation(EntityRocket rocket) {
        final double ROTATION_RATE = 0.01;

        // todo shouldnt this be normalized before scale?
        Vec3 rotationCorrection;
        if (rocket.universeTargetHeading.dot(rocket.universeHeading) > -0.9)
            rotationCorrection = rocket.universeTargetHeading.subtract(rocket.universeHeading).scale(ROTATION_RATE);
        else
            rotationCorrection = rocket.universeFront.subtract(rocket.universeHeading).scale(ROTATION_RATE);

        rocket.universeHeading = rocket.universeHeading.add(rotationCorrection).normalize();

        Vec3 targetFrontValid = rocket.universeHeading.cross(rocket.universeFront.cross(rocket.universeHeading)).normalize();
        if (targetFrontValid.dot(rocket.universeFront) < -0.9) // get some movement if it is directly on the other side
            targetFrontValid = rocket.universeHeading.cross(rocket.universeFront);
        rotationCorrection = targetFrontValid.subtract(rocket.universeFront).scale(ROTATION_RATE);
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
        Dimension targetDim = DimensionManager.get(target);
        Vec3 targetPosition = targetDim.getPosition(0);

        Vec3 targetPositionRelative = targetPosition.subtract(rocket.universePosition);
        Vec3 targetDirectiop = targetPositionRelative.normalize();

        rocket.universeTargetHeading = targetDirectiop;
        tickUniverseRotation(rocket);

        // move forward
        final double speed = Config.INSTANCE.rocketSpaceTravelSpeedBase * Math.max(0, rocket.universeHeading.dot(targetDirectiop)); // in AU per tick
        rocket.universePosition = rocket.universePosition.add(rocket.universeHeading.scale(speed));

        if (rocket.level() instanceof ServerLevel serverLevel && targetPositionRelative.length() < CelestialUtils.toAU(targetDim.getEarthRadiusMultiplier() * CelestialUtils.EARTH_RADIUS * Config.INSTANCE.planetRenderScaleMultiplier)) {
            // TODO: ifrocket.hasSatellites && shouldDeployThem -> deploy satellites shortly before dimension jump

            // get the teleportation target
            ServerLevel targetLevel = DimensionManager.getServerLevel(serverLevel.getServer(), target);
            Vec3 targetPos = new Vec3(rocket.getLastLaunchPosition().getX(), Config.INSTANCE.planetSkyHeight, rocket.getLastLaunchPosition().getZ());

            EntityRocket newRocket = rocket.teleportTo(targetLevel, targetPos);

            newRocket.setDeltaMovement(
                    newRocket.getRandom().nextDouble() * 2 - 1,
                    Config.INSTANCE.rocketPlanetEntrySpeedY,
                    newRocket.getRandom().nextDouble() * 2 - 1
            );
        }


        return false;
    }
}
