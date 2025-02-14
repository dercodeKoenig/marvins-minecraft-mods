package AOSBasicFluid.Pump;

import AOSWorkshopExpansion.MillStone.EntityMillStone;
import AOSWorkshopExpansion.WoodMill.BlockWoodMill;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.network.INetworkTagReceiver;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.BlockCrankShaftBase;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.ICrankShaftConnector;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import AgeOfSteam.Static;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.*;

import static AOSBasicFluid.Registry.ENTITY_PUMP;
import static AOSBasicFluid.Registry.PUMP_EXT;

// the pump will take the water block that is most away on the highest connected y level

public class EntityPump extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver, ICrankShaftConnector {

    public double progress = 0;
    public int maxRadius = 32;
    FluidType fluidToPump = Fluids.EMPTY.getFluidType();
    List<BlockPos> nextBlocksToScan = new LinkedList<>();
    HashSet<BlockPos> workedPositions = new HashSet<>();
    TreeSet<BlockPos> waterSourceBlocks = new TreeSet<>(
            new Comparator<BlockPos>() {
                @Override
                public int compare(BlockPos o1, BlockPos o2) {
                    // sort by distance to pump
                    if(o1.getCenter().distanceTo(getBlockPos().getCenter()) > o2.getCenter().distanceTo(getBlockPos().getCenter()))
                        return 1;
                    if(o1.getCenter().distanceTo(getBlockPos().getCenter()) < o2.getCenter().distanceTo(getBlockPos().getCenter()))
                        return -1;

                    if(o1.getY() > o2.getY())
                        return 1;
                    if(o1.getY() < o2.getY())
                        return -1;

                    if(o1.getX() > o2.getX())
                        return 1;
                    if(o1.getX() < o2.getX())
                        return -1;

                    if(o1.getZ() > o2.getZ())
                        return 1;
                    if(o1.getZ() < o2.getZ())
                        return -1;

                    return 0;
                }
            }
    );

    public EntityPump(BlockPos p_155229_, BlockState p_155230_) {
        super(ENTITY_PUMP.get(), p_155229_, p_155230_);
    }


    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return 600;
        }

        @Override
        public double getInertia(Direction face) {
            return 10;
        }

        @Override
        public double getTorqueResistance(Direction face) {
            return 50;
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

    @Override
    public void onLoad() {
        myMechanicalBlock.mechanicalOnload();
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


    public void findStartPos() {
        BlockPos start = getBlockPos().relative(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        while (true) {
            BlockPos below = start.below();
            if (below.getY() < level.getMinBuildHeight()) {
                return;
            }
            else if (level.getBlockState(below).isAir()) {
                level.setBlock(below, PUMP_EXT.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)), 3);
                return;
            }
            else if (level.getBlockState(below).getBlock().equals(PUMP_EXT.get())) {
                start = start.below();
            }
            else if (!level.getBlockState(below).getFluidState().isEmpty()) {
                fluidToPump = level.getBlockState(below).getFluidState().getType().getFluidType();
                nextBlocksToScan.add(below);
                return;
            }else{
                return;
            }
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
                    workedPositions.clear();
                    waterSourceBlocks.clear();
                    findStartPos();
                }
            }

            int maxSteps = 1000;
            int n = 0;
            while (!nextBlocksToScan.isEmpty()) {
                n++;
                if(n>maxSteps)
                    break;
                BlockPos next = nextBlocksToScan.removeFirst();
                if (!workedPositions.contains(next)) {
                    workedPositions.add(next);
                    double dx = next.getX() - getBlockPos().getX();
                    double dz = next.getZ() - getBlockPos().getZ();
                    double d = dx*dx+dz*dz;
                    BlockState s = level.getBlockState(next);
                    if (s.getFluidState().getType().getFluidType().equals(fluidToPump) && d < maxRadius*maxRadius) {
                        if (s.getFluidState().isSource()) {
                            waterSourceBlocks.add(next);
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
            if(nextBlocksToScan.isEmpty()){
                if(!waterSourceBlocks.isEmpty()){
                    BlockPos target = waterSourceBlocks.last();
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                    waterSourceBlocks.clear();
                }
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityPump) t).tick();
    }
}
