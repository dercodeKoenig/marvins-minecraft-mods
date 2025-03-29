package ARMachines.crystallizer;


import ARLib.multiblockCore.BlockMultiblockMaster;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import static ARMachines.MultiblockRegistry.ENTITY_CRYSTALLIZER;

public class BlockCrystallizer extends BlockMultiblockMaster {

    public BlockCrystallizer(Properties properties) {super(properties);}

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_CRYSTALLIZER.get().create(blockPos,blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityCrystallizer::tick;
    }
}
