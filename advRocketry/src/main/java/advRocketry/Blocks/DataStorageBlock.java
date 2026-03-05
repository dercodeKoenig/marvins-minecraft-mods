package advRocketry.Blocks;

import ARLib.multiblockCore.BlockMultiblockPart;
import advRocketry.BlockEntities.EntityCargoHold;
import advRocketry.BlockEntities.EntityDataStorageBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

import static advRocketry.Registry.ENTITY_DATA_STORAGE_BLOCK;

public class DataStorageBlock extends BlockMultiblockPart implements EntityBlock {
    public DataStorageBlock() {
        super(Properties.of());
        this.isSpecialBlock = true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_DATA_STORAGE_BLOCK.get().create(blockPos, blockState);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (super.useWithoutItem(state, level, pos, player, hitResult) == InteractionResult.PASS) {
            if (level.getBlockEntity(pos) instanceof EntityDataStorageBlock dataStorageBlock && player instanceof ServerPlayer serverPlayer)
                dataStorageBlock.openGui(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if(level.getBlockEntity(pos) instanceof EntityDataStorageBlock dataStorageBlock){
            dataStorageBlock.popInventory();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityDataStorageBlock::tick;
    }
}
