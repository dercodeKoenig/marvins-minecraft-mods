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

    Vec3 lastRocketPosition = new Vec3(0, 0, 0);

    public static double travelHeight = 100;
    public static double maxD = 50; // for pd controller travel target distance

    public void run(EntityRocket rocket) {

        rocket.enableMainEngines(true);

        if (rocket.level().dimension().location().equals(targetDimensionId)) {
            // we are at the correct dimension

            double dx = target.getX() - rocket.position().x;
            double dz = target.getZ() - rocket.position().z;

            double distanceToTargetXZ = Math.sqrt(dx * dx + dz * dz);

            if (distanceToTargetXZ < 10) {
                // land
                rocket.controllerKDMultiplier = 1; // more aggressive breaking
                rocket.enableSecondaryEngines(true); // help or it swings around too much

                int y = rocket.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
                double dyi = rocket.position().y - y;
                double targetY = rocket.position().y - dyi * 1.2 + distanceToTargetXZ - 1;
                rocket.setTargetPosition(new Vec3(target.getX(), targetY, target.getZ()));

                // check if landed
                if(rocket.onGround()){
                    rocket.setTargetHeading(new Vec3(0, 1, 0));
                    rocket.setDeltaMovement(0,0,0);
                    rocket.endProgram();
                    for (BlockPos i : rocket.getEnginePositions()) {
                        Vec3 worldPos = RotationUtils.localToWorld(rocket, new Vec3(i.getX() + 0.5, i.getY() + 0.5, i.getZ() + 0.5));
                        ((ServerLevel) rocket.level()).sendParticles(new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.5f), 10), worldPos.x, worldPos.y, worldPos.z, 10, 0, 0, 0, 1);
                    }
                }
            } else {
                rocket.enableSecondaryEngines(false);

                // move to the correct xz coordinates
                if (rocket.position().y < travelHeight) {
                    // increase y first
                    rocket.setTargetPosition(new Vec3(rocket.position().x, travelHeight + 50, rocket.position().z));
                } else {
                    // high enough, move to target xz
                    rocket.setTargetPosition(new Vec3(rocket.position().x + Math.clamp(dx, -maxD, maxD), travelHeight + 50, rocket.position().z + Math.clamp(dz, -maxD, maxD)));
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
