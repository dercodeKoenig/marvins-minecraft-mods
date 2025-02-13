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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.List;

import static AOSBasicFluid.Registry.ENTITY_PUMP;

public class EntityPump extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver, ICrankShaftConnector {
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
        if (side == myState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()) {
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

    public void tick() {
        myMechanicalBlock.mechanicalTick();
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
