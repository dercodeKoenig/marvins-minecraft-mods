package advRocketry.Rocket.RocketPrograms;

import advRocketry.Rocket.EntityRocket;
import advRocketry.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class StationDockingProgram {
    public  static class DockingProgram {
        public Vec3 dockingPosition;
        // up, down, side
        public Direction dockingDirection;
        public boolean checkpointReached = false;

        public DockingProgram(Vec3 targetPosition, Direction dockingDirection) {
            this.dockingDirection = dockingDirection;
            this.dockingPosition = targetPosition;
        }

        void moveToCheckpoint(EntityRocket rocket) {
            Vec3 checkpointPos = dockingPosition.add(
                    new Vec3(
                            dockingDirection.getStepX(),
                            dockingDirection.getStepY(),
                            dockingDirection.getStepZ()
                    ).scale(10+rocket.size.getY())
            );

            rocket.enableMainEngines(true, false);
            rocket.enableSecondaryEngines(true, false);
            rocket.setRotationRateMultiplier(1, false);

            Vec3 toTarget = checkpointPos.subtract(rocket.position());

            Vec3 scaledToTarget = toTarget.scale(0.5);
            double maxD = 100;
            if (scaledToTarget.length() > maxD) {
                scaledToTarget = scaledToTarget.normalize().scale(maxD);
            }
            Vec3 targetVec3 = rocket.position().add(scaledToTarget);

            rocket.setTargetPosition(targetVec3, false);

            // check if stopped (at target or collision maybe)
            if (rocket.getDeltaMovement().length() < 0.01 && toTarget.length() < 5) {
                checkpointReached = true;
                System.out.println("checkpoint reached");
            }
        }


        void moveToDockingStation(EntityRocket rocket) {

            rocket.enableMainEngines(false, false);
            rocket.enableSecondaryEngines(true, false);
            rocket.setRotationRateMultiplier(0.2, false);
            rocket.setDefaultTargetHeading(new Vec3(0,1,0), false);

            Vec3 toTarget = dockingPosition.subtract(rocket.position());
            rocket.setTargetFront(toTarget, false);

            Vec3 scaledToTarget = new Vec3(toTarget.x, toTarget.y / 2, toTarget.z);
            Vec3 targetVec3 = rocket.position().add(scaledToTarget);

            rocket.setTargetPosition(targetVec3, false);

            if (rocket.getDeltaMovement().length() < 0.01 && toTarget.length() < 0.1) {
                rocket.setDeltaMovement(0,0,0);
                if(!rocket.level().isClientSide)
                    rocket.setPos(dockingPosition);
                rocket.endProgram();
            }
        }

        public void run(EntityRocket rocket){
             if(!checkpointReached){
                 moveToCheckpoint(rocket);
             }else{
                 moveToDockingStation(rocket);
             }
        }

        public void readFromNbt(CompoundTag nbt) {
            dockingDirection = Direction.values()[nbt.getInt("dockingDirection")];
            dockingPosition = Utils.deSerializeVec3(nbt.getCompound("dockingPosition"));
            checkpointReached = nbt.getBoolean("checkpointReached");
        }

        public CompoundTag saveToNbt() {
            CompoundTag tag = new CompoundTag();
            tag.put("dockingPosition", Utils.serializeVec3(dockingPosition));
            tag.putInt("dockingDirection", dockingDirection.ordinal());
            tag.putBoolean("checkpointReached", checkpointReached);
            return tag;
        }
    }

    public static class UnDockingProgram {
        // up, down, side
        public int undockingDirection = 0;
        public boolean checkpointReached = false;
        public Vec3 moveAwayDirection = Vec3.ZERO;

        public UnDockingProgram(int undockingDirection, Vec3 moveAwayDirection) {
            this.undockingDirection = undockingDirection;
            this.moveAwayDirection = moveAwayDirection;
        }

        public void readFromNbt(CompoundTag nbt) {
            undockingDirection = nbt.getInt("undockingDirection");
            checkpointReached = nbt.getBoolean("checkpointReached");
            moveAwayDirection = Utils.deSerializeVec3(nbt.getCompound("moveAwayDirection"));
        }

        public CompoundTag saveToNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("checkpointReached", checkpointReached);
            tag.putInt("undockingDirection", undockingDirection);
            tag.put("moveAwayDirection", Utils.serializeVec3(moveAwayDirection));
            return tag;
        }
    }
}
