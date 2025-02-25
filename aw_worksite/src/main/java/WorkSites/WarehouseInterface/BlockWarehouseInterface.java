package WorkSites.WarehouseInterface;

import WorkSites.EntityWorkSiteBase;
import WorkSites.WarehouseCrafter.MenuWarehouseCrafter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static WorkSites.Registry.ENTITY_WAREHOUSE_CRAFTER;
import static WorkSites.Registry.ENTITY_WAREHOUSE_INTERFACE;

public class BlockWarehouseInterface extends Block implements EntityBlock {

    public BlockWarehouseInterface() {
        super(Properties.of().noOcclusion());
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_WAREHOUSE_INTERFACE.get().create(blockPos, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            BlockEntity station = level.getBlockEntity(pos);
            if (station instanceof EntityWarehouseInterface e) {
                e.guiHandler.openGui(180, 190, true);
            }
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!(newState.getBlock() instanceof BlockWarehouseInterface)) {
            BlockEntity e = level.getBlockEntity(pos);
            if (e instanceof EntityWarehouseInterface r) {
                r.popInventory();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityWarehouseInterface::tick;
    }
}
