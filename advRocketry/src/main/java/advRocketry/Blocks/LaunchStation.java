package advRocketry.Blocks;

import advRocketry.BlockEntities.EntityLaunchStation;
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

import static advRocketry.Registry.ENTITY_LAUNCH_STATION;

public class LaunchStation extends Block implements EntityBlock {

    public static EnumProperty<State> STATE = EnumProperty.create("state", State.class);

    public enum State implements StringRepresentable {
        idle("idle"),
        rocket_landed("rocket_landed"),
        active("active");

        public final String name;
        State(String name){
            this.name = name;
        }
        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public LaunchStation() {
        super(Properties.of());
        registerDefaultState(getStateDefinition().any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(STATE, State.idle)
        );
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_LAUNCH_STATION.get().create(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
        builder.add(STATE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite())
                .setValue(STATE, State.idle)
                ;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if(level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof EntityLaunchStation launchStation) {
            if (level.hasNeighborSignal(pos)) {
                if(!launchStation.isRedstonePowered)
                    launchStation.launch();
                launchStation.isRedstonePowered = true;
            } else {
                launchStation.isRedstonePowered = false;
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof EntityLaunchStation launchStation) {
                launchStation.guiHandler.openGui(176, 135, true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof EntityLaunchStation launchStation && !launchStation.isValidBlockState(newState)) {
            launchStation.popInventory();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityLaunchStation::tick;
    }
}
