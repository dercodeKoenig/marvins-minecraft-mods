package AOSWorkshopExpansion.Conveyor;

import ARLib.network.INetworkTagReceiver;
import AgeOfSteam.Blocks.Mechanics.Axle.EntityAxleBase;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;

import static AOSWorkshopExpansion.Registry.ENTITY_CONVEYOR_ENGINE;

public class EntityConveyorEngine extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver {

    public double myInertia = 1;
    public double myFriction = 1;
    public double maxStress = 600;

    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return maxStress;
        }

        @Override
        public double getInertia(Direction face) {
            return myInertia;
        }

        @Override
        public double getTorqueResistance(Direction face) {
            return myFriction;
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

    public EntityConveyorEngine(BlockPos pos, BlockState blockState) {
        super(ENTITY_CONVEYOR_ENGINE.get(), pos, blockState);
    }

    @Override
    public void onLoad(){
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityConveyorEngine) t).tick();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        myMechanicalBlock.mechanicalReadServer(compoundTag,serverPlayer);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadClient(compoundTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction direction) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof ConveyorEngine) {
            Direction.Axis blockAxis = myState.getValue(ConveyorEngine.AXIS);
            if (direction.getAxis() == blockAxis && direction.getAxis() != Direction.Axis.Y) {
                return myMechanicalBlock;
            }

            if(direction == Direction.UP){
                // above check for engine block
                BlockState above = level.getBlockState(getBlockPos().above());
                if (above.getBlock() instanceof ConveyorBelt) {
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
}
