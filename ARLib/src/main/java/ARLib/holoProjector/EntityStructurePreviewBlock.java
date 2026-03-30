package ARLib.holoProjector;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import com.mojang.serialization.DataResult;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ARLib.ARLibRegistry.ENTITY_STRUCTURE_PREVIEW;


public class EntityStructurePreviewBlock extends BlockEntity implements INetworkTagReceiver {
    int maxLifeTime = 20 * 60 * 20;
    long last_sec = 0;
    int i = 0;
    int ticksExisted = 0;
    private List<Block> validBlocks = new ArrayList<>();
    public EntityStructurePreviewBlock(BlockPos pos, BlockState blockState) {
        super(ENTITY_STRUCTURE_PREVIEW.get(), pos, blockState);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityStructurePreviewBlock) t).tick();
    }

    public void setValidBlocks(List<Block> validBlocks) {
        this.validBlocks.addAll(validBlocks);
        if (!level.isClientSide) {
            CompoundTag info = new CompoundTag();
            saveAdditional(info, level.registryAccess());
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    public Block getBlockToRender() {
        long sec = System.currentTimeMillis() / 1000;
        if (sec != last_sec) {
            last_sec = sec;
            i += 1;
        }

        if (i >= validBlocks.size()) {
            i = 0;
        }

        if (validBlocks.isEmpty()) {
            return Blocks.AIR;
        }

        return validBlocks.get(i);
    }

    @Override
    public void onLoad() {
        if (level.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.put("ping", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Save the validBlocks list to NBT
        ListTag blockListTag = new ListTag();
        for (Block block : validBlocks) {
            DataResult<CompoundTag> encodedBlockState = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, block.defaultBlockState()).map((nbtTag) -> (CompoundTag) nbtTag);
            CompoundTag bt = new CompoundTag();
            bt.put("block", encodedBlockState.getOrThrow());
            blockListTag.add(bt);
        }
        tag.put("ValidBlocks", blockListTag);
        tag.putInt("ticks", ticksExisted);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Load the validBlocks list from NBT
        validBlocks.clear();
        if (tag.contains("ValidBlocks")) {
            ListTag blockListTag = tag.getList("ValidBlocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blockListTag.size(); i++) {
                CompoundTag blockStateNBT = blockListTag.getCompound(i).getCompound("block");
                DataResult<BlockState> decodedBlockState = BlockState.CODEC.parse(NbtOps.INSTANCE, blockStateNBT);
                Block b = decodedBlockState.getOrThrow().getBlock();
                validBlocks.add(b);
            }
        }
        if (tag.contains("ticks"))
            ticksExisted = tag.getInt("ticks");
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {
        if (compoundTag.contains("ping")) {
            CompoundTag response = new CompoundTag();
            saveAdditional(response, level.registryAccess());
            PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, response));
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        loadAdditional(compoundTag, level.registryAccess());
    }

    public void tick() {
        if (!level.isClientSide) {
            ticksExisted++;
            if (ticksExisted > maxLifeTime) {
                level.setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

}
