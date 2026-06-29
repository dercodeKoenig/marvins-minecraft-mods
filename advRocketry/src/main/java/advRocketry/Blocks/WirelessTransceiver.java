package advRocketry.Blocks;

import advRocketry.BlockEntities.EntityCargoHold;
import advRocketry.BlockEntities.EntityWirelessTransceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

import static advRocketry.Registry.BlockEntities.ENTITY_CARGO_HOLD;
import static advRocketry.Registry.BlockEntities.ENTITY_WIRELESS_TRANSCEIVER;

public class WirelessTransceiver extends Block implements EntityBlock {

    public static EnumProperty<State> STATE = EnumProperty.create("state", State.class);

    public WirelessTransceiver() {
        super(Properties.of()
            .strength(0.2f)
            .requiresCorrectToolForDrops()
            .noOcclusion()
        );
        registerDefaultState(
                getStateDefinition().any()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH)
                        .setValue(STATE, State.not_connected)
        );
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STATE);
        builder.add(BlockStateProperties.FACING);
    }

    @Nonnull
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(BlockStateProperties.FACING, context.getClickedFace())
                .setValue(STATE, State.not_connected);
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_WIRELESS_TRANSCEIVER.get().create(blockPos, blockState);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // copied from bush block
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // copied from wall torch block
        Direction facing = state.getValue(BlockStateProperties.FACING);
        BlockPos neighbor = pos.relative(facing.getOpposite());
        BlockState blockstate = level.getBlockState(neighbor);
        return blockstate.isFaceSturdy(level, neighbor, facing);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityWirelessTransceiver::tick;
    }

    // Cache the 6 directional shapes based on the 4x4x12 to 12x12x16 model coordinates
    // North means the block is placed on the South wall, facing North.
    protected static final VoxelShape SHAPE_NORTH = Block.box(4.0D, 4.0D, 12.0D, 12.0D, 12.0D, 16.0D);
    protected static final VoxelShape SHAPE_SOUTH = Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 4.0D);
    protected static final VoxelShape SHAPE_WEST = Block.box(12.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D);
    protected static final VoxelShape SHAPE_EAST = Block.box(0.0D, 4.0D, 4.0D, 4.0D, 12.0D, 12.0D);
    protected static final VoxelShape SHAPE_UP = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 4.0D, 12.0D);
    protected static final VoxelShape SHAPE_DOWN = Block.box(4.0D, 12.0D, 4.0D, 12.0D, 16.0D, 12.0D);

    @Override
    @Nonnull
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Return the correct cached shape based on the block's current facing direction
        return switch (state.getValue(BlockStateProperties.FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            default -> SHAPE_NORTH;
        };
    }

    public enum State implements StringRepresentable {
        not_connected("not_connected"),
        sender("sender"),
        receiver("receiver"),
        sender_active("sender_active"),
        receiver_active("receiver_active");

        public final String name;

        State(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
