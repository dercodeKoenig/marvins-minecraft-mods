package advRocketry.Blocks;

import ARLib.blocks.BlockFluidInputBlock;
import advRocketry.BlockEntities.EntityFuelingStation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static advRocketry.Registry.ENTITY_FUELING_STATION;

public class FuelingStation extends BlockFluidInputBlock implements EntityBlock {
    public FuelingStation() {
        super(Properties.of());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_FUELING_STATION.get().create(blockPos, blockState);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EntityFuelingStation fuelingStation) {
            if (fuelingStation.linkedRocket != null) {
                if(fuelingStation.linkedRocket.fuelTank.getCapacity() == fuelingStation.linkedRocket.getFuel()) {
                    return 15;
                }
            }
        }
        return 0;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityFuelingStation::tick;
    }
}
