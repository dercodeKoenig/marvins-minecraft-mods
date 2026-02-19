package advRocketry.Rocket.RocketPrograms;

import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import advRocketry.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class ProgramNavigateToPlanetPosition implements RocketProgram {

    public static String id = "ProgramNavigateToPlanetPosition";

    public ResourceLocation targetDimensionId;
    public BlockPos target;

    public static double travelHeight = 200;

    double lastVy;
    boolean isStarted = false; // make sure at start it actually fly up

    public void run(EntityRocket rocket) {

        // if rocket.hasSatellites && shouldDeploySatellites: move to space first

        if (rocket.level().dimension().location().equals(targetDimensionId)) {

            rocket.setDefaultTargetHeading(new Vec3(0, 1, 0), false);
            rocket.enableMainEngines(true, false);

            // we are at the correct dimension

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

            double maxDiffY = 500;

            if (!isStarted) {
                // make sure it starts correctly
                rocket.enableSecondaryEngines(false, false);
                targetVec3 = new Vec3(rocket.position().x, maxY + travelHeight, rocket.position().z);
                if (rocket.position().y > travelHeight / 3 + maxY) {
                    isStarted = true; // ok, it is in air now
                }
            } else if (distanceToTargetXZ > 50) {
                // when far away from target, make sure to maintain travel height to go there
                rocket.enableSecondaryEngines(false, false);

                if (rocket.position().y < 20 + maxY) {
                    // too low, pull up
                    targetVec3 = new Vec3(rocket.position().x, maxY + travelHeight, rocket.position().z);
                } else {
                    // travel to target at target height
                    // dont move faster than maxDiffxz in xz direction to not crash in ground on long distance
                    double maxDiffxz = 200;

                    // navigation is a bit more difficult on low gravity planets so dont move too fast there
                    double g = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(rocket.level().dimension().location()).getGravitationalMultiplier();
                    maxDiffxz *= Math.pow(g, 0.25);

                    double xzMultiplier = Math.min(1, (maxDiffxz / distanceToTargetXZ));
                    double targetX = rocket.position().x + dx * xzMultiplier;
                    double targetZ = rocket.position().z + dz * xzMultiplier;

                    targetVec3 = new Vec3(targetX, Math.max(maxY + travelHeight, rocket.position().y - maxDiffY), targetZ);

                    // rotate to the target front if it is a rocket assembler there
                    if (rocket.level().getBlockEntity(target) instanceof EntityRocketAssembler assembler) {
                        Direction targetFront = assembler.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
                        rocket.setTargetFront(new Vec3(targetFront.getStepX(), targetFront.getStepY(), targetFront.getStepZ()), false);
                    }
                }
            } else {
                // landing...
                rocket.enableSecondaryEngines(true, false); // help or it swings around too much

                double dy = yTargetBelow - rocket.position().y;
                double speedxz = new Vec3(rocket.getDeltaMovement().x, 0, rocket.getDeltaMovement().z).length();

                double heightErrorMultiplier = 0.5; // dont close the height error at once, to slowly approach target set the height target to rocketY + dy * heightErrorMultiplier
                double xzDistanceHeightMultiplier = 2; // target height increases as we move more away from the target position in xz direction
                double speedHeightMultiplier = 20; // if we move fast, target is higher. we will only land if the movement in xz is close to 0
                double yOffset = -3; // the offset to the target. using 0 would make target = ground level, but it would approach it very slow, so add extra offset to the downside
                double targetY = rocket.position().y + dy * heightErrorMultiplier + distanceToTargetXZ * xzDistanceHeightMultiplier + yOffset + speedxz * speedHeightMultiplier;

                if (rocket.position().y - targetY > maxDiffY) {
                    targetY = rocket.position().y - maxDiffY;
                }

                targetVec3 = new Vec3(targetVec3.x, targetY, targetVec3.z);

            }


            rocket.setTargetPosition(targetVec3, false);

            // check if landed
            // WARNING: onGround() appears to only work server side - it appears the server syncs it for 1 tick to client
            if ((rocket.onGround() || rocket.isInLiquid()) && distanceToTargetXZ < 10 && isStarted) {
                rocket.setDeltaMovement(0, 0, 0);
                rocket.endProgram();
            }
            lastVy = rocket.getDeltaMovement().y;
        } else {
            // we are not at target dim, move to space!
            if (NavigateToSpaceTravelDimension.run(rocket)) {
                // we are in space, navigate to the target planet, the program will teleport the rocket to target dim
                NavigateInSpaceToTargetDimension.run(rocket, targetDimensionId);
            }
        }
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        target = NbtUtils.readBlockPos(nbt, "target").get();
        isStarted = nbt.getBoolean("isStarted");
        targetDimensionId = ResourceLocation.parse(nbt.getString("targetDimensionId"));
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("target", NbtUtils.writeBlockPos(target));
        tag.putBoolean("isStarted", isStarted);
        tag.putString("targetDimensionId", targetDimensionId.toString());
        return tag;
    }
}
