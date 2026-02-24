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


    public VertexBuffer vertexBufferArm1;
    public MeshData meshArm1;
    public VertexBuffer vertexBufferArm2;
    public MeshData meshArm2;
    public VertexBuffer vertexBufferArm3;
    public MeshData meshArm3;
    public int lastLight;

    public int maxRadiusSqr = PumpConfig.INSTANCE.maxRadius * PumpConfig.INSTANCE.maxRadius;

    public PumpFluidTank myTank = new PumpFluidTank(PumpConfig.INSTANCE.tankCapacity) {
        @Override
        public void onContentsChanged() {
            setChanged();
        }
    };

    public double progress = 0;
    FluidType fluidToPump = Fluids.EMPTY.getFluidType();
    List<BlockPos> nextBlocksToScan = new LinkedList<>();
    HashSet<BlockPos> workedPositions = new HashSet<>();
    TreeSet<BlockPos> waterSourceBlocks = new TreeSet<>(
            new Comparator<BlockPos>() {
                @Override
                public int compare(BlockPos o1, BlockPos o2) {
                    // sort by distance to pump
                    if (o1.getCenter().distanceTo(getBlockPos().getCenter()) > o2.getCenter().distanceTo(getBlockPos().getCenter()))
                        return 1;
                    if (o1.getCenter().distanceTo(getBlockPos().getCenter()) < o2.getCenter().distanceTo(getBlockPos().getCenter()))
                        return -1;

                    if (o1.getY() > o2.getY())
                        return 1;
                    if (o1.getY() < o2.getY())
                        return -1;

                    if (o1.getX() > o2.getX())
                        return 1;
                    if (o1.getX() < o2.getX())
                        return -1;

                    if (o1.getZ() > o2.getZ())
                        return 1;
                    if (o1.getZ() < o2.getZ())
                        return -1;

                    return 0;
                }
            }
    );

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

    @Override
    public void onLoad() {
        myMechanicalBlock.mechanicalOnload();
        super.onLoad();
    }

    double force = 0;
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
            return force;
        }

        @Override
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            return 1;
        }
    };


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


    public void findStartPos() {
        BlockPos start = getBlockPos().relative(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        while (true) {
            BlockPos below = start.below();
            if (below.getY() < level.getMinBuildHeight()) {
                return;
            } else if (level.getBlockState(below).isAir()) {
                level.setBlock(below, PUMP_EXT.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)), 3);
                return;
            } else if (level.getBlockState(below).getBlock().equals(PUMP_EXT.get())) {
                start = start.below();
            } else if (!level.getBlockState(below).getFluidState().isEmpty()) {
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
            if (Config.INSTANCE.isBlackList) {
                return false;
            }
        } else {
            if (!Config.INSTANCE.isBlackList) {
                return false;
            }
        }
        return true;
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
            double trig_res_force_multiplier = Math.sin(myMechanicalBlock.currentRotation / 180 * Math.PI);
            force = -PumpConfig.INSTANCE.unevenForceMultiplier * trig_res_force_multiplier;

            progress += Math.abs(Static.rad_to_degree(myMechanicalBlock.internalVelocity) / (double) Static.TPS);
            progress = Math.min(progress, 3600);
            if (progress > 360) {
                if (nextBlocksToScan.isEmpty()) {
                    progress -= 360;
                    workedPositions.clear();
                    waterSourceBlocks.clear();
                    findStartPos();

                    if (!PumpConfig.INSTANCE.consumeWater && fluidToPump.equals(Fluids.WATER.getFluidType()) && !nextBlocksToScan.isEmpty()) {
                        // no actual pickup of water
                        myTank._fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
                        nextBlocksToScan.clear();
                    }

                }
            }

            int maxSteps = PumpConfig.INSTANCE.scanPerTick;
            int n = 0;
            while (!nextBlocksToScan.isEmpty()) {
                n++;
                if (n > maxSteps)
                    break;
                BlockPos next = nextBlocksToScan.removeFirst();
                if (!workedPositions.contains(next)) {
                    workedPositions.add(next);
                    double dx = next.getX() - getBlockPos().getX();
                    double dz = next.getZ() - getBlockPos().getZ();
                    double d = dx * dx + dz * dz;
                    BlockState s = level.getBlockState(next);
                    if (s.getFluidState().getType().getFluidType().equals(fluidToPump) && d < maxRadiusSqr) {
                        if (s.getFluidState().isSource()) {
                            waterSourceBlocks.add(next);

                            if (ModList.get().isLoaded("finite_water") && fluidToPump.equals(Fluids.WATER.getFluidType())) {
                                if (isInfiniteWater(next)) {
                                    // if my finite water mod is loaded and the current scan target is in a infinite water biome, use this block as target and break scanning
                                    nextBlocksToScan.clear();
                                    waterSourceBlocks.clear();
                                    waterSourceBlocks.add(next);
                                    break;
                                }
                            }
                        }

                        nextBlocksToScan.add(next.relative(Direction.SOUTH));
                        nextBlocksToScan.add(next.relative(Direction.EAST));
                        nextBlocksToScan.add(next.relative(Direction.NORTH));
                        nextBlocksToScan.add(next.relative(Direction.WEST));

                        BlockState aboveState = level.getBlockState(next.relative(Direction.UP));
                        if (!aboveState.getFluidState().isEmpty() && aboveState.getFluidState().getType().getFluidType().equals(fluidToPump)) {
                            waterSourceBlocks.clear();
                            nextBlocksToScan.clear();
                            workedPositions.clear();
                            nextBlocksToScan.add(next.relative(Direction.UP));
                        }

                    }
                }
            }
            if (nextBlocksToScan.isEmpty()) {
                if (!waterSourceBlocks.isEmpty()) {
                    BlockPos target = waterSourceBlocks.last();
                    tryPumpBlock(target);
                    waterSourceBlocks.clear();
                }
            }


            for (Direction d : new Direction[]{getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise(), getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getCounterClockWise()}) {
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

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityPump) t).tick();
    }
}
