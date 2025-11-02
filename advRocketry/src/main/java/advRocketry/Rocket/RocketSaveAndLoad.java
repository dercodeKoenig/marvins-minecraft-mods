package advRocketry.Rocket;

import advRocketry.utils.Utils;
import it.unimi.dsi.fastutil.Hash;
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

        boolean needsUpdateGuiModules = false;

        if(compoundTag.contains("passengers")){
            rocket.passengers = readPassengerPositions(compoundTag.getList("passengers", Tag.TAG_COMPOUND));
        }

        if(compoundTag.contains("universePosition"))
            rocket.universePosition =  Utils.deSerializeVec3(compoundTag.getCompound("universePosition"));

        if (compoundTag.contains("canUseMainEngines"))
            rocket.enableMainEngines(compoundTag.getBoolean("canUseMainEngines"),true);

        if (compoundTag.contains("mainEnginesBootup"))
            rocket.setMainEnginesBootup(compoundTag.getInt("mainEnginesBootup"), true);
        if (compoundTag.contains("secondaryEngines"))
            rocket.enableSecondaryEngines(compoundTag.getBoolean("secondaryEngines"), true);

        if (compoundTag.contains("currentProgram")) {
            rocket.setProgramAndSync(RocketProgram.createFromNbt(compoundTag.getCompound("currentProgram")));
        }

        if (compoundTag.contains("lastLaunchPosition")) {
            Vec3i lastLaunchPosition = Utils.deSerializeVec3i(compoundTag.getCompound("lastLaunchPosition"));
            rocket.setLastLaunchPosition(new BlockPos(lastLaunchPosition.getX(), lastLaunchPosition.getY(), lastLaunchPosition.getZ()), true);
        }

        if (compoundTag.contains("size")) {
            rocket.size = Utils.deSerializeVec3i(compoundTag.getCompound("size"));
            rocket.refreshDimensions();
        }

        if(compoundTag.contains("targetPosition"))
            rocket.setTargetPosition(Utils.deSerializeVec3(compoundTag.getCompound("targetPosition")), false);

        if (compoundTag.contains("heading"))
            rocket.heading = Utils.deSerializeVec3(compoundTag.getCompound("heading"));
        if (compoundTag.contains("defaultTargetHeading"))
            rocket.setDefaultTargetHeading(Utils.deSerializeVec3(compoundTag.getCompound("defaultTargetHeading")), true);
        if (compoundTag.contains("front"))
            rocket.front = Utils.deSerializeVec3(compoundTag.getCompound("front"));
        if (compoundTag.contains("targetFront"))
            rocket.setTargetFront(Utils.deSerializeVec3(compoundTag.getCompound("targetFront")), true);
        if (compoundTag.contains("initialFront"))
            rocket.initialFront = Utils.deSerializeVec3(compoundTag.getCompound("initialFront"));

        if (compoundTag.contains("fuelTank"))
            rocket.fuelTank.readFromNBT(rocket.level().registryAccess(), compoundTag.getCompound("fuelTank"));

        if (compoundTag.contains("blocks")) {
            rocket.blocks = new HashMap<>();
            ListTag blockTags = compoundTag.getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blockTags.size(); i++) {
                CompoundTag blockTag = blockTags.getCompound(i);
                BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
                BlockState state = NbtUtils.readBlockState(rocket.level().registryAccess().lookupOrThrow(Registries.BLOCK), blockTag.getCompound("block"));
                rocket.blocks.put(p, state);
            }
            rocket.requiresMeshUpdate = true;
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
            rocket.requiresMeshUpdate = true;
        }

        if(needsUpdateGuiModules)
            rocket.makeGui();
    }

    public static void addAdditionalSaveData(EntityRocket rocket, CompoundTag compoundTag) {
        compoundTag.put("passengers", savePassengerPositions(rocket.passengers));

        compoundTag.put("universePosition", Utils.serializeVec3(rocket.universePosition));

        compoundTag.putBoolean("canUseMainEngines", rocket.canUseMainEngines());
        compoundTag.putInt("mainEnginesBootup", rocket.getMainEnginesBootUp());
        compoundTag.putBoolean("secondaryEngines", rocket.canUseSecondaryEngines());

        compoundTag.put("currentProgram", RocketProgram.saveToNbt(rocket.getCurrentProgram()));

        compoundTag.put("lastLaunchPosition", Utils.serializeVec3i(rocket.getLastLaunchPosition()));

        compoundTag.put("size", Utils.serializeVec3i(rocket.size));

        compoundTag.put("targetPosition", Utils.serializeVec3(rocket.getTargetPosition()));

        compoundTag.put("heading", Utils.serializeVec3(rocket.heading));
        compoundTag.put("defaultTargetHeading", Utils.serializeVec3(rocket.getDefaultTargetHeading()));
        compoundTag.put("front", Utils.serializeVec3(rocket.front));
        compoundTag.put("targetFront", Utils.serializeVec3(rocket.getTargetFront()));
        compoundTag.put("initialFront", Utils.serializeVec3(rocket.initialFront));

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
