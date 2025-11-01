package BetterPipes.Tank;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.network.PacketDistributor;

import static BetterPipes.Registry.ENTITY_TANK;
import static net.minecraft.client.renderer.RenderType.TRANSIENT_BUFFER_SIZE;

// the pump will take the water block that is most away on the highest connected y level

public class EntityTank extends BlockEntity implements INetworkTagReceiver {


    TextureAtlasSprite spriteStill;
    int color;
    boolean requiresMeshUpdate = false;
    VertexBuffer vertexBuffer;
    ByteBufferBuilder myByteBuffer;
    MeshData mesh;
    int lastLight;

    public FluidTank myTank = new FluidTank(10000) {
        @Override
        public void onContentsChanged() {
            setChanged();
            if (!level.isClientSide) {
                syncTank(null);
            }
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            int toFill = resource.getAmount();
            int filled = super.fill(resource, action);
            int remaining = toFill - filled;
            int filledAbove = 0;
            if (remaining > 0) {
                filledAbove = forwardFillToAbove(resource.copyWithAmount(remaining), action);
            }
            int totalFilled = filled + filledAbove;
            return totalFilled;
        }
    };

    public int forwardFillToAbove(FluidStack resource, IFluidHandler.FluidAction action){
        BlockEntity entityAbove = level.getBlockEntity(getBlockPos().above());
        if(entityAbove instanceof EntityTank tankAbove){
            return tankAbove.myTank.fill(resource,action);
        }
        else return 0;
    }

    public EntityTank(BlockPos p_155229_, BlockState p_155230_) {
        super(ENTITY_TANK.get(), p_155229_, p_155230_);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                myByteBuffer = new ByteBufferBuilder(TRANSIENT_BUFFER_SIZE);
            });
            updateSprites(Fluids.WATER);
        }
    }

    @Override
    public void setRemoved(){
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer .close();
                myByteBuffer.close();
            });
        }
    }
    @Override
    public void onLoad() {
        if (level.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.put("client_onload", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }


    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myTank.readFromNBT(registries, tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myTank.writeToNBT(registries, tag);
    }

    public void tick() {
        if (!level.isClientSide) {
            if (myTank.getFluidAmount() > 0 && getBlockState().getValue(BlockTank.connectedBelow)) {
                BlockEntity other = level.getBlockEntity(getBlockPos().below());
                if (other instanceof EntityTank otherTank) {
                    int maxfill = 100;
                    if ((FluidStack.isSameFluidSameComponents(otherTank.myTank.getFluid(), myTank.getFluid()) && otherTank.myTank.getFluidAmount() < otherTank.myTank.getCapacity())|| otherTank.myTank.isEmpty()) {
                        int toFill = Math.min(maxfill, otherTank.myTank.getCapacity() - otherTank.myTank.getFluidAmount());
                        otherTank.myTank.fill(myTank.drain(toFill, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityTank) t).tick();
    }

    public void syncTank(ServerPlayer target) {
        CompoundTag info = new CompoundTag();
        CompoundTag tankTag = new CompoundTag();
        myTank.writeToNBT(level.registryAccess(), tankTag);
        info.put("tankTag", tankTag);
        info.putLong("time", System.nanoTime());
        if (target == null)
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
        else
            PacketDistributor.sendToPlayer(target, PacketBlockEntity.getBlockEntityPacket(this, info));
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        if (compoundTag.contains("client_onload")) {
            syncTank(serverPlayer);
        }
    }


    public void updateSprites(Fluid f) {
        if (f == Fluids.EMPTY) f = Fluids.WATER;
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(f);
        color = extensions.getTintColor();
        ResourceLocation fluidtextureStill = extensions.getStillTexture();
        spriteStill = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidtextureStill);
    }

    long lastTankSync = Long.MIN_VALUE; // this should avoid rare cases of new packets arrive before old ones and messing up data (maybe this can happen on slow ping)
    @Override
    public void readClient(CompoundTag compoundTag) {
        if (compoundTag.contains("tankTag") && compoundTag.contains("time")) {
            long newTime = compoundTag.getLong("time");

            // Detect a server reset by checking for an abnormally large time drop
            if (newTime < lastTankSync - 1_000_000_000L) { // 1 second in nanoseconds
                lastTankSync = Long.MIN_VALUE; // Reset so we start accepting new updates again
            }

            if (newTime > lastTankSync) {
                lastTankSync = newTime;
                CompoundTag tankTag = compoundTag.getCompound("tankTag");
                myTank.readFromNBT(level.registryAccess(), tankTag);
                updateSprites(myTank.getFluid().getFluid());
                requiresMeshUpdate = true;

                // Update connected tanks
                if (getBlockState().getValue(BlockTank.connectedBelow)) {
                    BlockEntity other = level.getBlockEntity(getBlockPos().below());
                    if (other instanceof EntityTank otherTank) {
                        otherTank.requiresMeshUpdate = true;
                    }
                }
                if (getBlockState().getValue(BlockTank.connectedAbove)) {
                    BlockEntity other = level.getBlockEntity(getBlockPos().above());
                    if (other instanceof EntityTank otherTank) {
                        otherTank.requiresMeshUpdate = true;
                    }
                }
            }
        }
    }
}
