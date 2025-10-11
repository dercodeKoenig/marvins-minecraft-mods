package advRocketry.Rocket;

import ARLib.gui.GuiHandlerEntity;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class EntityRocket extends Entity implements INetworkTagReceiver {

    public Map<BlockPos, BlockState> blocks = new HashMap<>();
    public Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    public Vec3i size = new Vec3i(0, 0, 0);
    public ItemStack navigationItem = ItemStack.EMPTY; // the one from the guidance computer
    public ItemStack usedNavigationItem = ItemStack.EMPTY; // the current one used (guidance computer item can be overwritten in launch terminal)

    public GuiHandlerEntity guiHandler;

    public EntityRocket(EntityType<?> entityType, Level level) {
        super(entityType, level);
        guiHandler = new GuiHandlerEntity(this);
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
    public boolean isInvulnerableTo(DamageSource source) {
        return true; // Rocket cannot be damaged
    }

    @Override
    public AABB makeBoundingBox() {
        if(size == null)return super.makeBoundingBox(); // happens because minecraft calls makeBoundingBox in constructor before the size value is assigned
        return new AABB(
                position().x - (double) size.getX() / 2,
                position().y,
                position().z - (double) size.getZ() / 2,
                position().x + (double) size.getX() / 2,
                position().y + size.getY(),
                position().z + (double) size.getZ() / 2
        );
    }

    public void openGui() {
        if (level().isClientSide) {
            guiHandler.openGui(200, 200, true);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        openGui();
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }


    @Override
    public void tick() {
        if (!level().isClientSide) {
            guiHandler.serverTick();
        }
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

        blockEntities = new HashMap<>();
        ListTag blockEntityTags = compoundTag.getList("blockEntities", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockEntityTags.size(); i++) {
            CompoundTag blockTag = blockEntityTags.getCompound(i);
            BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
            BlockState state = blocks.get(p);
            BlockEntity be = ((EntityBlock)state.getBlock()).newBlockEntity(p,state);
            be.loadCustomOnly(blockTag.getCompound("blockEntity"), registryAccess());
            blockEntities.put(p, be);
        }

        if (compoundTag.contains("navigationItem"))
            navigationItem = ItemStack.parse(registryAccess(), compoundTag.getCompound("navigationItem")).get();
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

        ListTag blockEntityTags = new ListTag(blockEntities.size());
        for (BlockPos i : blockEntities.keySet()) {
            BlockEntity blockEntity = blockEntities.get(i);
            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.put("blockPos", NbtUtils.writeBlockPos(i));
            blockEntityTag.put("blockEntity", blockEntity.saveCustomOnly(registryAccess()));
            blockEntityTags.add(blockEntityTag);
        }
        compoundTag.put("blockEntities", blockEntityTags);

        if (navigationItem != ItemStack.EMPTY)
            compoundTag.put("navigationItem", navigationItem.save(registryAccess()));
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
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
        guiHandler.readClient(compoundTag);
        if (compoundTag.contains("additionalSaveData"))
            readAdditionalSaveData(compoundTag.getCompound("additionalSaveData"));
    }
}
