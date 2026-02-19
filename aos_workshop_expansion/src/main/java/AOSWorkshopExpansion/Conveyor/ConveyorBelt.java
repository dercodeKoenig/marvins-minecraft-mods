package AOSWorkshopExpansion.Conveyor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static AOSWorkshopExpansion.Registry.ENTITY_CONVEYOR_BELT;
import static AOSWorkshopExpansion.Registry.ENTITY_CONVEYOR_ENGINE;

public class ConveyorBelt extends Block implements EntityBlock {

    public static EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ConveyorBelt() {
        super(Properties.of().noOcclusion());
        BlockState state = this.stateDefinition.any();
        state = state.setValue(FACING, Direction.NORTH);
        this.registerDefaultState(state);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_CONVEYOR_BELT.get().create(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer != null) {
            if (!placer.isShiftKeyDown())
                level.setBlock(pos, state.setValue(FACING, placer.getDirection().getClockWise()), 3);
            else
                level.setBlock(pos, state.setValue(FACING, placer.getDirection().getCounterClockWise()), 3);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityConveyorBelt::tick;
    }

    VoxelShape myShape = Shapes.create(0, 0, 0, 1, (double) 2 / 16, 1);

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return myShape;
    }

    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

}
