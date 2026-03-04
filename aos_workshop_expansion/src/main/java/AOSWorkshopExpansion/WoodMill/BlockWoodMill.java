package AOSWorkshopExpansion.WoodMill;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockMaster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import static AOSWorkshopExpansion.Registry.ENTITY_WOODMILL;

public class BlockWoodMill extends BlockMultiblockMaster implements EntityBlock {

    public BlockWoodMill() {
        super(Properties.of().noOcclusion().strength(1.0f));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_WOODMILL.get().create(blockPos,blockState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockEntity me = level.getBlockEntity(pos);
        if(me instanceof EntityMultiblockMaster mm){
            mm.scanStructure();
        }
            return state;
    }

    /* managed by BlockMultiblockMaster
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity b = level.getBlockEntity(pos);
        if(b instanceof EntityWoodMill h)
            return h.use(player);
        return InteractionResult.PASS;
    }
     */

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if(blockEntity instanceof EntityWoodMill s){
            s.removeCurrentInputStacks();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityWoodMill::tick;
    }
}
