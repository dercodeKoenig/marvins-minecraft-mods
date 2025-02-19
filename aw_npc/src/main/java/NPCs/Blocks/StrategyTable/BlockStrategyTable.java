package NPCs.Blocks.StrategyTable;

import NPCs.Blocks.TownHall.EntityTownHall;
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

import static NPCs.Registry.ENTITY_STRATEGY_TABLE;

public class BlockStrategyTable extends Block implements EntityBlock {
    public BlockStrategyTable() {
        super(Properties.of());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_STRATEGY_TABLE.get().create(blockPos,blockState);
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityStrategyTable::tick;
    }


    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity e = level.getBlockEntity(pos);
        if (e instanceof EntityStrategyTable t) {
            t.useWithoutItem(player);
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

}
