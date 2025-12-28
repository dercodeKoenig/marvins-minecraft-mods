package advRocketry.Rocket.RocketPrograms;

import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import advRocketry.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;

public class ProgramNavigateToPlanetPosition implements RocketProgram {

    public static String id = "ProgramNavigateToPlanetPosition";

    public ResourceLocation targetDimensionId;
    public BlockPos target;

    public static double travelHeight = 150;
    public static double maxD = 100; // for pd controller travel target distance so that we dont get too fast

    double lastVy;

    public void run(EntityRocket rocket) {

        if(rocket.fuelTank.isEmpty()) rocket.endProgram();

        // if rocket.hasSatellites && shouldDeploySatellites: move to space first

        if (rocket.level().dimension().location().equals(targetDimensionId)) {

            rocket.setDefaultTargetHeading(new Vec3(0, 1, 0), false);
            rocket.enableMainEngines(true, false);

            // we are at the correct dimension

            double dx = target.getX() - rocket.position().x;
            double dz = target.getZ() - rocket.position().z;

            int y = Utils.findGroundY(rocket.level(), new BlockPos(target.getX(), rocket.level().getMaxBuildHeight(), target.getZ()));
            double dy = y - rocket.position().y;
            double distanceToTargetXZ = Math.sqrt(dx * dx + dz * dz);
            double speedxz = new Vec3(rocket.getDeltaMovement().x, 0, rocket.getDeltaMovement().z).length();

            double heightErrorMultiplier = 0.5; // dont close the height error at once, to slowly approach target set the height target to rocketY + dy * heightErrorMultiplier
            double xzDistanceHeightMultiplier = 1; // target height increases as we move more away from the target position in xz direction
            double speedHeightMultiplier = 20; // if we move fast, target is higher. we will only land if the movement in xz is close to 0
            double yOffset = -2; // the offset to the target. using 0 would make target = ground level, but it would approach it very slow, so add extra offset to the downside
            double targetY = rocket.position().y + dy * heightErrorMultiplier + distanceToTargetXZ * xzDistanceHeightMultiplier + yOffset + speedxz * speedHeightMultiplier;

            double maxDiffY = 500;
            if (rocket.position().y - targetY > maxDiffY) {
                targetY = rocket.position().y - maxDiffY;
            }

            Vec3 targetVec3 = new Vec3(target.getX(), targetY, target.getZ());


            if (distanceToTargetXZ < 50)
                rocket.enableSecondaryEngines(true, false); // help or it swings around too much
            else {
                rocket.enableSecondaryEngines(false, false);

                int yCurrentBelow = Utils.findGroundY(rocket.level(), new BlockPos(rocket.blockPosition().getX(), rocket.level().getMaxBuildHeight(), rocket.blockPosition().getZ()));

                if (rocket.position().y - yCurrentBelow < 20) {
                    // start / move up
                    targetVec3 = new Vec3(rocket.position().x, targetY, rocket.position().z);
                }
            }

            rocket.setTargetPosition(targetVec3, false);

            // check if landed
            // WARNING: onGround() appears to only work server side - it appears the server syncs it for 1 tick to client
            if ((rocket.onGround() || rocket.isInLiquid()) && distanceToTargetXZ < 10) {
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
        targetDimensionId = ResourceLocation.parse(nbt.getString("targetDimensionId"));
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("target", NbtUtils.writeBlockPos(target));
        tag.putString("targetDimensionId", targetDimensionId.toString());
        return tag;
    }
}
