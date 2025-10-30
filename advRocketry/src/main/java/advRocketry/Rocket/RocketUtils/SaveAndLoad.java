package advRocketry.Rocket.RocketUtils;

import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import advRocketry.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;

public class SaveAndLoad {


    public static void readAdditionalSaveData(EntityRocket rocket, CompoundTag compoundTag) {
        if(compoundTag.contains("rocketProgram")){
            rocket.currentProgram = RocketProgram.createFromNbt(compoundTag.getCompound("rocketProgram"));
        }

        rocket.lastLaunchPosition = NbtUtils.readBlockPos(compoundTag, "lastLaunchPosition").get();

        rocket.size = Utils.deSerializeVec3i(compoundTag.getCompound("size"));

        rocket.heading = Utils.deSerializeVec3(compoundTag.getCompound("heading"));
        rocket.targetHeading = Utils.deSerializeVec3(compoundTag.getCompound("targetHeading"));
        rocket.front = Utils.deSerializeVec3(compoundTag.getCompound("front"));
        rocket.targetFront = Utils.deSerializeVec3(compoundTag.getCompound("targetFront"));
        rocket.initialFront = Utils.deSerializeVec3(compoundTag.getCompound("initialFront"));

        rocket.fuelTank.readFromNBT(rocket.level().registryAccess(), compoundTag.getCompound("fuelTank"));

        rocket.blocks = new HashMap<>();
        ListTag blockTags = compoundTag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockTags.size(); i++) {
            CompoundTag blockTag = blockTags.getCompound(i);
            BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
            BlockState state = NbtUtils.readBlockState(rocket.level().registryAccess().lookupOrThrow(Registries.BLOCK), blockTag.getCompound("block"));
            rocket.blocks.put(p, state);
        }

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
    }

    public static void addAdditionalSaveData(EntityRocket rocket, CompoundTag compoundTag) {
        if(rocket.currentProgram != null){
            compoundTag.put("rocketProgram", RocketProgram.saveToNbt(rocket.currentProgram));
        }

        compoundTag.put("lastLaunchPosition", NbtUtils.writeBlockPos(rocket.lastLaunchPosition));

        compoundTag.put("size", Utils.serializeVec3i(rocket.size));

        compoundTag.put("heading", Utils.serializeVec3(rocket.heading));
        compoundTag.put("targetHeading", Utils.serializeVec3(rocket.targetHeading));
        compoundTag.put("front", Utils.serializeVec3(rocket.front));
        compoundTag.put("targetFront", Utils.serializeVec3(rocket.targetFront));
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
