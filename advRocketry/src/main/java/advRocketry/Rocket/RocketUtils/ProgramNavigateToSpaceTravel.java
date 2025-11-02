package advRocketry.Rocket.RocketUtils;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.DimensionProperties;
import advRocketry.Dimension.SpaceTravelManager;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.*;

// just a helper program, is not actually a real full program
public class ProgramNavigateToSpaceTravel {

    public static int orbitHeight = 2000;

    public static boolean run(EntityRocket rocket) {

        if (rocket.level().dimension().location().equals(SpaceTravelManager.dimId)) {
            return true;
        }

        rocket.enableMainEngines(true, false);
        rocket.enableSecondaryEngines(false, false);

        Dimension myDim = DimensionManager.get(rocket.level().dimension().location());
        if (myDim != null && myDim.getType() == DimensionProperties.PlanetType.SPACE_STATION) {
            // logic for space station
            // undock from station and move to launchpos.y-50, then thrust away
        } else {
            // normal logic; just fly high up!
            rocket.enableMainEngines(true, false);
            rocket.enableSecondaryEngines(false, false);
            rocket.setTargetPosition(new Vec3(rocket.position().x, orbitHeight + 500, rocket.position().z), false);

            orbitHeight = 400;
            if (rocket.position().y > orbitHeight && rocket.level() instanceof ServerLevel serverLevel) {
                // teleport to space travel dimension

                // first stop rocket movement because all future movement is virtual
                // and the rocket should not fly out of the force loaded chunk
                rocket.setDeltaMovement(0, 0, 0);
                rocket.setTargetPosition(null, false);

                // get the teleportation target
                ServerLevel target = DimensionManager.getServerLevel(serverLevel.getServer(), SpaceTravelManager.dimId);
                ChunkPos targetPos = SpaceTravelManager.getNextFreeChunkPos();
                BlockPos targetBlockPos = targetPos.getMiddleBlockPosition(100);


                // the dimension change is like this:
                // 1: unmount entities, but store where they were seated
                // 1: teleport every entity to the new dimension and put the new uuid to the new seat map
                // 2: teleport rocket
                // 3: find the entities by the new uuid and mount them at random position
                // 4: fix the seat position
                // 5: on client side: trigger remount on first tick because minecraft fails to sync correctly

                // store the passengers to remount them after dimension change at correct positions
                Map<UUID, BlockPos> newPassengerPositions = new HashMap<>();

                // unmount, teleport and store new uuid
                for (Entity passenger : rocket.getPassengers()) {
                    if (passenger != null) {
                        DimensionTransition transition = new DimensionTransition(target, targetBlockPos.getCenter().add(0,100,0), new Vec3(0, 0, 0), rocket.getYRot(), rocket.getXRot(), false, DimensionTransition.DO_NOTHING);
                        BlockPos seatPos = rocket.getPassengersPositions().get(passenger.getUUID());
                        passenger.stopRiding();
                        Entity newEntity = passenger.changeDimension(transition);
                        newPassengerPositions.put(newEntity.getUUID(), seatPos);
                    }
                }

                // teleport rocket
                DimensionTransition transition = new DimensionTransition(target, targetBlockPos.getCenter(), new Vec3(0, 0, 0), rocket.getYRot(), rocket.getXRot(), false, DimensionTransition.DO_NOTHING);
                EntityRocket newRocket = (EntityRocket) rocket.changeDimension(transition);

                // remount passengers
                for (UUID passengerUUID : newPassengerPositions.keySet()) {
                    Entity e = (target).getEntity(passengerUUID);
                    if (e != null) {
                        e.startRiding(newRocket);
                    }
                }

                // fix passengers positions
                newRocket.setPassengersPositions(newPassengerPositions);


                // initial command to force load the chunk so that the rocket starts ticking
                SpaceTravelManager.keepChunkLoaded(targetPos);

                newRocket.endProgram();
            }
        }

        return false;
    }
}
