package advRocketry.Rocket.RocketPrograms;

import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;

// these programs are very simple, they just fly to space and start mission!
public class ProgramMissionStart implements RocketProgram {

    NavigateToSpaceTravelDimension navigateToSpaceTravelDimension;
    ResourceLocation returnLevel;
    BlockPos returnPos;


    public ProgramMissionStart(EntityRocket rocket, ResourceLocation returnLevel, BlockPos returnPos) {
        this.returnLevel = returnLevel;
        this.returnPos = returnPos;
        this.navigateToSpaceTravelDimension = new NavigateToSpaceTravelDimension(rocket);
    }

    public void startMission() {

    }

    @Override
    public void run(EntityRocket rocket) {
        if (navigateToSpaceTravelDimension.run(rocket, new NavigateToSpaceTravelDimension.SpaceReachedCallback() {
            @Override
            public boolean onSpaceReached() {
                startMission();
                return true;
            }
        })) {

        }
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        navigateToSpaceTravelDimension.readFromNbt(nbt);
        returnLevel = ResourceLocation.parse(nbt.getString("returnLevel"));
        returnPos = NbtUtils.readBlockPos(nbt, "returnPos").get();
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("navigateToSpaceTravelDimension", navigateToSpaceTravelDimension.saveToNbt());
        tag.putString("returnLevel", returnLevel.toString());
        tag.put("returnPos", NbtUtils.writeBlockPos(returnPos));
        return tag;
    }
}
