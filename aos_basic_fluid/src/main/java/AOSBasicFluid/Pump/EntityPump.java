package AOSBasicFluid.Pump;

import ARLib.network.INetworkTagReceiver;
import ARLib.utils.VertexBufferCleaner;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.BlockCrankShaftBase;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.ICrankShaftConnector;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import AgeOfSteam.Static;
import FiniteWater.Config;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.*;

import static AOSBasicFluid.Registry.ENTITY_PUMP;
import static AOSBasicFluid.Registry.PUMP_EXT;


public class EntityPump extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver, ICrankShaftConnector {


    static Direction[] HORIZONTAL_DIRECTIONS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    public VertexBuffer vertexBufferArm1;
    public MeshData meshArm1;
    public VertexBuffer vertexBufferArm2;
    public MeshData meshArm2;
    public VertexBuffer vertexBufferArm3;
    public MeshData meshArm3;
    public int lastLight;
    public int maxRadiusSqr = PumpConfig.INSTANCE.maxRadius * PumpConfig.INSTANCE.maxRadius;
    public OutputOnlyTank myTank = new OutputOnlyTank(PumpConfig.INSTANCE.tankCapacity) {
        @Override
        public void onContentsChanged() {
            setChanged();
        }
    };
    public double progress = 0;
    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return 99999999;
        }

        @Override
        public double getInertia(Direction face) {
            return 5;
        }

        @Override
        public double getTorqueResistance(Direction face) {
            return PumpConfig.INSTANCE.resistance;
        }

        @Override
        public double getTorqueProduced(Direction face) {
            return 0;
        }

        @Override
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            return 1;
        }
    };
    // cache for distance check
    Vec3 myCenter = Vec3.ZERO;
    // holds the fluid below the pump block that is to be pumped
    // Fluid can be flowing or still, but FluidType is same for both
    FluidType fluidToPump = Fluids.EMPTY.getFluidType();
    // the blocks that have to be scanned
    ArrayDeque<BlockPos> nextBlocksToScan = new ArrayDeque<>();
    // the blocks already scanned to not scan double
    // storing the positions as pos.asLong() in this hashSet was found to be faster than HashSet<BlockPos>
    LongOpenHashSet workedPositions = new LongOpenHashSet();
    // mark the position with the largest distance that is the target to be pumped
    // (so we do not drain the local area and run out of fluid)
    BlockPos bestTarget = null;
    double bestTargetDistance = -1;

    public EntityPump(BlockPos p_155229_, BlockState p_155230_) {
        super(ENTITY_PUMP.get(), p_155229_, p_155230_);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBufferArm1 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBufferArm2 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBufferArm3 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);

                VertexBufferCleaner.register(this, vertexBufferArm1);
                VertexBufferCleaner.register(this, vertexBufferArm2);
                VertexBufferCleaner.register(this, vertexBufferArm3);
            });
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityPump) t).tick();
    }

    @Override
    public void onLoad() {
        myMechanicalBlock.mechanicalOnload();
        myCenter = getBlockPos().getCenter();
        super.onLoad();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        myMechanicalBlock.mechanicalReadServer(compoundTag, serverPlayer);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadClient(compoundTag);
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (side == myState.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            BlockEntity t = level.getBlockEntity(getBlockPos().relative(side));
            if (t instanceof EntityCrankShaftBase cs && cs.myType == CrankShaftType.LARGE) {
                if (cs.getBlockState().getValue(BlockCrankShaftBase.ROTATION_AXIS) != getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()) {
                    return myMechanicalBlock;
                }
            }
        }
        return null;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public List<CrankShaftType> getConnectableCrankshafts() {
        return List.of(CrankShaftType.LARGE);
    }

    public void initStartPosOrPlaceExtensions() {
        BlockPos start = getBlockPos().relative(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        while (true) {
            BlockPos below = start.below();
            if (below.getY() < level.getMinBuildHeight()) {
                return;
            } else if (level.getBlockState(below).isAir()) {
                // place pump extension and return, no action for this cycle
                level.setBlock(below, PUMP_EXT.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)), 3);
                return;
            } else if (level.getBlockState(below).getBlock().equals(PUMP_EXT.get())) {
                // scan lower while there are extensions
                start = start.below();
            } else if (!level.getBlockState(below).getFluidState().isEmpty()) {
                // found a fluid below the pump, this will be pumped (if a source is found)
                fluidToPump = level.getBlockState(below).getFluidState().getType().getFluidType();
                nextBlocksToScan.add(below);
                return;
            } else {
                return;
            }
        }
    }

    public boolean isInfiniteWater(BlockPos p) {
        Holder<Biome> h = level.getBiome(p);
        ResourceLocation id = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(h.value());
        String idString = id.toString();
        if (Config.INSTANCE.biomes.contains(idString)) {
            return !Config.INSTANCE.isBlackList;
        } else {
            return Config.INSTANCE.isBlackList;
        }
    }

    public void tryPumpBlock(BlockPos pos) {
        BlockState targetState = level.getBlockState(pos);
        if (myTank._fill(new FluidStack(targetState.getFluidState().getType(), 1000), IFluidHandler.FluidAction.SIMULATE) == 1000) {
            myTank._fill(new FluidStack(targetState.getFluidState().getType(), 1000), IFluidHandler.FluidAction.EXECUTE);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if (!level.isClientSide) {

            progress += Math.abs(Static.rad_to_degree(myMechanicalBlock.internalVelocity) / (double) Static.TPS);
            progress = Math.min(progress, 3600);
            if (progress > 360) {
                if (nextBlocksToScan.isEmpty()) {
                    progress -= 360;
                    // reset
                    workedPositions.clear();
                    bestTarget = null;
                    initStartPosOrPlaceExtensions();

                    if (!PumpConfig.INSTANCE.consumeWater && fluidToPump.equals(Fluids.WATER.getFluidType()) && !nextBlocksToScan.isEmpty()) {
                        // no actual pickup of water
                        myTank._fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
                        nextBlocksToScan.clear();
                    }

                }
            }
            long t0 = System.nanoTime();
            int maxSteps = PumpConfig.INSTANCE.scanPerTick;
            int n = 0;
            while (!nextBlocksToScan.isEmpty()) {
                n++;
                if (n > maxSteps * 1000)
                    break;
                BlockPos next = nextBlocksToScan.pollFirst();
                if (workedPositions.add(next.asLong())) {

                    double dx = next.getX() - getBlockPos().getX();
                    double dz = next.getZ() - getBlockPos().getZ();
                    double d = dx * dx + dz * dz;
                    if (d > maxRadiusSqr)
                        continue;

                    BlockState state = level.getBlockState(next);
                    if (!state.getFluidState().getType().getFluidType().equals(fluidToPump))
                        continue;


                    if (state.getFluidState().isSource()) {
                        // remember the block most away to be pumped
                        double dist = next.distToCenterSqr(myCenter);
                        if (bestTarget == null || dist > bestTargetDistance) {
                            bestTarget = next;
                            bestTargetDistance = dist;
                        }

                        if (ModList.get().isLoaded("finite_water") && fluidToPump.equals(Fluids.WATER.getFluidType())) {
                            if (isInfiniteWater(next)) {
                                // if my finite water mod is loaded and the current scan target is in an infinite water biome, use this block as target and break scanning
                                nextBlocksToScan.clear(); // signals to stop scanning
                                bestTarget = next;
                                break;
                            }
                        }
                    }

                    // possible next directions are horizontal or up
                    // never scan down, the pump can not drain a block lower than the starting pos
                    for (Direction direction : HORIZONTAL_DIRECTIONS) {
                        BlockPos neighbor = next.relative(direction);
                        if (!workedPositions.contains(neighbor.asLong())) {
                            nextBlocksToScan.add(neighbor);
                        }
                    }

                    // special case up:
                    // we always prefer to pump from the highest water available
                    // (except if finite water is loaded, then we also pump the first infinite water to save compute)
                    BlockPos abovePos = next.relative(Direction.UP);
                    BlockState aboveState = level.getBlockState(abovePos);
                    if (!aboveState.getFluidState().isEmpty() && aboveState.getFluidState().getType().getFluidType().equals(fluidToPump)) {
                        // reset everything and use the above fluid as start position
                        bestTarget = null;
                        nextBlocksToScan.clear();
                        workedPositions.clear();
                        nextBlocksToScan.add(abovePos);
                    }
                }
            }
            if (n > 0)
                System.out.println((double) (System.nanoTime() - t0) / 1000 / 1000 + ":" + n);

            // if no next block to scan is available, everything is complete
            if (nextBlocksToScan.isEmpty()) {
                if (bestTarget != null) {
                    // pick up the water most far away (so we do not drain the local area and run out of water)
                    tryPumpBlock(bestTarget);
                    bestTarget = null;
                }
            }

            Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            for (Direction d : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
                if (!myTank.getFluid().isEmpty()) {
                    IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, getBlockPos().relative(d), d.getOpposite());
                    if (fluidHandler != null) {
                        double relativeFill = (double) myTank.getFluidAmount() / myTank.getCapacity();
                        int toExtract = (int) (100 * relativeFill);
                        toExtract = Math.max(1, toExtract);

                        int canFill = fluidHandler.fill(myTank.drain(toExtract, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.SIMULATE);
                        if (canFill > 0) {
                            fluidHandler.fill(myTank.drain(canFill, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
        myTank.readFromNBT(registries, tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
        myTank.writeToNBT(registries, tag);
    }
}
