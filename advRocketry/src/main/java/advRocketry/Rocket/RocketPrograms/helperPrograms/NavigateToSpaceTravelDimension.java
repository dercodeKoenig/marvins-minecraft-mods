package advRocketry.Rocket.RocketPrograms.helperPrograms;

import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Utils.CelestialUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

// just a helper program, is not actually a real full program
public class NavigateToSpaceTravelDimension {

    public StationDockingProgram.UnDockingProgram unDockingProgram;

    public NavigateToSpaceTravelDimension() {

    }

    public NavigateToSpaceTravelDimension(EntityRocket rocket) {
        BlockEntity undockingStation = null;
        BlockPos undockingStationPos = rocket.getDockingStationPos();
        if (undockingStationPos != null) {
            rocket.level().getChunk(undockingStationPos);
            undockingStation = rocket.level().getBlockEntity(undockingStationPos);
        }

        if (undockingStation instanceof EntityRocketAssembler rocketAssembler && DimensionManager.INSTANCE_SERVER.get(rocket.level().dimension().location()) instanceof SpaceStationDimension) {
            unDockingProgram = new StationDockingProgram.UnDockingProgram(
                    rocketAssembler.getLandingPos(rocket),
                    rocketAssembler.getDockingDirection(),
                    rocketAssembler.getMoveAwayDirection()
            );
        }
    }

    public static void teleportToSpaceTravel(EntityRocket rocket, Dimension myDim) {
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
        ServerLevel target = DimensionManager.getServerLevel(RocketTravelDimension.dimId);
        ChunkPos targetPos = RocketTravelDimension.getNextFreeChunkPos();
        BlockPos targetBlockPos = targetPos.getMiddleBlockPosition(100);

        rocket.teleportTo(target, targetBlockPos.getCenter(), new Vec3(0, 0, 0));
    }

    public boolean run(EntityRocket rocket, SpaceReachedCallback callback) {

        if (rocket.level().dimension().location().equals(RocketTravelDimension.dimId)) {
            return true;
        }

        Dimension myDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(rocket.level().dimension().location());
        if (myDim instanceof SpaceStationDimension spaceStationDimension) {
            // logic for space station

            if (unDockingProgram != null) {
                unDockingProgram.run(rocket, () -> {
                }, () -> {
                    unDockingProgram = null;
                });
            }
            if (unDockingProgram == null) {
                if (!callback.onSpaceReached()) {
                    if (rocket.level() instanceof ServerLevel serverLevel) {
                        teleportToSpaceTravel(rocket, myDim);
                    }
                }
            }
        } else {
            // normal logic; just fly high up!
            rocket.controller.enableMainEngines(true, false);
            rocket.controller.enableSecondaryEngines(false, false);
            rocket.controller.setDefaultTargetHeading(new Vec3(0, 1, 0), false);
            rocket.controller.setRotationRateMultiplier(1, false);

            rocket.controller.setTargetPosition(new Vec3(rocket.position().x, Config.INSTANCE.planet_Sky_Height + 500, rocket.position().z), false);

            if (rocket.position().y > Config.INSTANCE.planet_Sky_Height) {
                if (!callback.onSpaceReached()) {
                    if (rocket.level() instanceof ServerLevel) {
                        teleportToSpaceTravel(rocket, myDim);
                    } else {
                        // client side, while waiting on dimension transition do not stop and rotate the rocket midflight, just keep going and wait for teleport to kick in
                        rocket.controller.setTargetPosition(new Vec3(rocket.position().x, rocket.position().y + 500, rocket.position().z), false);
                    }
                }
            }
        }
        return false;
    }

    public void readFromNbt(CompoundTag nbt) {
        if (nbt.contains("unDockingProgram")) {
            unDockingProgram = new StationDockingProgram.UnDockingProgram();
            unDockingProgram.readFromNbt(nbt.getCompound("unDockingProgram"));
        }
    }

    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        if (unDockingProgram != null)
            tag.put("unDockingProgram", unDockingProgram.saveToNbt());
        return tag;
    }

    public interface SpaceReachedCallback {
        default boolean onSpaceReached() {
            return false;
        }
    }
}
