package BetterPipes.Tank;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

public class EntityTank extends BlockEntity implements INetworkTagReceiver {


    public FluidTank myTank;
    TextureAtlasSprite spriteStill;
    int color;

    public EntityTank(BlockEntityType type, BlockPos p_155229_, BlockState p_155230_, int capacity) {
        super(type, p_155229_, p_155230_);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            updateSprites(Fluids.WATER);
        }
        myTank = new FluidTank(capacity) {
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
    }

    public EntityTank(BlockPos p_155229_, BlockState p_155230_) {
        this(ENTITY_TANK.get(), p_155229_, p_155230_, 10000);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityTank) t).tick();
    }

    public int forwardFillToAbove(FluidStack resource, IFluidHandler.FluidAction action) {
        BlockEntity entityAbove = level.getBlockEntity(getBlockPos().above());
        if (entityAbove instanceof EntityTank tankAbove) {
            return tankAbove.myTank.fill(resource, action);
        } else return 0;
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
                    int maxfill = 1000;
                    if ((FluidStack.isSameFluidSameComponents(otherTank.myTank.getFluid(), myTank.getFluid()) && otherTank.myTank.getFluidAmount() < otherTank.myTank.getCapacity()) || otherTank.myTank.isEmpty()) {
                        int toFill = Math.min(maxfill, otherTank.myTank.getCapacity() - otherTank.myTank.getFluidAmount());
                        otherTank.myTank.fill(myTank.drain(toFill, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }
    }

    public void syncTank(ServerPlayer target) {
        CompoundTag info = new CompoundTag();
        CompoundTag tankTag = new CompoundTag();
        myTank.writeToNBT(level.registryAccess(), tankTag);
        info.put("tankTag", tankTag);
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

    @Override
    public void readClient(CompoundTag compoundTag) {
        if (compoundTag.contains("tankTag")) {
            CompoundTag tankTag = compoundTag.getCompound("tankTag");
            myTank.readFromNBT(level.registryAccess(), tankTag);
            updateSprites(myTank.getFluid().getFluid());
        }
    }
}
