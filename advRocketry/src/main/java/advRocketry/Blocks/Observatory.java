package advRocketry.Blocks;

import ARLib.multiblockCore.BlockMultiblockMaster;
import advRocketry.BlockEntities.EntityObservatory;
import advRocketry.Registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class Observatory extends BlockMultiblockMaster {
    public static EnumProperty<TaskState> TASK_STATE = EnumProperty.create("task_state", TaskState.class);

    public Observatory() {
        super(Properties.of()
            .destroyTime(2.0f)
            .requiresCorrectToolForDrops()
        );
        registerDefaultState(defaultBlockState().setValue(TASK_STATE, TaskState.idle));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TASK_STATE);
    }

    @Nonnull
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(STATE_MULTIBLOCK_FORMED, false)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite())
                .setValue(TASK_STATE, TaskState.idle);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return BlockEntities.ENTITY_OBSERVATORY.get().create(blockPos, blockState);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof EntityObservatory observatory && !observatory.isValidBlockState(newState)) {
            observatory.popInventory();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityObservatory::tick;
    }

    public enum TaskState implements StringRepresentable {
        idle("idle"),
        scanning_planet("scanning_planet"),
        scanning_asteroid("scanning_asteroid"),
        searching_planet("searching_planet");

        public final String name;

        TaskState(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
