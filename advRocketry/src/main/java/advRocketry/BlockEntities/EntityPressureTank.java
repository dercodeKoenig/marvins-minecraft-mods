package advRocketry.BlockEntities;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import BetterPipes.Tank.EntityTank;
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

import static advRocketry.Registry.BlockEntities.ENTITY_PRESSURE_TANK;

public class EntityPressureTank extends BlockEntity implements INetworkTagReceiver {


    public FluidTank tank;
    public TextureAtlasSprite spriteStill;
    public int color;

    public EntityPressureTank(BlockEntityType type, BlockPos p_155229_, BlockState p_155230_, int capacity) {
        super(type, p_155229_, p_155230_);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            updateSprites(Fluids.WATER);
        }
        tank = new FluidTank(capacity) {
            @Override
            public void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide) {
                    syncTank(null);
                }
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
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

    public EntityPressureTank(BlockPos p_155229_, BlockState p_155230_) {
        this(ENTITY_PRESSURE_TANK.get(), p_155229_, p_155230_, 100000);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityPressureTank) t).tick();
    }

    public int forwardFillToAbove(FluidStack resource, IFluidHandler.FluidAction action) {
        if(level == null) return 0; // can be null when in rocket
        BlockEntity entityAbove = level.getBlockEntity(getBlockPos().above());
        if (entityAbove instanceof EntityPressureTank tankAbove) {
            return tankAbove.tank.fill(resource, action);
        } else return 0;
    }

    @Override
    public void onLoad() {
        if (level.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.put("onLoad", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("tank"))
            tank.readFromNBT(registries, tag.getCompound("tank"));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(registries, tankTag);
        tag.put("tank", tankTag);
    }

    public void tick() {
        if (!level.isClientSide) {

            // drain into tank below
            if (tank.getFluidAmount() > 0) {
                if (level.getBlockEntity(getBlockPos().below()) instanceof EntityPressureTank otherTank) {
                    int maxfill = 1000;
                    if (
                            (FluidStack.isSameFluidSameComponents(otherTank.tank.getFluid(), tank.getFluid()) && otherTank.tank.getFluidAmount() < otherTank.tank.getCapacity()) ||
                                    otherTank.tank.isEmpty()
                    ) {
                        int toFill = Math.min(maxfill, otherTank.tank.getCapacity() - otherTank.tank.getFluidAmount());
                        otherTank.tank.fill(tank.drain(toFill, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }
    }

    public void syncTank(ServerPlayer target) {
        CompoundTag info = new CompoundTag();
        saveAdditional(info, level.registryAccess());
        if (target == null)
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
        else
            PacketDistributor.sendToPlayer(target, PacketBlockEntity.getBlockEntityPacket(this, info));
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        if (compoundTag.contains("onLoad")) {
            syncTank(serverPlayer);
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        loadAdditional(compoundTag, level.registryAccess());
        updateSprites(tank.getFluid().getFluid());
    }

    public void updateSprites(Fluid f) {
        if (f == Fluids.EMPTY) f = Fluids.WATER;
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(f);
        color = extensions.getTintColor();
        ResourceLocation fluidtextureStill = extensions.getStillTexture();
        spriteStill = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidtextureStill);
    }
}
