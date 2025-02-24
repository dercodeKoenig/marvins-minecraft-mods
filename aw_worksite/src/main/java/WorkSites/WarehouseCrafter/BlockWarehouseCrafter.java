package WorkSites.WarehouseCrafter;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static WorkSites.Registry.ENTITY_WAREHOUSE_CRAFTER;

///  this entire Package is almost exact copy of the Engineering Station from the research system

public class BlockWarehouseCrafter extends Block implements EntityBlock {
    //public static BooleanProperty HAS_BOOK = BooleanProperty.create("has_book");

    public BlockWarehouseCrafter() {
        super(Properties.of().noOcclusion());
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                //.setValue(HAS_BOOK, false)
                );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite())
                //.setValue(HAS_BOOK, false)
                ;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
        //builder.add(HAS_BOOK);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_WAREHOUSE_CRAFTER.get().create(blockPos, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer s) {
            BlockEntity station = level.getBlockEntity(pos);
            if (station instanceof EntityWarehouseCrafter e) {
                s.openMenu(
                        new SimpleMenuProvider(
                                (x, y, z) ->
                                        new MenuWarehouseCrafter(x, y, e), Component.literal("Engineering Station")
                        ), pos
                );
            }
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!(newState.getBlock() instanceof BlockWarehouseCrafter)) {
            BlockEntity e = level.getBlockEntity(pos);
            if (e instanceof EntityWarehouseCrafter r) {
                r.popInventory();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
