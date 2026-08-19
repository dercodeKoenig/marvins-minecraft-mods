package advRocketry.Rocket.RocketPrograms;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.SpaceStationDimension;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class ProgramTestFlight extends ProgramMissionStartBase {

    public ProgramTestFlight(){

    }

    public ProgramTestFlight(EntityRocket rocket, ResourceLocation returnLevel, BlockPos returnPos, UUID missionId) {
        super(rocket, returnLevel, returnPos, missionId);
    }

    public void startMission(EntityRocket rocket) {
        // only test program, fly high up, then return
        Dimension returnDim = DimensionManager.INSTANCE_SERVER.get(returnLevel);
        if(returnDim instanceof SpaceStationDimension){
            ProgramNavigateToSpaceStation program = new ProgramNavigateToSpaceStation(rocket, returnLevel, returnPos);
            rocket.setProgramAndSync(program);
        }else{
            ProgramNavigateToPlanetPosition program = new ProgramNavigateToPlanetPosition(rocket, returnLevel, returnPos);
            rocket.setProgramAndSync(program);
        }
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        super.readFromNbt(nbt);
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = super.saveToNbt();
        return tag;
    }
}
