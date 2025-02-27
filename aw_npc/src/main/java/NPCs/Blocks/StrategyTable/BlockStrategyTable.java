package NPCs.Blocks.StrategyTable;

import NPCs.Blocks.TownHall.EntityTownHall;
import NPCs.Blocks.TownHall.TownHallNames;
import NPCs.Blocks.TownHall.TownHallOwners;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static NPCs.Registry.ENTITY_STRATEGY_TABLE;

public class BlockStrategyTable extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockStrategyTable() {
        super(Properties.of().noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
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

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        BlockEntity e = level.getBlockEntity(pos);
        if(e instanceof EntityStrategyTable t){
            for (int i = 0; i < t.orderInventory_fighters.getSlots(); i++) {
                Block.popResource(level,pos,t.orderInventory_fighters.getStackInSlot(i).copy());
                t.orderInventory_fighters.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }



    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }
        return null;
    }


    // this is just because it blocks light and i dont know how to prevent it else
    VoxelShape notFullBlock = Shapes.create(0.01, 0.01, 0.01, 0.99, 0.99, 0.99);
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.notFullBlock;
    }

    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

}
