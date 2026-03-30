package BetterPipes.PipeBase;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.VertexBufferCleaner;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.ICrankShaftConnector;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import AgeOfSteam.Static;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityPipe extends BlockEntity implements INetworkTagReceiver, IMechanicalBlockProvider, ICrankShaftConnector {

    public static int STATE_UPDATE_TICKS = 40;
    public static int FORCE_OUTPUT_AFTER_TICKS = 20;

    public Map<Direction, PipeConnection> connections = new HashMap<>();
    public FluidTank tank;
    public int mainCapacity;
    public int flowRate;
    FluidRenderData renderData = new FluidRenderData();
    VertexBuffer vertexBuffer; // using vbo for the fluid is faster. trading less mesh building for more render calls
    MeshData fluidMesh;
    VertexBuffer vertexBufferPumpArm;
    VertexBuffer vertexBufferPumpCube;
    boolean requiresMeshUpdate = false;
    boolean requiresMeshUpdate2 = false;
    int lastLight;

    FluidStack last_tankFluid = FluidStack.EMPTY;
    int lastFill;
    int ticksWithFluidInTank;
    boolean tankNorth = false;
    boolean tankEast = false;
    boolean tankWest = false;
    boolean tankSouth = false;

    Direction crankShaftSide = null;
    boolean hasAnyExtractionConnections = false;

    double mechanicalResistance;
    public EntityPipe(BlockEntityType type, BlockPos pos, BlockState blockState, int mainCapacity, int flowRate) {
        super(type, pos, blockState);
        this.mainCapacity = mainCapacity;
        this.flowRate = flowRate;

        tank = new FluidTank(mainCapacity) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };

        for (Direction i : Direction.values()) {
            connections.put(i, new PipeConnection(this, i, mainCapacity / 2));
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBufferPumpArm = new VertexBuffer(VertexBuffer.Usage.STATIC);
                vertexBufferPumpCube = new VertexBuffer(VertexBuffer.Usage.STATIC);

                VertexBufferCleaner.register(this, vertexBuffer);
                VertexBufferCleaner.register(this, vertexBufferPumpArm);
                VertexBufferCleaner.register(this, vertexBufferPumpCube);
            });
        }
    }    AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return 9999;
        }

        @Override
        public double getInertia(Direction direction) {
            return 0.1;
        }

        @Override
        public double getTorqueResistance(Direction direction) {
            return mechanicalResistance;
        }

        @Override
        public double getTorqueProduced(Direction direction) {
            return 0;
        }

        @Override
        public double getRotationMultiplierToInside(@Nullable Direction direction) {
            return 1;
        }

        @Override
        public void propagateTickBeforeUpdate() {
            super.propagateTickBeforeUpdate();

            // because the crankshaft can dynamically connect and unconnect, make sure the arm is in sync with the crankshaft
            // this is easier than always reset the rotation on connect or unconnect
            // do not use this in tick, if the pipe ticks before crankshaft the rotation will be out of sync
            // this is specifically what this method is designed to do, to run when all mechanical blocks are on same state before tick
            // runs on both server and client side to keep the visuals correct
            if (crankShaftSide != null && hasAnyExtractionConnections) {
                if (level.getBlockEntity(getBlockPos().relative(crankShaftSide)) instanceof IMechanicalBlockProvider mechanicalBlockProvider &&
                        mechanicalBlockProvider.getMechanicalBlock(crankShaftSide.getOpposite()) instanceof AbstractMechanicalBlock mechanicalBlock
                ) {
                    myMechanicalBlock.currentRotation = mechanicalBlock.currentRotation;
                }
            }
        }
    };

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityPipe) t).tick();
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return connections.get(side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
        if (level.isClientSide) {
            CompoundTag tag = new CompoundTag();
            tag.put("client_onload", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, tag));
            setRequiresMeshUpdate();
        }
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        hasAnyExtractionConnections = false;
        for (Direction i : Direction.values()) {
            BlockPipe.ConnectionState face = getBlockState().getValue(BlockPipe.connections.get(i));
            if (face == BlockPipe.ConnectionState.EXTRACTION) {
                hasAnyExtractionConnections = true;
                break;
            }
        }

        // this is because for some reason minecraft stops updating the sprite
        // so i do it every tick
        if (level.isClientSide) {
            renderData.updateSprites(tank.getFluid().getFluid());
            for (Direction i : Direction.values())
                connections.get(i).renderData.updateSprites(connections.get(i).tank.getFluid().getFluid());
        }

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

                // check for changes and sync to client
                boolean requiresUpdate = false;
                if (!FluidStack.isSameFluidSameComponents(last_tankFluid, tank.getFluid()) || last_tankFluid.getAmount() != tank.getFluidAmount()) {
                    requiresUpdate = true;
                }
                last_tankFluid = tank.getFluid().copy(); // Update the last known tank fluid

                // check connections for changes to sync
                for (Direction direction : Direction.values()) {
                    PipeConnection conn = connections.get(direction);
                    if (conn.needsSync())
                        requiresUpdate = true;
                }

                if (requiresUpdate) {
                    syncTanksToClient(null);
                }
            }
            mechanicalResistance = 5;
            for (Direction direction : Direction.allShuffled(level.random)) {
                PipeConnection conn = connections.get(direction);
                if (state.getValue(BlockPipe.connections.get(direction)) == BlockPipe.ConnectionState.CONNECTED || state.getValue(BlockPipe.connections.get(direction)) == BlockPipe.ConnectionState.EXTRACTION) {
                    if (conn.lastFill > 0) {
                        if (!conn.getsInputFromInside && isUpdateTick) {
                            //drain into main tank
                            double transferRateMultiplier = (double) conn.lastFill / mainCapacity * 4;
                            int target_free = mainCapacity / 2;
                            int has_free = mainCapacity - lastFill;
                            double speedMultiplier = Math.min(1, (float) has_free / target_free);
                            int toTransfer = (int) (flowRate * update_after_ticks * speedMultiplier * Math.min(1, transferRateMultiplier));

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
                                            double transferRateMultiplier = (double) conn.lastFill / mainCapacity * 4;
                                            int target_free = mainCapacity / 4;
                                            int has_free = mainCapacity / 2 - pipeconn.lastFill;
                                            double speedMultiplier = Math.min(1, (float) has_free / target_free);
                                            int toTransfer = (int) (flowRate * update_after_ticks * speedMultiplier * Math.min(1, transferRateMultiplier));

                                            if (toTransfer == 0 && conn.ticksWithFluidInTank >= FORCE_OUTPUT_AFTER_TICKS / 2)
                                                toTransfer = 1;

                                            FluidStack drained = conn.tank.drain(toTransfer, IFluidHandler.FluidAction.SIMULATE);
                                            int filled = conn.neighborFluidHandler().fill(drained, IFluidHandler.FluidAction.SIMULATE);
                                            toTransfer = Math.min(filled, toTransfer);
                                            pipeconn.fill(conn.drain(toTransfer, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                                        }
                                    } else {
                                        // for others, output every tick
                                        double transferRateMultiplier = (double) conn.lastFill / mainCapacity * 4;
                                        int toTransfer = (int) (flowRate * transferRateMultiplier);

                                        toTransfer = (int) Math.min(toTransfer, flowRate * 1.1);

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


                    IFluidHandler neighbor = conn.neighborFluidHandler();
                    if (neighbor != null && !(neighbor instanceof PipeConnection)) {
                        if (state.getValue(BlockPipe.connections.get(direction)) == BlockPipe.ConnectionState.EXTRACTION) {
                            // extract from a neighbor fluid handler
                            // this runs every tick
                            double toDrainDouble = Math.min(flowRate, flowRate * Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f);
                            int toDrain = (int) toDrainDouble;
                            toDrain = Math.abs(toDrain);
                            FluidStack drained = neighbor.drain(toDrain, IFluidHandler.FluidAction.SIMULATE);
                            int filled = conn.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                            int toTransfer = Math.min(filled, drained.getAmount());
                            drained = neighbor.drain(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                            conn.fill(drained, IFluidHandler.FluidAction.EXECUTE);


                            if (toDrain == drained.getAmount()) {
                                // make sure the resistance is about the same so it keeps the flow static and not updates the meshes all the time
                                // use the double value for a consistent resistance force
                                mechanicalResistance += toDrainDouble / flowRate * 50;
                            } else
                                // if not everything was drained, make the resistance depending on how much was drained
                                mechanicalResistance += (double) drained.getAmount() / flowRate * 50;
                        }
                    }

                    if (isUpdateTick) {
                        if (lastFill > 0) {
                            //drain main tank into connection, using 2 stage update
                            if (!conn.outputsToInside && state.getValue(BlockPipe.connections.get(direction)) != BlockPipe.ConnectionState.EXTRACTION) {
                                double transferRateMultiplier = (double) lastFill / mainCapacity * 2;
                                int target_free = mainCapacity / 4;
                                int has_free = mainCapacity / 2 - conn.lastFill;
                                double speedMultiplier = Math.min(1, (float) has_free / target_free);
                                int toTransfer = (int) (flowRate * update_after_ticks * speedMultiplier * Math.min(1, transferRateMultiplier));

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

    public void toggleExtractionMode(Direction hitFace) {
        BlockState state = getBlockState();
        if (state.getValue(BlockPipe.connections.get(hitFace)) == BlockPipe.ConnectionState.CONNECTED) {
            state = state.setValue(BlockPipe.connections.get(hitFace), BlockPipe.ConnectionState.EXTRACTION);
        } else if (state.getValue(BlockPipe.connections.get(hitFace)) == BlockPipe.ConnectionState.EXTRACTION) {
            state = state.setValue(BlockPipe.connections.get(hitFace), BlockPipe.ConnectionState.CONNECTED);
        }
        level.setBlock(getBlockPos(), state, 3);
    }

    public void syncTanksToClient(ServerPlayer p) {
        CompoundTag updateTag = new CompoundTag();
        for (Direction direction : Direction.values()) {
            PipeConnection conn = connections.get(direction);
            CompoundTag tag = conn.getUpdateTag(level.registryAccess());
            updateTag.put(direction.getName(), tag);
        }
        updateTag.put("mainTank", getUpdateTag(level.registryAccess()));
        if (p != null)
            PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, updateTag));
        else
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, updateTag));
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer player) {
        if (compoundTag.contains("client_onload")) {
            syncTanksToClient(player);
        }
        myMechanicalBlock.mechanicalReadServer(compoundTag, player);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        for (Direction direction : Direction.values()) {
            if (compoundTag.contains(direction.getName())) {
                connections.get(direction).handleUpdateTag(compoundTag.getCompound(direction.getName()), level.registryAccess());
            }
        }
        if (compoundTag.contains("mainTank")) {
            handleUpdateTag(compoundTag.getCompound("mainTank"), level.registryAccess());
        }
        myMechanicalBlock.mechanicalReadClient(compoundTag);
    }

    public void setRequiresMeshUpdate() {
        requiresMeshUpdate2 = true;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        handleUpdateTag(tag.getCompound("main"), registries);

        for (Direction direction : Direction.values()) {
            PipeConnection conn = connections.get(direction);
            conn.loadAdditional(registries, tag);
        }
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put("main", getUpdateTag(registries));

        for (Direction direction : Direction.values()) {
            PipeConnection conn = connections.get(direction);
            conn.saveAdditional(registries, tag);
        }
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!tank.isEmpty()) {
            tag.put("fluid", tank.getFluid().save(registries));
        }

        if (crankShaftSide != null)
            tag.putInt("crankShaftSide", crankShaftSide.ordinal());

        tag.putBoolean("tankNorth", tankNorth);
        tag.putBoolean("tankEast", tankEast);
        tag.putBoolean("tankSouth", tankSouth);
        tag.putBoolean("tankWest", tankWest);

        // client might have different config file, and config packet might arrive after the tanks are created
        // it needs the correct capacity to render the fluids correctly
        // flow rate shouldnt matter for rendering
        // i know it could be made simpler with a get method in the children classes, but the render code is >1400 lines and already refactored by a llm and it uses the tank capacity
        tag.putInt("mainCapacity", mainCapacity);

        return tag;
    }

    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {

        FluidStack newFluid = FluidStack.EMPTY;
        if (tag.contains("fluid")) {
            newFluid = (FluidStack.parse(registries, tag.getCompound("fluid")).get());
        }
        if (!FluidStack.isSameFluidSameComponents(newFluid, tank.getFluid()) || tank.getFluidAmount() != newFluid.getAmount()) {
            setRequiresMeshUpdate();
        }
        tank.setFluid(newFluid);

        crankShaftSide = null;
        if (tag.contains("crankShaftSide"))
            crankShaftSide = Direction.values()[tag.getInt("crankShaftSide")];

        tankEast = tag.getBoolean("tankEast");
        tankSouth = tag.getBoolean("tankSouth");
        tankNorth = tag.getBoolean("tankNorth");
        tankWest = tag.getBoolean("tankWest");

        int serverMainCapacity = tag.getInt("mainCapacity");
        if (serverMainCapacity != mainCapacity) {
            mainCapacity = serverMainCapacity;
            tank.setCapacity(mainCapacity);
            for (PipeConnection connection : connections.values()) {
                connection.tank.setCapacity(mainCapacity / 2);
            }
        }
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction direction) {
        if (crankShaftSide == null) {
            return null;
        }
        if (direction != crankShaftSide) {
            return null;
        }
        // we need to have 1 or more connections in extraction mode to use the pump mode
        if (hasAnyExtractionConnections) {
            return myMechanicalBlock;
        }
        return null;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    public static class FluidRenderData {

        TextureAtlasSprite spriteFLowing;
        TextureAtlasSprite spriteStill;
        int color;

        public FluidRenderData() {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                updateSprites(Fluids.WATER);
            }
        }

        public void updateSprites(Fluid f) {
            if (f == Fluids.EMPTY) f = Fluids.WATER;
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(f);
            color = extensions.getTintColor();
            ResourceLocation fluidtextureStill = extensions.getStillTexture();
            spriteStill = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidtextureStill);
            ResourceLocation fluidtextureFlowing = extensions.getFlowingTexture();
            spriteFLowing = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidtextureFlowing);
        }
    }


}