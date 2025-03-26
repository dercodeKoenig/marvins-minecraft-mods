package ARMachines.rollingMachine;


import ARLib.multiblockCore.BlockMultiblockMaster;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

import static ARMachines.MultiblockRegistry.ENTITY_LATHE;
import static ARMachines.MultiblockRegistry.ENTITY_ROLLINGMACHINE;

public class BlockRollingMachine extends BlockMultiblockMaster {

    public BlockRollingMachine(Properties properties) {super(properties);}

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_ROLLINGMACHINE.get().create(blockPos,blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityRollingMachine::tick;
    }
}
