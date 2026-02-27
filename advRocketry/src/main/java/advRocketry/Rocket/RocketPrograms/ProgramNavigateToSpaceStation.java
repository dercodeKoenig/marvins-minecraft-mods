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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class ProgramNavigateToSpaceStation implements RocketProgram {

    public static String id = "ProgramNavigateToSpaceStation";

    public ResourceLocation targetDimensionId;
    public ResourceLocation originDimensionId;
    public BlockPos target;

    // for docking mode
    Vec3 dockingPosition=null;
    int dockingDirection=0;

    boolean isStarted = false;

    public ProgramNavigateToSpaceStation(){
        // empty constructor required for save & load
    }

    public ProgramNavigateToSpaceStation(ResourceLocation targetDimensionId, ResourceLocation originDimensionId, BlockPos target, EntityRocket rocket){
        this.target = target;
        this.targetDimensionId = targetDimensionId;
        this.originDimensionId = originDimensionId;
        if(!originDimensionId.equals(targetDimensionId))
            isStarted = true;

        // docking detection, has to be here because client can not force load chunk and cannot detect in flight
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(server != null) {
            ServerLevel targetLevel = DimensionManager.getServerLevel(server, targetDimensionId);
            targetLevel.getChunk(target); // should load the chunk
            BlockEntity targetBE =targetLevel.getBlockEntity(target);
            if(targetBE instanceof EntityRocketAssembler rocketAssembler){
               dockingPosition = rocketAssembler.getLandingPos(rocket);
               Direction dockingStationFacing = rocketAssembler.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
               dockingDirection = -1;
               System.out.println("docking station found!");
            }
        }

    }

    public void run(EntityRocket rocket) {

        if (rocket.level().dimension().location().equals(targetDimensionId)) {

            if(dockingPosition == null){
                runWithoutDockingStation(rocket);
            }


        } else {
            // we are not at target dim, move to space!
            if (NavigateToSpaceTravelDimension.run(rocket)) {
                // we are in space, navigate to the target planet, the program will teleport the rocket to target dim
                NavigateInSpaceToTargetDimension.run(rocket, targetDimensionId, originDimensionId,() -> {

                    ServerLevel targetLevel = DimensionManager.getServerLevel(ServerLifecycleHooks.getCurrentServer(), targetDimensionId);

                    Vec3 targetPos = new Vec3(2000,100,0);

                    Vec3 entrySpeed = new Vec3(0,0,0);

                    EntityRocket newRocket = rocket.teleportTo(targetLevel, targetPos, entrySpeed);


                    Vec3 toTarget = target.getCenter().subtract(newRocket.position());
                    newRocket.setHeadingAndFrontDirect(toTarget,toTarget.cross(new Vec3(0,1,0).cross(toTarget)));
                });
            }
        }
    }

    void runWithoutDockingStation(EntityRocket rocket){

        Vec3 toTarget = target.getCenter().subtract(rocket.position());

        rocket.setDefaultTargetHeading(toTarget, false);

        rocket.setTargetFront(new Vec3(0, 1, 0), false);

        rocket.enableMainEngines(true, false);

        rocket.enableSecondaryEngines(true, false);

        rocket.setRotationRateMultiplier(1, false);

        Vec3 scaledToTarget = toTarget.scale(1);
        double maxD = 100;
        if(scaledToTarget.length() > maxD){
            scaledToTarget = scaledToTarget.normalize().scale(maxD);
        }
        Vec3 targetVec3 = rocket.position().add(scaledToTarget);

        rocket.setTargetPosition(targetVec3, false);

        // check if stopped (at target or collision maybe)
        if (rocket.getDeltaMovement().length() < 0.01 && toTarget.length() < 50) {
            rocket.setDeltaMovement(0, 0, 0);
            rocket.endProgram();
        }
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        target = NbtUtils.readBlockPos(nbt, "target").get();
        isStarted = nbt.getBoolean("isStarted");
        targetDimensionId = ResourceLocation.parse(nbt.getString("targetDimensionId"));
        if(nbt.contains("originDimensionId"))
            originDimensionId = ResourceLocation.parse(nbt.getString("originDimensionId"));
        dockingDirection = nbt.getInt("dockingDirection");
        dockingPosition = Utils.deSerializeVec3(nbt.getCompound("dockingPosition"));
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("target", NbtUtils.writeBlockPos(target));
        tag.putBoolean("isStarted", isStarted);
        tag.putString("targetDimensionId", targetDimensionId.toString());
        if(originDimensionId != null)
            tag.putString("originDimensionId", originDimensionId.toString());
        tag.put("dockingPosition", Utils.serializeVec3(dockingPosition));
        tag.putInt("dockingDirection", dockingDirection);
        return tag;
    }
}
