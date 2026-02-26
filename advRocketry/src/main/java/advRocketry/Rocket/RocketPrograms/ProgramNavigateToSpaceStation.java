package advRocketry.Rocket.RocketPrograms;

import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import advRocketry.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class ProgramNavigateToSpaceStation implements RocketProgram {

    public static String id = "ProgramNavigateToSpaceStation";

    public ResourceLocation targetDimensionId;
    public ResourceLocation originDimensionId;
    public BlockPos target;

    boolean isStarted = false;

    public ProgramNavigateToSpaceStation(){
        // empty constructor required for save & load
    }

    public ProgramNavigateToSpaceStation(ResourceLocation targetDimensionId, ResourceLocation originDimensionId, BlockPos target){
        this.target = target;
        this.targetDimensionId = targetDimensionId;
        this.originDimensionId = originDimensionId;
        if(!originDimensionId.equals(targetDimensionId))
            isStarted = true;
    }

    public void run(EntityRocket rocket) {

        if (rocket.level().dimension().location().equals(targetDimensionId)) {

            Vec3 toTarget = target.getCenter().subtract(rocket.position());


                rocket.setDefaultTargetHeading(toTarget, false);
                rocket.enableMainEngines(true, false);
                rocket.enableSecondaryEngines(true, false);
                rocket.setRotationRateMultiplier(1, false);



            Vec3 targetVec3 = new Vec3(target.getCenter().x, target.getCenter().y, target.getCenter().z);

            if (rocket.level().getBlockEntity(target) instanceof EntityRocketAssembler assembler) {
                targetVec3 = assembler.getLandingPos(rocket);
            }

            double maxD = 200;
            if(toTarget.length() > maxD){
                targetVec3 = rocket.position().add(toTarget.normalize().scale(maxD));
            }
            rocket.setTargetPosition(targetVec3, false);

            // check if at target
            if (toTarget.length() < 0.1 && rocket.getDeltaMovement().length() < 0.002) {
                rocket.setDeltaMovement(0, 0, 0);
                rocket.setPos(targetVec3);
                rocket.endProgram();
            }
        } else {
            // we are not at target dim, move to space!
            if (NavigateToSpaceTravelDimension.run(rocket)) {
                // we are in space, navigate to the target planet, the program will teleport the rocket to target dim
                NavigateInSpaceToTargetDimension.run(rocket, targetDimensionId, originDimensionId,() -> {

                    ServerLevel targetLevel = DimensionManager.getServerLevel(ServerLifecycleHooks.getCurrentServer(), targetDimensionId);

                    Vec3 targetPos = new Vec3(0,100,2000);

                    Vec3 entrySpeed = new Vec3(0,0,0);

                    rocket.teleportTo(targetLevel, targetPos, entrySpeed);
                });
            }
        }
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        target = NbtUtils.readBlockPos(nbt, "target").get();
        isStarted = nbt.getBoolean("isStarted");
        targetDimensionId = ResourceLocation.parse(nbt.getString("targetDimensionId"));
        if(nbt.contains("originDimensionId"))
            originDimensionId = ResourceLocation.parse(nbt.getString("originDimensionId"));
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("target", NbtUtils.writeBlockPos(target));
        tag.putBoolean("isStarted", isStarted);
        tag.putString("targetDimensionId", targetDimensionId.toString());
        if(originDimensionId != null)
            tag.putString("originDimensionId", originDimensionId.toString());
        return tag;
    }
}
