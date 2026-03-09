package advRocketry.Rocket.RocketPrograms;

import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import advRocketry.Rocket.RocketPrograms.helperPrograms.NavigateInSpaceToTargetDimension;
import advRocketry.Rocket.RocketPrograms.helperPrograms.NavigateToSpaceTravelDimension;
import advRocketry.Utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class ProgramNavigateToPlanetPosition implements RocketProgram {

    public static double travelHeight = 200;
    public ResourceLocation targetDimensionId;
    public ResourceLocation originDimensionId;
    public BlockPos target;
    boolean isStarted = false; // make sure at start it actually fly up

    NavigateToSpaceTravelDimension navigateToSpaceTravelDimension;

    public ProgramNavigateToPlanetPosition() {
        // empty constructor required for save & load
    }

    public ProgramNavigateToPlanetPosition(EntityRocket rocket, ResourceLocation targetDimensionId, BlockPos target) {
        this.target = target;
        this.targetDimensionId = targetDimensionId;
        this.originDimensionId = rocket.level().dimension().location();

        if (!Objects.equals(originDimensionId, targetDimensionId)) {
            // we need to go to other dimension
            navigateToSpaceTravelDimension = new NavigateToSpaceTravelDimension(rocket);
        }
    }

    public void run(EntityRocket rocket) {

        // if rocket.hasSatellites && shouldDeploySatellites: move to space first
        // or make a custom program for it?
        // custom program is better....

        PlanetDimension targetDimension = (PlanetDimension) DimensionManager.getDimensionManager(rocket.level().isClientSide).get(targetDimensionId);
        if (targetDimension == null) {
            rocket.setDeltaMovement(0, 0, 0);
            rocket.endProgram();
            return;
        }

        if (rocket.level().dimension().location().equals(targetDimensionId)) {
            // we are at the correct dimension

            rocket.controller.setDefaultTargetHeading(new Vec3(0, 1, 0), false);
            rocket.controller.enableMainEngines(true, false);
            rocket.controller.setRotationRateMultiplier(1, false);

            Vec3 targetVec3 = new Vec3(target.getCenter().x, target.getCenter().y, target.getCenter().z);

            if (rocket.level().getBlockEntity(target) instanceof EntityRocketAssembler assembler) {
                targetVec3 = assembler.getLandingPos(rocket);
            }

            double dx = targetVec3.x - rocket.position().x;
            double dz = targetVec3.z - rocket.position().z;
            double distanceToTargetXZ = Math.sqrt(dx * dx + dz * dz);

            int yCurrentBelow = Utils.findGroundY(rocket.level(), new BlockPos(rocket.blockPosition().getX(), rocket.level().getMaxBuildHeight(), rocket.blockPosition().getZ()));
            int yTargetBelow = Utils.findGroundY(rocket.level(), new BlockPos((int) targetVec3.x, rocket.level().getMaxBuildHeight(), (int) targetVec3.z));

            int maxY = Math.max(yTargetBelow, yCurrentBelow);

            if (!isStarted) {
                // make sure it starts correctly
                rocket.controller.enableSecondaryEngines(false, false);
                targetVec3 = new Vec3(rocket.position().x, maxY + travelHeight, rocket.position().z);
                if (rocket.position().y > travelHeight / 3 + maxY) {
                    isStarted = true; // ok, it is in air now
                }
            } else if (distanceToTargetXZ > 50) {
                // when far away from target, make sure to maintain travel height to go there
                rocket.controller.enableSecondaryEngines(false, false);

                if (rocket.position().y < 20 + maxY) {
                    // too low, pull up
                    targetVec3 = new Vec3(rocket.position().x, maxY + travelHeight, rocket.position().z);
                } else {
                    // travel to target at target height
                    // dont move faster than maxDiffxz in xz direction to not crash in ground on long distance
                    // (the controller has received an upgrade to prevent this but still, dont go too fast for chunk loading)
                    double maxDiffxz = 200;
                    // dont go too fast down for stability
                    double maxDiffY = 500;

                    // navigation is a bit more difficult on low gravity planets so dont move too fast there
                    double g = 1;
                    Dimension myDim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(rocket.level().dimension().location());
                    if (myDim != null)
                        g = myDim.getGravitationalMultiplier();
                    maxDiffxz *= Math.pow(g, 0.25);

                    double xzMultiplier = Math.min(1, (maxDiffxz / distanceToTargetXZ));
                    double targetX = rocket.position().x + dx * xzMultiplier;
                    double targetZ = rocket.position().z + dz * xzMultiplier;

                    targetVec3 = new Vec3(targetX, Math.max(maxY + travelHeight, rocket.position().y - maxDiffY), targetZ);
                }
            } else {
                // landing...
                rocket.controller.enableSecondaryEngines(true, false); // help or it swings around too much


                double xzDistanceHeightMultiplier = 5; // target height increases as we move more away from the target position in xz direction
                double speedHeightMultiplier = 20; // if we move fast, target is higher. we will only land if the movement in xz is close to 0
                double yOffset = 3; // the offset to the target. using 0 would make target = ground level, but it would approach it very slow, so add extra offset
                double speedxz = new Vec3(rocket.getDeltaMovement().x, 0, rocket.getDeltaMovement().z).length();
                double targetY = yTargetBelow + distanceToTargetXZ * xzDistanceHeightMultiplier + yOffset + speedxz * speedHeightMultiplier;

                // we know the distance to target and the rocket max acceleration
                // we can know the target speed to be able to stop at ground by
                // v = sqrt( 2 * a * d )
                // if we are too fast, it should not be too bad because a will increase as we burn fuel so it can slightly stabilize again
                // also note the rocket will reduce its max acc near ground so i keep a base distance to keep it moving
                double d = Math.max(2, rocket.position().y - targetY); // make it have a base speed for lower, especially since the rocket will reduce its allowed acc near ground
                double a = rocket.getThrustMax() / rocket.getMass();
                double g = rocket.getGravity();
                double aEff = Math.max(0,Math.min(a, rocket.getMaxAcceleration()) - g); // never have it go negative
                double vTarget = -Math.sqrt(2 * d * aEff) * 0.8; // scaling a bit down just for safety
                double vCurrent = rocket.getDeltaMovement().y; // consider the y movement
                double error = vTarget - vCurrent;

                // now we have a velocity error, but the pd controller still expects a position
                // i will move the target toward the rocket when we are too fast so it will slow down
                // i will move the target lower if we are too slow and have room to speed up
                targetY = targetY + error * 1000 - yOffset;

                // extra safety, it is unstable without
                // the logic above would work if the rocket would face up
                // but in reality it does not face up
                // so here, i limit max speed and max downside
                double maxD = 1000;
                if (rocket.position().y - targetY > maxD)
                    targetY = rocket.position().y - maxD;
                targetY = Math.max(targetY, yCurrentBelow - 13); // some y offset or it would approach infinite slow

                // in the end it is not a true suicide burn,
                // but it should make heavy rockets with low acceleration thrust early and no smash into ground

                //System.out.println(targetY+":"+vTarget+":"+vCurrent+":"+error);
                targetVec3 = new Vec3(targetVec3.x, targetY, targetVec3.z);
            }

            // rotate to the target front if it is a rocket assembler there
            if (rocket.position().y > maxY + 1) {
                if (rocket.level().getBlockEntity(target) instanceof EntityRocketAssembler assembler) {
                    Direction targetFront = assembler.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
                    rocket.controller.setTargetFront(new Vec3(targetFront.getStepX(), targetFront.getStepY(), targetFront.getStepZ()), false);
                }
            } else {
                rocket.controller.setTargetFront(rocket.controller.getFront(), false);
            }


            rocket.controller.setTargetPosition(targetVec3, false);

            // check if landed
            // WARNING: onGround() appears to only work server side - it appears the server syncs it for 1 tick to client
            if ((rocket.onGround() || rocket.isInLiquid()) && distanceToTargetXZ < 10 && isStarted) {
                rocket.setDeltaMovement(0, 0, 0);
                rocket.endProgram();
            }

        } else {

            if (rocket.level().dimension().location().equals(RocketTravelDimension.dimId)) {
                // we are in space, navigate to the target planet and teleport the rocket to target dim
                NavigateInSpaceToTargetDimension.run(rocket, targetDimensionId, originDimensionId, () -> {
                    teleportToPlanet(rocket);
                });
            } else {
                // we are not at target dim, move to space!
                navigateToSpaceTravelDimension.run(rocket, new NavigateToSpaceTravelDimension.SpaceReachedCallback() {
                    public boolean onSpaceReached() {
                        if(rocket.level().isClientSide) return false;

                        // if we come from a pace station that orbits the planet,
                        // skip space travel and go to target instantly
                        Dimension rocketDimension = DimensionManager.INSTANCE_SERVER.get(rocket.level().dimension().location());
                        if (rocketDimension instanceof SpaceStationDimension spaceStationDimension) {
                            if (Objects.equals(spaceStationDimension.getParentDimensionId(), targetDimensionId) && spaceStationDimension.isInOrbit()) {
                                teleportToPlanet(rocket);
                                return true;
                            }
                        }
                        return false;
                    }
                });
            }
        }
    }

    public void teleportToPlanet(EntityRocket rocket) {
        if(rocket.level().isClientSide)return;

        // get the teleportation target
        ServerLevel targetLevel = DimensionManager.getServerLevel(targetDimensionId);
        Vec3 targetPos = new Vec3(target.getX(), Config.INSTANCE.planet_Sky_Height, target.getZ());

        Vec3 entrySpeed = new Vec3(
                (Math.random() * 2 - 1) * 0.3,
                Config.INSTANCE.rocket_Planet_Entry_Speed_Y,
                (Math.random() * 2 - 1) * 0.3);

        rocket.teleportTo(targetLevel, targetPos, entrySpeed);
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        target = NbtUtils.readBlockPos(nbt, "target").get();
        isStarted = nbt.getBoolean("isStarted");
        targetDimensionId = ResourceLocation.parse(nbt.getString("targetDimensionId"));
        originDimensionId = ResourceLocation.parse(nbt.getString("originDimensionId"));
        if(nbt.contains("navigateToSpaceTravelDimension")) {
            navigateToSpaceTravelDimension = new NavigateToSpaceTravelDimension();
            navigateToSpaceTravelDimension.readFromNbt(nbt.getCompound("navigateToSpaceTravelDimension"));
        }
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("target", NbtUtils.writeBlockPos(target));
        tag.putBoolean("isStarted", isStarted);
        tag.putString("targetDimensionId", targetDimensionId.toString());
        tag.putString("originDimensionId", originDimensionId.toString());
        if(navigateToSpaceTravelDimension != null)
            tag.put("navigateToSpaceTravelDimension", navigateToSpaceTravelDimension.saveToNbt());
        return tag;
    }
}
