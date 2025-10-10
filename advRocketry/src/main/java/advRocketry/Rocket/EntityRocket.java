package advRocketry.Rocket;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.network.PacketEntity;
import advRocketry.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class EntityRocket extends Entity implements INetworkTagReceiver {

    public Map<BlockPos, BlockState> blocks = new HashMap<>();
    public Vec3i size = new Vec3i(0, 0, 0);
    public ItemStack navigationChip = ItemStack.EMPTY;

    public EntityRocket(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void onAddedToLevel() {
        if (level().isClientSide) {
            CompoundTag req = new CompoundTag();
            req.putInt("ping", 0);
            PacketDistributor.sendToServer(PacketEntity.getEntityPacket(this, req));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        CompoundTag sizeTag = compoundTag.getCompound("size");
        size = new Vec3i(sizeTag.getInt("x"), sizeTag.getInt("y"), sizeTag.getInt("z"));

        blocks = new HashMap<>();
        ListTag blockTags = compoundTag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockTags.size(); i++) {
            CompoundTag blockTag = blockTags.getCompound(i);
            BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
            BlockState state = NbtUtils.readBlockState(level().registryAccess().lookupOrThrow(Registries.BLOCK), blockTag.getCompound("block"));
            blocks.put(p, state);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        CompoundTag sizeTag = new CompoundTag();
        sizeTag.putInt("x", size.getX());
        sizeTag.putInt("y", size.getY());
        sizeTag.putInt("z", size.getZ());
        compoundTag.put("size", sizeTag);

        ListTag blockTags = new ListTag(blocks.size());
        for (BlockPos i : blocks.keySet()) {
            BlockState state = blocks.get(i);
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("blockPos", NbtUtils.writeBlockPos(i));
            blockTag.put("block", NbtUtils.writeBlockState(state));
            blockTags.add(blockTag);
        }
        compoundTag.put("blocks", blockTags);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        if (compoundTag.contains("ping")) {
            CompoundTag additionalSaveData = new CompoundTag();
            addAdditionalSaveData(additionalSaveData);
            CompoundTag info = new CompoundTag();
            info.put("additionalSaveData", additionalSaveData);
            PacketDistributor.sendToPlayer(serverPlayer, PacketEntity.getEntityPacket(this, info));
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        if (compoundTag.contains("additionalSaveData"))
            readAdditionalSaveData(compoundTag.getCompound("additionalSaveData"));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true; // Rocket cannot be damaged
    }
}
