package advRocketry.Blocks;

import advRocketry.BlockEntities.EntitySpaceStationAssembler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static advRocketry.Registry.BlockEntities.ENTITY_SPACE_STATION_ASSEMBLER;


public class SpaceStationAssembler extends RocketAssembler implements EntityBlock {

    public SpaceStationAssembler() {
        super();
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_SPACE_STATION_ASSEMBLER.get().create(blockPos, blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntitySpaceStationAssembler::tick;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if(level.getBlockEntity(pos) instanceof EntitySpaceStationAssembler spaceStationAssembler){
            spaceStationAssembler.popInventory();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

}
