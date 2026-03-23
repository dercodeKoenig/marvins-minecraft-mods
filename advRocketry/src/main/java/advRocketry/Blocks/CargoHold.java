package advRocketry.Blocks;

import advRocketry.BlockEntities.EntityCargoHold;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

import static advRocketry.Registry.BlockEntities.ENTITY_CARGO_HOLD;

public class CargoHold extends Block implements EntityBlock {
    public CargoHold() {
        super(Properties.of().destroyTime(0.5f));
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_CARGO_HOLD.get().create(blockPos, blockState);
    }


    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if(level.getBlockEntity(pos) instanceof EntityCargoHold cargoHold)
            cargoHold.openGui();
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if(level.getBlockEntity(pos) instanceof EntityCargoHold cargoHold){
            cargoHold.popInventory();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityCargoHold::tick;
    }

}
