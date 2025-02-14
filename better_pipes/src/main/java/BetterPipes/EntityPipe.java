package BetterPipes;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.sun.tools.jconsole.JConsoleContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

import static BetterPipes.Registry.ENTITY_PIPE;
import static net.minecraft.client.renderer.RenderType.TRANSIENT_BUFFER_SIZE;

public class EntityPipe extends BlockEntity implements INetworkTagReceiver {

    public static int MAX_OUTPUT_RATE = Config.INSTANCE.maxOutputRate;
    public static int REQUIRED_FILL_FOR_MAX_OUTPUT = Config.INSTANCE.mainRequiredFillForMaxOutput;
    public static int MAIN_CAPACITY = Config.INSTANCE.main_capacity;

    public static int CONNECTION_MAX_OUTPUT_RATE = Config.INSTANCE.maxOutputRate;
    public static int CONNECTION_REQUIRED_FILL_FOR_MAX_OUTPUT = Config.INSTANCE.connectionRequiredFillForMaxOutput;
    public static int CONNECTION_CAPACITY = Config.INSTANCE.connection_capacity;

    public static int STATE_UPDATE_TICKS = 40;
    public static int FORCE_OUTPUT_AFTER_TICKS = 20;

    public Map<Direction, PipeConnection> connections = new HashMap<>();
    public FluidTank tank = new simpleBlockEntityTank(MAIN_CAPACITY, this);
    FluidStack last_tankFluid = FluidStack.EMPTY;
    int lastFill;
    int ticksWithFluidInTank = 0;

    public fluidRenderData renderData = new fluidRenderData();
    public VertexBuffer vertexBuffer;
    public MeshData mesh;
    public boolean requiresMeshUpdate = false;
    public boolean requiresMeshUpdate2 = false;
    public ByteBufferBuilder myByteBuffer;
    public int lastLight;

    public EntityPipe(BlockPos pos, BlockState blockState) {
        super(ENTITY_PIPE.get(), pos, blockState);
        for (Direction i : Direction.values()) {
            connections.put(i, new PipeConnection(this, i));
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                myByteBuffer = new ByteBufferBuilder(TRANSIENT_BUFFER_SIZE);
            });
        }
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return connections.get(side);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level.isClientSide) {
            UUID from = Minecraft.getInstance().player.getUUID();
            CompoundTag tag = new CompoundTag();
            tag.put("client_onload", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, tag));
            setRequiresMeshUpdate();
        }
    }

    @Override
    public void setRemoved() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer.close();
                myByteBuffer.close();
            });

        }
        super.setRemoved();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityPipe) t).tick();
    }

    public void tick() {

        // this is because for some reason minecraft stops updating the sprite
        // so i do it every tick
        renderData.updateSprites(tank.getFluid().getFluid());
        for (Direction i : Direction.values())
            connections.get(i).renderData.updateSprites(connections.get(i).tank.getFluid().getFluid());

        // to not re-mesh on every packet, re-mesh only once per tick at max
        if (FMLEnvironment.dist == Dist.CLIENT && requiresMeshUpdate2) {
            requiresMeshUpdate2 = false;
            requiresMeshUpdate = true;
        }

        // 2 stage ticking, you can set it to 3 to do the stages every 3 ticks
        // but the tank needs to have enough capacity to fill / drain x times the tick flowrate during the update tick
        int update_after_ticks = 2;

        if (!level.isClientSide) {
            BlockState state = level.getBlockState(getBlockPos());
            boolean isUpdateTick = level.getGameTime() % update_after_ticks == 1;
            boolean isSyncTick = level.getGameTime() % update_after_ticks == 0;
            if (isSyncTick) {
                // store last fill data for all pipes to use in updateTick
                lastFill = tank.getFluidAmount();
                for (Direction direction : Direction.values()) {
                    connections.get(direction).lastFill = connections.get(direction).tank.getFluidAmount();
                }

                if (!FluidStack.isSameFluidSameComponents(last_tankFluid, tank.getFluid())) {
                    if (!tank.getFluid().isEmpty())
                        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) getLevel(), new ChunkPos(getBlockPos()), PacketFluidUpdate.getPacketFluidUpdate(getBlockPos(), null, tank.getFluid().getFluid()));
                }
                if (last_tankFluid.getAmount() != tank.getFluidAmount()) {
                    PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) getLevel(), new ChunkPos(getBlockPos()), PacketFluidAmountUpdate.getPacketFluidUpdate(getBlockPos(), null, tank.getFluidAmount()));
                }
                last_tankFluid = tank.getFluid().copy(); // Update the last known tank fluid

                CompoundTag updateTag = new CompoundTag();
                boolean needsSendUpdate = false;
                for (Direction direction : Direction.values()) {
                    PipeConnection conn = connections.get(direction);
                    if (state.getValue(BlockPipe.connections.get(direction)) == BlockPipe.ConnectionState.CONNECTED || state.getValue(BlockPipe.connections.get(direction)) == BlockPipe.ConnectionState.EXTRACTION) {
                        conn.syncTanks();
                        if (conn.needsSync()) {
                            updateTag.put(direction.getName(), conn.getUpdateTag(level.registryAccess()));
                            needsSendUpdate = true;
                        }
                    }
                }
                if (needsSendUpdate) {
                    updateTag.putLong("time", System.currentTimeMillis());
                    PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, updateTag));
                }
            }
            for (Direction direction : Direction.allShuffled(level.random)) {
                PipeConnection conn = connections.get(direction);
                if (state.getValue(BlockPipe.connections.get(direction)) == BlockPipe.ConnectionState.CONNECTED || state.getValue(BlockPipe.connections.get(direction)) == BlockPipe.ConnectionState.EXTRACTION) {
                    if (conn.lastFill > 0) {
                        if (!conn.getsInputFromInside && isUpdateTick) {
                            //drain into main tank
                            double transferRateMultiplier = (double) conn.lastFill / CONNECTION_REQUIRED_FILL_FOR_MAX_OUTPUT;
                            int toTransfer;
                            int target_free = MAIN_CAPACITY - REQUIRED_FILL_FOR_MAX_OUTPUT;
                            int has_free = MAIN_CAPACITY - lastFill;
                            double speedMultiplier = Math.min(1, (float) has_free / target_free);
                            toTransfer = (int) (CONNECTION_MAX_OUTPUT_RATE * update_after_ticks * speedMultiplier * Math.min(1, transferRateMultiplier));

                            if (toTransfer == 0 && conn.ticksWithFluidInTank >= FORCE_OUTPUT_AFTER_TICKS / 2)
                                toTransfer = 1;

                            FluidStack drained = conn.tank.drain(toTransfer, IFluidHandler.FluidAction.SIMULATE);
                            int filled = tank.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                            toTransfer = Math.min(filled, toTransfer);
                            tank.fill(conn.drain(toTransfer, IFluidHandler.FluidAction.EXECUTE, true), IFluidHandler.FluidAction.EXECUTE);
                        }

                        if (!conn.getsInputFromOutside) {
                            if (conn.neighborFluidHandler() != null) {
                                if (state.getValue(BlockPipe.connections.get(direction)) != BlockPipe.ConnectionState.EXTRACTION) {
                                    //drain to outside tank
                                    if (conn.neighborFluidHandler() instanceof PipeConnection pipeconn) {
                                        if (isUpdateTick) {
                                            // for pipes use normal 2 stage tick logic
                                            double transferRateMultiplier = (double) conn.lastFill / CONNECTION_REQUIRED_FILL_FOR_MAX_OUTPUT;
                                            int toTransfer;
                                            int target_free = CONNECTION_CAPACITY - CONNECTION_REQUIRED_FILL_FOR_MAX_OUTPUT;
                                            int has_free = CONNECTION_CAPACITY - pipeconn.lastFill;
                                            double speedMultiplier = Math.min(1, (float) has_free / target_free);
                                            toTransfer = (int) (CONNECTION_MAX_OUTPUT_RATE * update_after_ticks * speedMultiplier * Math.min(1, transferRateMultiplier));

                                            if (toTransfer == 0 && conn.ticksWithFluidInTank >= FORCE_OUTPUT_AFTER_TICKS / 2)
                                                toTransfer = 1;

                                            FluidStack drained = conn.tank.drain(toTransfer, IFluidHandler.FluidAction.SIMULATE);
                                            int filled = conn.neighborFluidHandler().fill(drained, IFluidHandler.FluidAction.SIMULATE);
                                            toTransfer = Math.min(filled, toTransfer);
                                            pipeconn.fill(conn.drain(toTransfer, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                                        }
                                    } else {
                                        // for others, output every tick
                                        double transferRateMultiplier = (double) conn.lastFill / CONNECTION_REQUIRED_FILL_FOR_MAX_OUTPUT;
                                        int toTransfer = (int) (CONNECTION_MAX_OUTPUT_RATE * transferRateMultiplier);
                                        if (toTransfer > CONNECTION_MAX_OUTPUT_RATE)
                                            toTransfer = CONNECTION_MAX_OUTPUT_RATE + (int) (transferRateMultiplier * CONNECTION_MAX_OUTPUT_RATE / 10f);
                                        if (toTransfer == 0 && conn.ticksWithFluidInTank >= FORCE_OUTPUT_AFTER_TICKS / 2)
                                            toTransfer = 1;

                                        FluidStack drained = conn.tank.drain(toTransfer, IFluidHandler.FluidAction.SIMULATE);
                                        int filled = conn.neighborFluidHandler().fill(drained, IFluidHandler.FluidAction.SIMULATE);
                                        toTransfer = Math.min(filled, toTransfer);
                                        conn.neighborFluidHandler().fill(conn.drain(toTransfer, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                                    }
                                }
                            }
                        }
                    }

                    if (state.getValue(BlockPipe.pipe_is_extraction_active) && state.getValue(BlockPipe.connections.get(direction)) == BlockPipe.ConnectionState.EXTRACTION) {
                        // extract from a neighbor fluid handler
                        // this runs every tick
                        try {
                            FluidStack drained = conn.neighborFluidHandler().drain(CONNECTION_MAX_OUTPUT_RATE, IFluidHandler.FluidAction.SIMULATE);
                            int filled = conn.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                            int toTransfer = Math.min(filled, drained.getAmount());
                            drained = conn.neighborFluidHandler().drain(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                            conn.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                        } catch (Exception e) {
                            level.setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
                            System.out.println(new RuntimeException(e));
                        }
                    }
                    if (isUpdateTick) {
                        if (lastFill > 0) {
                            //drain main tank into connection, using 2 stage update
                            if (!conn.outputsToInside && state.getValue(BlockPipe.connections.get(direction)) != BlockPipe.ConnectionState.EXTRACTION) {
                                double transferRateMultiplier = (double) lastFill / REQUIRED_FILL_FOR_MAX_OUTPUT;
                                int toTransfer = (int) (MAX_OUTPUT_RATE * 2 * transferRateMultiplier);
                                int target_free = CONNECTION_CAPACITY - CONNECTION_REQUIRED_FILL_FOR_MAX_OUTPUT;
                                int has_free = CONNECTION_CAPACITY - conn.lastFill;
                                double speedMultiplier = Math.min(1, (float) has_free / target_free);
                                toTransfer = (int) (MAX_OUTPUT_RATE * update_after_ticks * speedMultiplier * Math.min(1, transferRateMultiplier));

                                if (toTransfer == 0 && ticksWithFluidInTank >= FORCE_OUTPUT_AFTER_TICKS)
                                    toTransfer = 1;

                                FluidStack drained = tank.drain(toTransfer, IFluidHandler.FluidAction.SIMULATE);
                                int filled = conn.tank.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                                toTransfer = Math.min(filled, toTransfer);
                                conn.fill(tank.drain(toTransfer, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE, true);
                            }
                        }
                    }
                }
            }

            if (!tank.isEmpty() && ticksWithFluidInTank < FORCE_OUTPUT_AFTER_TICKS + 1)
                ticksWithFluidInTank++;
            else if (tank.isEmpty()) {
                ticksWithFluidInTank = 0;
            }
            for (Direction direction : Direction.allShuffled(level.random)) {
                PipeConnection conn = connections.get(direction);
                if (state.getValue(BlockPipe.connections.get(direction)) == BlockPipe.ConnectionState.CONNECTED || state.getValue(BlockPipe.connections.get(direction)) == BlockPipe.ConnectionState.EXTRACTION) {
                    conn.update();
                }
            }
        }
    }

    public BlockState setExtractionMode(BlockState state, boolean mode) {
        if (state.getValue(BlockPipe.pipe_is_extraction) != mode) {
            state = state.setValue(BlockPipe.pipe_is_extraction_active, false);
        }
        state = state.setValue(BlockPipe.pipe_is_extraction, mode);

        for (Direction i : Direction.values()) {
            if((state.getValue(BlockPipe.connections.get(i)) == BlockPipe.ConnectionState.CONNECTED || state.getValue(BlockPipe.connections.get(i)) == BlockPipe.ConnectionState.EXTRACTION)) {
                boolean shouldBeExtraction = mode && !(connections.get(i).neighborFluidHandler() instanceof PipeConnection);
                if (shouldBeExtraction)
                    state = state.setValue(BlockPipe.connections.get(i), BlockPipe.ConnectionState.EXTRACTION);
                else
                    state = state.setValue(BlockPipe.connections.get(i), BlockPipe.ConnectionState.CONNECTED);
            }
        }
        return state;
    }

    public BlockState setExtractionActive(BlockState state, boolean mode) {
        state = state.setValue(BlockPipe.pipe_is_extraction_active, mode);
        return state;
    }

    public void toggleExtractionActive() {
        BlockState state = level.getBlockState(getBlockPos());
        state = setExtractionActive(state, !state.getValue(BlockPipe.pipe_is_extraction_active));
        level.setBlock(getBlockPos(), state, 3);
    }

    public void toggleExtractionMode() {
        BlockState state = level.getBlockState(getBlockPos());

        boolean hasAnyConnectionsInExtractionMode = false;
        for (Direction i : Direction.values()) {
            if (state.getValue(BlockPipe.connections.get(i)) == BlockPipe.ConnectionState.EXTRACTION)
                hasAnyConnectionsInExtractionMode = true;
        }
        if (hasAnyConnectionsInExtractionMode) {
            state = setExtractionMode(state, false);
        } else {
            boolean hasAnyValidConnections = false;
            for (Direction i : Direction.values()) {
                if ((state.getValue(BlockPipe.connections.get(i)) == BlockPipe.ConnectionState.CONNECTED || state.getValue(BlockPipe.connections.get(i)) == BlockPipe.ConnectionState.EXTRACTION) && !(connections.get(i).neighborFluidHandler() instanceof PipeConnection))
                    hasAnyValidConnections = true;
            }
            if (hasAnyValidConnections) {
                state = setExtractionMode(state, !state.getValue(BlockPipe.pipe_is_extraction));
            }
        }
        level.setBlock(getBlockPos(), state, 3);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer player) {
        if (compoundTag.contains("client_onload")) {

            CompoundTag updateTag = new CompoundTag();
            for (Direction direction : Direction.values()) {
                PipeConnection conn = connections.get(direction);
                CompoundTag tag = conn.getUpdateTag(level.registryAccess());
                updateTag.put(direction.getName(), tag);
                conn.sendInitialTankUpdates(player);
            }
            updateTag.putLong("time", System.currentTimeMillis());
            PacketDistributor.sendToPlayer(player, PacketBlockEntity.getBlockEntityPacket(this, updateTag));
            if (!tank.getFluid().isEmpty()) {
                PacketDistributor.sendToPlayer(player, PacketFluidUpdate.getPacketFluidUpdate(getBlockPos(), null, tank.getFluid().getFluid()));
                PacketDistributor.sendToPlayer(player, PacketFluidAmountUpdate.getPacketFluidUpdate(getBlockPos(), null, tank.getFluidAmount()));
            }

        }
    }

    long lastUpdate = 0;

    @Override
    public void readClient(CompoundTag compoundTag) {
        if (compoundTag.contains(("time"))) {
            long updateTime = compoundTag.getLong("time");
            if (updateTime >= lastUpdate) {
                lastUpdate = updateTime;
                for (Direction direction : Direction.values()) {
                    if (compoundTag.contains(direction.getName())) {
                        connections.get(direction).handleUpdateTag(compoundTag.getCompound(direction.getName()), level.registryAccess());
                    }
                }
                setRequiresMeshUpdate();
            }
        }
    }

    public void setRequiresMeshUpdate() {
        requiresMeshUpdate2 = true;
    }

    long lastFluidInTankUpdate;

    public void setFluidInTank(Fluid f, long time) {
        if (time > lastFluidInTankUpdate) {
            lastFluidInTankUpdate = time;
            tank.setFluid(new FluidStack(f, Math.max(1,tank.getFluidAmount())));
            setRequiresMeshUpdate();
        }
    }

    long lastFluidAmountUpdate;

    public void setFluidAmountInTank(int amount, long time) {
        if (time > lastFluidAmountUpdate) {
            lastFluidAmountUpdate = time;
            Fluid myFluid = tank.getFluid().getFluid();
            if (myFluid == Fluids.EMPTY && amount > 0) myFluid = Fluids.WATER;
            if (amount <= 0) myFluid = Fluids.EMPTY;
            tank.setFluid(new FluidStack(myFluid, amount));
            setRequiresMeshUpdate();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        tank.readFromNBT(registries, tag.getCompound("mainTank"));

        for (Direction direction : Direction.values()) {
            PipeConnection conn = connections.get(direction);
            conn.loadAdditional(registries, tag);
        }

    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(registries, tankTag);
        tag.put("mainTank", tankTag);

        for (Direction direction : Direction.values()) {
            PipeConnection conn = connections.get(direction);
            conn.saveAdditional(registries, tag);
        }
    }
}