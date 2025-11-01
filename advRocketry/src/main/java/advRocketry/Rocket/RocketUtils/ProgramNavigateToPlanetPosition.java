package advRocketry.Rocket.RocketUtils;

import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.core.jmx.Server;
import org.joml.Vector3f;

public class ProgramNavigateToPlanetPosition implements RocketProgram {

    public static String id = "ProgramNavigateToPlanetPosition";

    public ResourceLocation targetDimensionId;
    public BlockPos target;

    public static double travelHeight = 150;
    public static double maxD = 100; // for pd controller travel target distance so that we dont get too fast

    public void run(EntityRocket rocket) {
        travelHeight = 100;

        maxD = 100;

        rocket.enableMainEngines(true);

        if (rocket.level().dimension().location().equals(targetDimensionId)) {
            // we are at the correct dimension

            double dx = target.getX() - rocket.position().x;
            double dz = target.getZ() - rocket.position().z;

            int y = rocket.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
            double dy = y - rocket.position().y;
            double distanceToTargetXZ = Math.sqrt(dx * dx + dz * dz);
            double speedxz = new Vec3(rocket.getDeltaMovement().x, 0, rocket.getDeltaMovement().z).length();

            double heightErrorMultiplier = 0.5; // dont close the height error at once, to slowly approach target set the height target to rocketY + dy * heightErrorMultiplier
            double xzDistanceHeightMultiplier = 1; // target height increases as we move more away from the target position in xz direction
            double speedHeightMultiplier = 20; // if we move fast, target is higher. we will only land if the movement in xz is close to 0
            double yOffset = -2; // the offset to the target. using 0 would make target = ground level, but it would approach it very slow, so add extra offset to the downside
            double targetY = rocket.position().y + dy * heightErrorMultiplier + distanceToTargetXZ * xzDistanceHeightMultiplier + yOffset + speedxz*speedHeightMultiplier;

            Vec3 targetVec3 = new Vec3(target.getX(), targetY, target.getZ());

            if (distanceToTargetXZ < 50)
                rocket.enableSecondaryEngines(true); // help or it swings around too much
            else{
                int yCurrentBelow = rocket.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
                if(rocket.position().y - yCurrentBelow < 20){
                    // start / move up
                    targetVec3 = new Vec3(rocket.position().x, targetY, rocket.position().z);
                }
            }

            rocket.setTargetPosition(targetVec3);

            // check if landed
            if (rocket.onGround() && distanceToTargetXZ < 5) {
                rocket.setTargetHeading(new Vec3(0, 1, 0));
                rocket.setDeltaMovement(0, 0, 0);
                rocket.endProgram();
                for (BlockPos i : rocket.getEnginePositions()) {
                    Vec3 worldPos = RotationUtils.localToWorld(rocket, new Vec3(i.getX() + 0.5, i.getY() + 0.5, i.getZ() + 0.5));
                    ((ServerLevel) rocket.level()).sendParticles(new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.5f), 10), worldPos.x, worldPos.y, worldPos.z, 10, 0, 0, 0, 1);
                }
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
