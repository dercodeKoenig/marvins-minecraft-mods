package advRocketry.Rocket.RocketPrograms;

import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.Rocket.EntityRocket;
import advRocketry.utils.CelestialUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;

// just a helper program, is not actually a real full program
public class NavigateToSpaceTravelDimension {

    public static boolean run(EntityRocket rocket) {

        if(rocket.fuelTank.isEmpty()) rocket.endProgram();

        if (rocket.level().dimension().location().equals(RocketTravelDimension.dimId)) {
            return true;
        }

        rocket.enableMainEngines(true, false);
        rocket.enableSecondaryEngines(false, false);
        rocket.setDefaultTargetHeading(new Vec3(0,1,0), false);

        Dimension myDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(rocket.level().dimension().location());
        if (myDim != null && myDim.getType() == DimensionProperties.DimensionType.SPACE_STATION) {
            // logic for space station
            // undock from station and move to launchpos.y-50, then thrust away
        } else {
            // normal logic; just fly high up!
            rocket.enableMainEngines(true, false);
            rocket.enableSecondaryEngines(false, false);
            rocket.setTargetPosition(new Vec3(rocket.position().x, Config.INSTANCE.planetSkyHeight + 5000, rocket.position().z), false);

            if(rocket.position().y > Config.INSTANCE.planetSkyHeight) {
                if (rocket.level() instanceof ServerLevel serverLevel) {
                    // teleport to space travel dimension

                    if (myDim != null) {
                        // for planets, move to the correct position relative to the planet.
                        if (myDim instanceof PlanetDimension p) {
                            double r = CelestialUtils.toAU(p.getEarthRadiusMultiplier() * CelestialUtils.EARTH_RADIUS * Config.INSTANCE.planetRenderScaleMultiplier * 1.1 + Config.INSTANCE.planetSkyHeight * 10);
                            Vec3 planetUp = p.getGlobalAxisDirections(0, p.getLatitudeFromZPosition(rocket.position().z)).up;
                            rocket.universePosition = myDim.getPosition(0).add(planetUp.scale(r));
                            rocket.universeHeading = planetUp.normalize();
                            rocket.universeFront = rocket.universeHeading.cross(new Vec3(1, 0, 0));
                            if (rocket.universeFront.length() < 0.01)
                                rocket.universeFront = rocket.universeHeading.cross(new Vec3(0, 0, 1));
                        } else {
                            rocket.universePosition = myDim.getPosition(0);
                        }
                    }

                    // get the teleportation target
                    ServerLevel target = DimensionManager.getServerLevel(serverLevel.getServer(), RocketTravelDimension.dimId);
                    ChunkPos targetPos = RocketTravelDimension.getNextFreeChunkPos();
                    BlockPos targetBlockPos = targetPos.getMiddleBlockPosition(100);


                    rocket.teleportTo(target, targetBlockPos.getCenter(), new Vec3(0, 0, 0));


                    // initial command to force load the chunk so that the rocket starts ticking
                    RocketTravelDimension.keepChunkLoaded(targetPos);

                    // newRocket.endProgram();
                }else{
                    // client side, while waiting on dimension transition do not stop and rotate the rocket midflight, just keep going and wait for teleport to kick in
                    rocket.setTargetPosition(new Vec3(rocket.position().x, rocket.position().y + 5000, rocket.position().z), false);
                }
            }
        }

        return false;
    }
}
