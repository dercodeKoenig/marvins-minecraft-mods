package advRocketry.Blocks;

import advRocketry.BlockEntities.EntityPressureTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static advRocketry.Registry.BlockEntities.ENTITY_PRESSURE_TANK;


public class PressureTank extends Block implements EntityBlock {

    public static BooleanProperty connectedBelow = BooleanProperty.create("connected_down");
    public static BooleanProperty connectedAbove = BooleanProperty.create("connected_up");

    public PressureTank() {
        
        super(Properties.of()
            .destroyTime(0.3f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.GLASS)
            .noOcclusion()
        );
        registerDefaultState(
                defaultBlockState()
                        .setValue(connectedAbove, false)
                        .setValue(connectedBelow, false)
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_PRESSURE_TANK.get().create(blockPos, blockState);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        if (direction == Direction.DOWN) {
            if (neighborState.getBlock() instanceof PressureTank) {
                state = state.setValue(connectedBelow, true);
            } else {
                state = state.setValue(connectedBelow, false);
            }
        }

        if (direction == Direction.UP) {
            if (neighborState.getBlock() instanceof PressureTank) {
                state = state.setValue(connectedAbove, true);
            } else {
                state = state.setValue(connectedAbove, false);
            }
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(connectedBelow);
        builder.add(connectedAbove);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityPressureTank::tick;
    }

    protected VoxelShape getVisualShape(BlockState p_309057_, BlockGetter p_308936_, BlockPos p_308956_, CollisionContext p_309006_) {
        return Shapes.empty();
    }

    protected float getShadeBrightness(BlockState p_308911_, BlockGetter p_308952_, BlockPos p_308918_) {
        return 1.0F;
    }

    protected boolean propagatesSkylightDown(BlockState p_309084_, BlockGetter p_309133_, BlockPos p_309097_) {
        return true;
    }
}