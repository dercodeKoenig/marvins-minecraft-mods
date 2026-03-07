package advRocketry.Rocket.RocketPrograms;

import advRocketry.GlobalTime;
import advRocketry.Missions.RocketMission;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.units.qual.N;

import java.util.UUID;

// these programs are very simple, they just fly to space and start mission!
public class ProgramMissionStartBase implements RocketProgram {

    NavigateToSpaceTravelDimension navigateToSpaceTravelDimension;
    ResourceLocation returnLevel;
    BlockPos returnPos;
    UUID missionId;

    public ProgramMissionStartBase() {
        // empty constructor required for save & load
    }

    public ProgramMissionStartBase(EntityRocket rocket, ResourceLocation returnLevel, BlockPos returnPos, UUID missionId) {
        this.returnLevel = returnLevel;
        this.returnPos = returnPos;
        this.navigateToSpaceTravelDimension = new NavigateToSpaceTravelDimension(rocket);
        this.missionId = missionId;
    }

    public void startMission(EntityRocket rocket) {
        if(rocket.level().isClientSide)
            return;
        RocketMission mission = new RocketMission();
        mission.startMission(rocket, GlobalTime.getGlobalTime() + 20 * 10, missionId, returnLevel, returnPos);
    }

    @Override
    public void run(EntityRocket rocket) {
        if (navigateToSpaceTravelDimension.run(rocket, new NavigateToSpaceTravelDimension.SpaceReachedCallback() {
            @Override
            public boolean onSpaceReached() {
                startMission(rocket);
                return true;
            }
        })) {
            // this should not run
            // it would run if the program starts while the rocket is in space travel where it would not trigger the callback
            startMission(rocket);
        }
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        navigateToSpaceTravelDimension = new NavigateToSpaceTravelDimension();
        navigateToSpaceTravelDimension.readFromNbt(nbt);
        returnLevel = ResourceLocation.parse(nbt.getString("returnLevel"));
        returnPos = NbtUtils.readBlockPos(nbt, "returnPos").get();
        missionId = nbt.getUUID("missionId");
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("navigateToSpaceTravelDimension", navigateToSpaceTravelDimension.saveToNbt());
        tag.putString("returnLevel", returnLevel.toString());
        tag.put("returnPos", NbtUtils.writeBlockPos(returnPos));
        tag.putUUID("missionId", missionId);
        return tag;
    }
}
