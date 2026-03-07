package advRocketry.Rocket.RocketPrograms.helperPrograms;

import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class StationDockingProgram {
    public  static class DockingProgram {
        public Vec3 dockingPosition;
        public Direction dockingDirection; // up, down, side
        public boolean checkpointReached = false;
        public BlockPos dockingStationPos;

        public DockingProgram() {}

        public DockingProgram(BlockPos dockingStationPos, Vec3 landingPos, Direction dockingDirection) {
            this.dockingDirection = dockingDirection;
            this.dockingPosition = landingPos;
            this.dockingStationPos = dockingStationPos;
        }

        void moveToCheckpoint(EntityRocket rocket) {
            Vec3 checkpointPos = this.dockingPosition.add(
                    new Vec3(
                            dockingDirection.getStepX(),
                            dockingDirection.getStepY(),
                            dockingDirection.getStepZ()
                    ).scale(10+rocket.size.getY())
            );
            Vec3 toTarget = checkpointPos.subtract(rocket.position());
            if (rocket.getDeltaMovement().length() < 0.03 && toTarget.length() < 5)
                rocket.controller.enableMainEngines(false, false);
            else
                rocket.controller.enableMainEngines(true, false);
            rocket.controller.enableSecondaryEngines(true, false);
            rocket.controller.setRotationRateMultiplier(1, false);
            rocket.controller.setDefaultTargetHeading(rocket.controller.getHeading(), false);
            rocket.controller.setTargetFront(new Vec3(0,1,0), false);



            double maxD = 100;
            if (toTarget.length() > maxD) {
                toTarget = toTarget.normalize().scale(maxD);
            }
            Vec3 targetVec3 = rocket.position().add(toTarget);
            rocket.controller.setTargetPosition(targetVec3, false);


            // check if stopped (at target or collision maybe)
            if (rocket.getDeltaMovement().length() < 0.002 && toTarget.length() < 2) {
                checkpointReached = true;
            }
        }


        void moveToDockingStation(EntityRocket rocket) {

            rocket.controller.enableMainEngines(false, false);
            rocket.controller.enableSecondaryEngines(true, false);

            if(rocket.level().getBlockEntity(dockingStationPos) instanceof EntityRocketAssembler rocketAssembler) {
                Direction stationFacing = rocketAssembler.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
                if (rocketAssembler.horizontalDocking) {
                    rocket.controller.setRotationRateMultiplier(0.5, false);
                    rocket.controller.setTargetFront(new Vec3(0, 1, 0), false);
                    rocket.controller.setDefaultTargetHeading(new Vec3(stationFacing.getOpposite().getStepX(), stationFacing.getOpposite().getStepY(), stationFacing.getOpposite().getStepZ()), false);
                } else {
                    rocket.controller.setRotationRateMultiplier(0.2, false);
                    rocket.controller.setTargetFront(new Vec3(stationFacing.getStepX(), stationFacing.getStepY(), stationFacing.getStepZ()), false);
                    rocket.controller.setDefaultTargetHeading(new Vec3(0, 1, 0), false);
                }
            }

            Vec3 toTarget = dockingPosition.subtract(rocket.position());

            Vec3 scaledToTarget = new Vec3(toTarget.x*1, toTarget.y*1, toTarget.z*1);
            Vec3 targetVec3 = rocket.position().add(scaledToTarget);

            rocket.controller.setTargetPosition(targetVec3, false);

            if (rocket.getDeltaMovement().length() < 0.005 && toTarget.length() < 0.1) {
                if(!rocket.level().isClientSide) {
                    rocket.setPos(dockingPosition);
                    rocket.setDeltaMovement(0,0,0);
                }
                rocket.endProgram();
            }
        }

        public void run(EntityRocket rocket){
             if(!checkpointReached){
                 moveToCheckpoint(rocket);
             }else{
                 rocket.setDockingStationPos(dockingStationPos, false);
                 moveToDockingStation(rocket);
             }
        }

        public void readFromNbt(CompoundTag nbt) {
            dockingPosition = Utils.deSerializeVec3(nbt.getCompound("dockingPosition"));
            dockingDirection = Direction.values()[nbt.getInt("dockingDirection")];
            checkpointReached = nbt.getBoolean("checkpointReached");
            dockingStationPos = NbtUtils.readBlockPos(nbt, "dockingStationPos").get();
        }

        public CompoundTag saveToNbt() {
            CompoundTag tag = new CompoundTag();
            tag.put("dockingPosition", Utils.serializeVec3(dockingPosition));
            tag.putInt("dockingDirection", dockingDirection.ordinal());
            tag.putBoolean("checkpointReached", checkpointReached);
            tag.put("dockingStationPos", NbtUtils.writeBlockPos(dockingStationPos));
            return tag;
        }
    }

    public static class UnDockingProgram {

        // up, down, side
        public Direction undockingDirection;
        public Vec3 moveAwayDirection;
        public Vec3 dockingPosition;
        public boolean checkpointReached = false;

        public UnDockingProgram() {}

        public UnDockingProgram(Vec3 dockingPosition, Direction undockingDirection, Vec3 moveAwayDirection) {
            this.undockingDirection = undockingDirection;
            this.moveAwayDirection = moveAwayDirection;
            this.dockingPosition = dockingPosition ;
        }

        public void moveToCheckpoint(EntityRocket rocket, Runnable onCheckpointReached) {
            Vec3 checkpointPos = this.dockingPosition.add(
                    new Vec3(
                            undockingDirection.getStepX(),
                            undockingDirection.getStepY(),
                            undockingDirection.getStepZ()
                    ).scale(10+rocket.size.getY())
            );

            rocket.controller.enableMainEngines(false, false);
            rocket.controller.enableSecondaryEngines(true, false);

            rocket.controller.setTargetPosition(checkpointPos, false);

            // check if stopped (at target or collision maybe)
            if (rocket.getDeltaMovement().length() < 0.05 && rocket.position().distanceTo(checkpointPos) < 5) {
                rocket.controller.setRotationRateMultiplier(0.2, false);
                rocket.controller.setDefaultTargetHeading(moveAwayDirection, false);
                rocket.controller.setTargetFront(new Vec3(0,1,0), false);
            }

            // if we are far enough away and we came to a stop and we align with the target heading, move on to next stage
            if (rocket.getDeltaMovement().length() < 0.01 && // no more movement
                    rocket.position().distanceTo(dockingPosition) > checkpointPos.distanceTo(dockingPosition) - 3 && // moved away enough
                    rocket.controller.getHeading().dot(rocket.controller.getDefaultTargetHeading()) > 0.8) { // heading aligns with target vector
                checkpointReached = true;
                onCheckpointReached.run();
            }
        }

        void moveAway(EntityRocket rocket, Runnable onStationLeft){
            if(rocket.position().distanceTo(dockingPosition) > 200){
                // program success
                rocket.controller.setTargetPosition(null, false);
                onStationLeft.run();
            }
            else{
                rocket.controller.enableMainEngines(true, false);
                rocket.controller.enableSecondaryEngines(false, false);
                rocket.controller.setRotationRateMultiplier(1, false);
                rocket.controller.setTargetFront(new Vec3(0,1,0), false);
                rocket.controller.setDefaultTargetHeading(moveAwayDirection, false);

                rocket.controller.setTargetPosition(rocket.position().add(moveAwayDirection.scale(100)), false);
            }

        }

        public void run(EntityRocket rocket, Runnable onCheckpointReached, Runnable onStationLeft) {
            if (!checkpointReached) {
                moveToCheckpoint(rocket, onCheckpointReached);
            } else {
                moveAway(rocket,onStationLeft);
            }
        }

        public void readFromNbt(CompoundTag nbt) {
            undockingDirection = Direction.values()[nbt.getInt("undockingDirection")];
            checkpointReached = nbt.getBoolean("checkpointReached");
            moveAwayDirection = Utils.deSerializeVec3(nbt.getCompound("moveAwayDirection"));
            dockingPosition = Utils.deSerializeVec3(nbt.getCompound("dockingPosition"));
        }

        public CompoundTag saveToNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("checkpointReached", checkpointReached);
            tag.putInt("undockingDirection", undockingDirection.ordinal());
            tag.put("moveAwayDirection", Utils.serializeVec3(moveAwayDirection));
            tag.put("dockingPosition", Utils.serializeVec3(dockingPosition));
            return tag;
        }

        public interface CheckpointReachedCallback{

        }
        public interface SpaceReachedCallback{

        }

    }
}
