package advRocketry.Blocks;

import advRocketry.BlockEntities.EntitySatelliteMonitor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static advRocketry.Registry.BlockEntities.ENTITY_SATELLITE_MONITOR;

public class SatelliteMonitor extends Block implements EntityBlock {

    public static EnumProperty<State> STATE = EnumProperty.create("state", State.class);

    public SatelliteMonitor() {
        super(Properties.of());
        registerDefaultState(getStateDefinition().any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(SatelliteMonitor.STATE, SatelliteMonitor.State.idle)
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_SATELLITE_MONITOR.get().create(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
        builder.add(SatelliteMonitor.STATE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()).setValue(SatelliteMonitor.STATE, SatelliteMonitor.State.idle);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof EntitySatelliteMonitor satelliteMonitor) {
                satelliteMonitor.openGui();
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof EntitySatelliteMonitor satelliteMonitor && !satelliteMonitor.isValidBlockState(newState)) {
            satelliteMonitor.popInventory();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntitySatelliteMonitor::tick;
    }

    public enum State implements StringRepresentable {
        idle("idle"),
        satellite_connected("satellite_connected"),
        active("active");

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
