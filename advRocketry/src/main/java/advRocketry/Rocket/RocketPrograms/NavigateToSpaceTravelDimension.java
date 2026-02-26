package advRocketry.Rocket.RocketPrograms;

import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.Rocket.EntityRocket;
import advRocketry.utils.CelestialUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

// just a helper program, is not actually a real full program
public class NavigateToSpaceTravelDimension {

    public static boolean run(EntityRocket rocket) {

        if (rocket.level().dimension().location().equals(RocketTravelDimension.dimId)) {
            return true;
        }

        rocket.enableMainEngines(true, false);
        rocket.enableSecondaryEngines(false, false);
        rocket.setDefaultTargetHeading(new Vec3(0,1,0), false);

        Dimension myDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(rocket.level().dimension().location());
        if (myDim instanceof SpaceStationDimension spaceStationDimension) {
            // logic for space station
            // undock from station and move to launchpos.y-50, then thrust away
            if(rocket.level() instanceof ServerLevel serverLevel) {
                ServerLevel target = DimensionManager.getServerLevel(serverLevel.getServer(), RocketTravelDimension.dimId);
                ChunkPos targetPos = RocketTravelDimension.getNextFreeChunkPos();
                BlockPos targetBlockPos = targetPos.getMiddleBlockPosition(100);

                rocket.teleportTo(target, targetBlockPos.getCenter(), new Vec3(0, 0, 0));
            }
        } else {
            // normal logic; just fly high up!
            rocket.enableMainEngines(true, false);
            rocket.enableSecondaryEngines(false, false);
            rocket.setTargetPosition(new Vec3(rocket.position().x, Config.INSTANCE.planet_Sky_Height + 5000, rocket.position().z), false);

            if(rocket.position().y > Config.INSTANCE.planet_Sky_Height) {
                if (rocket.level() instanceof ServerLevel serverLevel) {
                    // teleport to space travel dimension

                    if (myDim != null) {
                        if (myDim instanceof PlanetDimension p) {
                            // for planets, move to the correct position relative to the planet.
                            double r = CelestialUtils.toAU(p.getEarthRadiusMultiplier() * CelestialUtils.EARTH_RADIUS * Config.INSTANCE.planet_Render_Scale_Multiplier * 1.1 + Config.INSTANCE.planet_Sky_Height * 10);
                            Vec3 planetUp = p.getGlobalAxisDirections(0, p.getLatitudeFromZPosition(rocket.position().z)).up;
                            rocket.universePosition = myDim.getPosition(0).add(planetUp.scale(r));
                            rocket.universeHeading = planetUp.normalize();
                            rocket.universeFront = rocket.universeHeading.cross(new Vec3(1, 0, 0));
                            if (rocket.universeFront.length() < 0.01)
                                rocket.universeFront = rocket.universeHeading.cross(new Vec3(0, 0, 1));
                        } else {
                            // just move to the position of the origin space object
                            rocket.universePosition = myDim.getPosition(0);
                        }
                    }

                    // get the teleportation target
                    ServerLevel target = DimensionManager.getServerLevel(serverLevel.getServer(), RocketTravelDimension.dimId);
                    ChunkPos targetPos = RocketTravelDimension.getNextFreeChunkPos();
                    BlockPos targetBlockPos = targetPos.getMiddleBlockPosition(100);

                    rocket.teleportTo(target, targetBlockPos.getCenter(), new Vec3(0, 0, 0));

                }else{
                    // client side, while waiting on dimension transition do not stop and rotate the rocket midflight, just keep going and wait for teleport to kick in
                    rocket.setTargetPosition(new Vec3(rocket.position().x, rocket.position().y + 5000, rocket.position().z), false);
                }
            }
        }

        return false;
    }
}
