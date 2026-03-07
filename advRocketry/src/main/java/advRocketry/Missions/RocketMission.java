package advRocketry.Missions;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.SpaceStationDimension;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketPrograms.ProgramNavigateToPlanetPosition;
import advRocketry.Rocket.RocketPrograms.ProgramNavigateToSpaceStation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

import static advRocketry.Registry.GeneralRegistry.ENTITY_ROCKET;

public class RocketMission {
    public long completeTime;
    CompoundTag rocketTag;
    UUID missionID;
    ResourceLocation returnLevelId;
    BlockPos returnPos;

    public void completeMission() {
        System.out.println("mission complete: " + missionID);
        restoreRocket();
    }

    public void startMission(EntityRocket rocket, long completeTime, UUID missionID, ResourceLocation landingLevel, BlockPos landingPos) {
        if (rocket.level().isClientSide)
            return;
        this.returnLevelId = landingLevel;
        this.returnPos = landingPos;
        this.missionID = missionID;
        this.completeTime = completeTime;
        rocketTag = new CompoundTag();
        rocket.addAdditionalSaveData(rocketTag);
        rocket.discard();
        MissionManager.missions.put(this.missionID, this);
        System.out.println("started mission " + missionID);
    }

    public EntityRocket restoreRocket() {
        Level level = DimensionManager.getServerLevel(returnLevelId);
        EntityRocket rocket = ENTITY_ROCKET.get().create(level); // <- level() in rocket should not be null
        rocket.readAdditionalSaveData(rocketTag);
        rocket.setDeltaMovement(0, 0, 0);

        Dimension returnDim = DimensionManager.INSTANCE_SERVER.get(returnLevelId);
        if (returnDim instanceof SpaceStationDimension spaceStationDimension) {
            // create the program
            ProgramNavigateToSpaceStation program = new ProgramNavigateToSpaceStation(rocket, returnLevelId, returnPos);
            // let the program find the spawn position
            program.teleportToStation(rocket);
            // set the program
            rocket.setProgramAndSync(program);
            System.out.println("restore rocket on space station dimension: " + returnLevelId);
        } else {
            // assume planet dimension
            ProgramNavigateToPlanetPosition program = new ProgramNavigateToPlanetPosition(rocket, returnLevelId, returnPos);
            // let the program find the spawn position
            program.teleportToPlanet(rocket);
            // set program
            rocket.setProgramAndSync(program);
            System.out.println("restore rocket on planet dimension: " + returnLevelId);
        }

        level.addFreshEntity(rocket);

        return rocket;
    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("rocket", rocketTag);
        tag.putLong("completeTime", completeTime);
        tag.putUUID("missionID", missionID);
        tag.putString("returnLevelId", returnLevelId.toString());
        tag.put("returnPos", NbtUtils.writeBlockPos(returnPos));
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        rocketTag = tag.getCompound("rocket");
        completeTime = tag.getLong("completeTime");
        missionID = tag.getUUID("missionID");
        returnLevelId = ResourceLocation.parse(tag.getString("returnLevelId"));
        returnPos = NbtUtils.readBlockPos(tag, "returnPos").get();
    }
}