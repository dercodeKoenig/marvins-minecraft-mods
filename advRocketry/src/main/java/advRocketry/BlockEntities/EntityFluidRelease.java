package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.PacketBlockEntity;
import advRocketry.API;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.GlobalTime;
import advRocketry.Registry.GasRegistry;
import advRocketry.Render.Particles.RocketParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import static advRocketry.Registry.BlockEntities.ENTITY_CARGO_HOLD;
import static advRocketry.Registry.BlockEntities.ENTITY_FLUID_RELEASE;

public class EntityFluidRelease extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public FluidTank tank;

    // Accumulate 1 bucket of water before placing the source for visualization.
    // The placed / later removed water should not contribute to composition,
    // but the player could still "farm" water by releasing 1 bucket, let it place the source, and take the source away.
    // By accumulating 1 water before placing the source it makes sure player can not farm water
    int accumulatedWaterBeforePlace = 0;
    Fluid lastReleasedFluid = Fluids.EMPTY;

    // if working, notify the clients every few ticks so they spawn particles
    long lastParticleSent = 0;
    // client will keep spawning particles after receiving the packet until timeout is reached
    int clientParticleTimeout;


    public EntityFluidRelease(BlockPos pos, BlockState blockState) {
        super(ENTITY_FLUID_RELEASE.get(), pos, blockState);
        tank = new FluidTank(10000) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                for (GasRegistry.Gas gas : GasRegistry.gases.values()) {
                    if (gas.fluid.equals(stack.getFluid()))
                        return true;
                }
                return false;
            }
        };
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityFluidRelease) t).tick();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        if (compoundTag.contains("release_gas")) {
            // reset timeout
            clientParticleTimeout = 20;
        }
    }


    // tank is emptied every tick unless the output is blocked
    // i do not need to save the tank
    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    public void tick() {

        if (level.isClientSide) {
            if (clientParticleTimeout > 0) {
                clientParticleTimeout--;
                if (GlobalTime.getGlobalTime() % 3 == 0) {
                    // reuse rocket particles
                    Vec3 worldPos = getBlockPos().getCenter();
                    Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
                    worldPos = worldPos.add(facing.getStepX() * 0.7, 0, facing.getStepZ() * 0.7);
                    new RocketParticle(
                            (ClientLevel) level,
                            worldPos.x + (Math.random() - 0.5) * 0.5,
                            worldPos.y + (Math.random() - 0.5) * 0.5,
                            worldPos.z + (Math.random() - 0.5) * 0.5,
                            facing.getStepX() * 0.1,
                            Math.random() * 0.1,
                            facing.getStepZ() * 0.1,
                            new Vector3f(0.5f, 0.5f, 0.5f).mul(1.5f),
                            0.2f,
                            1,
                            100,
                            true,
                            false
                    );
                }
            }
        }

        if (!level.isClientSide) {

            Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos inFrontPos = getBlockPos().relative(facing);
            BlockState inFrontState = level.getBlockState(inFrontPos);

            // release all fluid into composition
            FluidStack fluidStack = tank.getFluid();
            if (!fluidStack.isEmpty() && inFrontState.canBeReplaced()) {
                if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {
                    for (GasRegistry.Gas gas : GasRegistry.gases.values()) {
                        if (gas.fluid.equals(fluidStack.getFluid())) {
                            // release all in atmosphere, the system will make sure it rains down if required
                            API.addGasInBuckets(level.dimension().location(), gas.id, (double) fluidStack.getAmount() / 1000);
                            break;
                        }
                    }
                }
                tank.setFluid(FluidStack.EMPTY);


                Dimension dim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
                boolean isLiquidWater = false;
                if (fluidStack.getFluid().equals(Fluids.WATER) && dim != null && !dim.shouldBoilBlocks(GasRegistry.water, getBlockPos()))
                    isLiquidWater = true;


                // send particles over every few ticks
                if (!isLiquidWater) {
                    if (GlobalTime.getGlobalTime() > lastParticleSent + 18) {
                        CompoundTag info = new CompoundTag();
                        info.putInt("release_gas", 0);
                        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
                        lastParticleSent = GlobalTime.getGlobalTime();
                    }
                }
                // water special, place fluid (composition tracker ignores this class)
                else {
                    if (!inFrontState.getBlock().equals(Blocks.WATER)) {
                        if (accumulatedWaterBeforePlace > 1000) {
                            level.setBlock(inFrontPos, Blocks.WATER.defaultBlockState(), 3);
                            accumulatedWaterBeforePlace = 0;
                        } else {
                            accumulatedWaterBeforePlace += fluidStack.getAmount();
                        }
                    }
                }
            }
            // remove water in front of me again (composition tracker ignores this class)
            if (lastReleasedFluid.equals(Fluids.WATER) && !fluidStack.getFluid().equals(Fluids.WATER)) {
                if (inFrontState.getBlock().equals(Blocks.WATER) && inFrontState.getFluidState().isSource()) {
                    level.setBlock(inFrontPos, Blocks.AIR.defaultBlockState(), 3);
                    // since we removed the water, we can instantly place it again next tick and no need to wait
                    accumulatedWaterBeforePlace = 1000;
                }
            }

            lastReleasedFluid = fluidStack.getFluid();
        }
    }
}