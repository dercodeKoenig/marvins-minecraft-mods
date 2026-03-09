package advRocketry.Rocket;

import advRocketry.Utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RocketSaveAndLoad {

    public static ListTag savePassengerPositions(Map<UUID, BlockPos > passengers){
        ListTag list = new ListTag();
        for (UUID uuid : passengers.keySet()){
            BlockPos pos = passengers.get(uuid);
            CompoundTag tag = new CompoundTag();
            tag.put("pos", Utils.serializeVec3i(pos));
            tag.putUUID("uuid", uuid);
            list.add(tag);
        }
        return list;
    }

    public static HashMap<UUID, BlockPos >  readPassengerPositions(ListTag list){
        HashMap<UUID, BlockPos > passengers = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Vec3i pos = Utils.deSerializeVec3i(tag.getCompound("pos"));
            BlockPos blockPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
            UUID uuid = tag.getUUID("uuid");
            passengers.put(uuid, blockPos);
        }
        return passengers;
    }


    public static void readAdditionalSaveData(EntityRocket rocket, CompoundTag compoundTag) {

        boolean needsUpdateGuiModules = false; // when BlockEntities change or are initially loaded
        boolean needsUpdateStructure = false; // when Blocks change or are initially loaded

        boolean isInitialLoad = compoundTag.contains("initialLoad");

        // it does not correctly sync movement / position after dimension change, so i do it myself
        if(compoundTag.contains("deltaMovement")) {
            rocket.setDeltaMovement(Utils.deSerializeVec3(compoundTag.getCompound("deltaMovement")));
            rocket.lerpDeltaMovementSteps = 0;
        }
        if(compoundTag.contains("position")) {
            rocket.setPos(Utils.deSerializeVec3(compoundTag.getCompound("position")));
            rocket.lerpSteps = 0;
        }


        if(compoundTag.contains("passengers"))
            rocket.passengers = readPassengerPositions(compoundTag.getList("passengers", Tag.TAG_COMPOUND));

        if(compoundTag.contains("universePosition"))
            rocket.universePosition =  Utils.deSerializeVec3(compoundTag.getCompound("universePosition"));
        if(compoundTag.contains("universeFront"))
            rocket.universeFront =  Utils.deSerializeVec3(compoundTag.getCompound("universeFront"));
        if(compoundTag.contains("universeHeading"))
            rocket.universeHeading =  Utils.deSerializeVec3(compoundTag.getCompound("universeHeading"));

        if (compoundTag.contains("canUseMainEngines"))
            rocket.controller.enableMainEngines(compoundTag.getBoolean("canUseMainEngines"),true);
        if (compoundTag.contains("mainEnginesBootup"))
            rocket.controller.setMainEnginesBootup(compoundTag.getInt("mainEnginesBootup"), true);
        if (compoundTag.contains("secondaryEngines"))
            rocket.controller.enableSecondaryEngines(compoundTag.getBoolean("secondaryEngines"), true);

        if(compoundTag.contains("rotationRateMultiplier"))
            rocket.controller.setRotationRateMultiplier(compoundTag.getDouble("rotationRateMultiplier"),true);

        if (compoundTag.contains("currentProgram")) {
            rocket.setProgramAndSync(ProgramRegistry.createFromNbt(compoundTag.getCompound("currentProgram")));
        }

        if (compoundTag.contains("dockingStationPos")) {
            Vec3i dockingStationPos = Utils.deSerializeVec3i(compoundTag.getCompound("dockingStationPos"));
            rocket.setDockingStationPos(dockingStationPos, true);
        }

        if (compoundTag.contains("size")) {
            rocket.size = Utils.deSerializeVec3i(compoundTag.getCompound("size"));
            rocket.refreshDimensions();
        }

        if(compoundTag.contains("targetPosition"))
            rocket.controller.setTargetPosition(Utils.deSerializeVec3(compoundTag.getCompound("targetPosition")), false);

        if (compoundTag.contains("heading"))
            rocket.controller.heading = Utils.deSerializeVec3(compoundTag.getCompound("heading"));
        if (compoundTag.contains("heading") && isInitialLoad)
            rocket.controller.lazyHeading = rocket.controller.heading;
        if (compoundTag.contains("defaultTargetHeading"))
            rocket.controller.setDefaultTargetHeading(Utils.deSerializeVec3(compoundTag.getCompound("defaultTargetHeading")), true);
        if (compoundTag.contains("front"))
            rocket.controller.front = Utils.deSerializeVec3(compoundTag.getCompound("front"));
        if (compoundTag.contains("front") && isInitialLoad)
            rocket.controller.lazyFront = rocket.controller.front;
        if (compoundTag.contains("targetFront"))
            rocket.controller.setTargetFront(Utils.deSerializeVec3(compoundTag.getCompound("targetFront")), true);
        if (compoundTag.contains("initialFront"))
            rocket.initialFront = Utils.deSerializeVec3(compoundTag.getCompound("initialFront"));

        if (compoundTag.contains("fuelCapacity")) {
            rocket.fuelTank.setCapacity(compoundTag.getInt("fuelCapacity"));
        }
        if (compoundTag.contains("fuelTank")) {
            rocket.fuelTank.readFromNBT(rocket.level().registryAccess(), compoundTag.getCompound("fuelTank"));
        }

        if (compoundTag.contains("blocks")) {
            rocket.blocks = new HashMap<>();
            ListTag blockTags = compoundTag.getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blockTags.size(); i++) {
                CompoundTag blockTag = blockTags.getCompound(i);
                BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
                BlockState state = NbtUtils.readBlockState(rocket.level().registryAccess().lookupOrThrow(Registries.BLOCK), blockTag.getCompound("block"));
                rocket.blocks.put(p, state);
            }
            needsUpdateStructure = true;
        }

        if (compoundTag.contains("blockEntities")) {
            rocket.blockEntities = new HashMap<>();
            ListTag blockEntityTags = compoundTag.getList("blockEntities", Tag.TAG_COMPOUND);
            for (int i = 0; i < blockEntityTags.size(); i++) {
                CompoundTag blockTag = blockEntityTags.getCompound(i);
                BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
                BlockState state = rocket.blocks.get(p);
                BlockEntity be = ((EntityBlock) state.getBlock()).newBlockEntity(p, state);
                be.loadCustomOnly(blockTag.getCompound("blockEntity"), rocket.registryAccess());
                rocket.blockEntities.put(p, be);
            }
            needsUpdateGuiModules = true; // gui depends on block entities
            needsUpdateStructure = true;
        }

        if(needsUpdateGuiModules)
            rocket.makeGui();
        if(needsUpdateStructure){
            rocket.setStructureChanged();
        }
    }

    public static void addAdditionalSaveData(EntityRocket rocket, CompoundTag compoundTag) {

        // it does not correctly sync movement / position after dimension change, so i do it myself
        compoundTag.put("deltaMovement", Utils.serializeVec3(rocket.getDeltaMovement()));
        compoundTag.put("position", Utils.serializeVec3(rocket.position()));

        compoundTag.put("passengers", savePassengerPositions(rocket.passengers));

        compoundTag.put("universePosition", Utils.serializeVec3(rocket.universePosition));
        compoundTag.put("universeFront", Utils.serializeVec3(rocket.universeFront));
        compoundTag.put("universeHeading", Utils.serializeVec3(rocket.universeHeading));

        compoundTag.putBoolean("canUseMainEngines", rocket.controller.canUseMainEngines());
        compoundTag.putInt("mainEnginesBootup", rocket.controller.getMainEnginesBootUp());
        compoundTag.putBoolean("secondaryEngines", rocket.controller.canUseSecondaryEngines());

        compoundTag.putDouble("rotationRateMultiplier", rocket.controller.getRotationRateMultiplier());

        compoundTag.put("currentProgram", ProgramRegistry.saveToNbt(rocket.getCurrentProgram()));

        compoundTag.put("dockingStationPos", Utils.serializeVec3i(rocket.getDockingStationPos()));

        compoundTag.put("size", Utils.serializeVec3i(rocket.size));

        compoundTag.put("targetPosition", Utils.serializeVec3(rocket.controller.getTargetPosition()));

        compoundTag.put("heading", Utils.serializeVec3(rocket.controller.heading));
        compoundTag.put("defaultTargetHeading", Utils.serializeVec3(rocket.controller.getDefaultTargetHeading()));
        compoundTag.put("front", Utils.serializeVec3(rocket.controller.front));
        compoundTag.put("targetFront", Utils.serializeVec3(rocket.controller.getTargetFront()));
        compoundTag.put("initialFront", Utils.serializeVec3(rocket.initialFront));

        compoundTag.putInt("fuelCapacity", rocket.fuelTank.getCapacity());
        compoundTag.put("fuelTank", rocket.fuelTank.writeToNBT(rocket.level().registryAccess(), new CompoundTag()));

        ListTag blockTags = new ListTag(rocket.blocks.size());
        for (BlockPos i : rocket.blocks.keySet()) {
            BlockState state = rocket.blocks.get(i);
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("blockPos", NbtUtils.writeBlockPos(i));
            blockTag.put("block", NbtUtils.writeBlockState(state));
            blockTags.add(blockTag);
        }
        compoundTag.put("blocks", blockTags);

        ListTag blockEntityTags = new ListTag(rocket.blockEntities.size());
        for (BlockPos i : rocket.blockEntities.keySet()) {
            BlockEntity blockEntity = rocket.blockEntities.get(i);
            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.put("blockPos", NbtUtils.writeBlockPos(i));
            blockEntityTag.put("blockEntity", blockEntity.saveCustomOnly(rocket.registryAccess()));
            blockEntityTags.add(blockEntityTag);
        }
        compoundTag.put("blockEntities", blockEntityTags);

    }

}
