package advRocketry.Blocks;

import ARLib.blockentities.EntityFluidInputBlock;
import ARLib.blocks.BlockFluidInputBlock;
import advRocketry.BlockEntities.EntityFuelingStation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static advRocketry.Registry.ENTITY_FUELING_STATION;

public class FuelingStation extends Block implements EntityBlock {
    public FuelingStation() {
        super(Properties.of());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_FUELING_STATION.get().create(blockPos, blockState);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EntityFuelingStation fuelingStation) {
            if (fuelingStation.linkedRocket != null) {
                if(fuelingStation.linkedRocket.fuelTank.getCapacity() == fuelingStation.linkedRocket.getFuel()) {
                    return 15;
                }
            }
        }
        return 0;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof EntityFuelingStation fuelingStation) {
                fuelingStation.guiHandler.openGui(176, 165, true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof EntityFuelingStation fuelingStation) {
            fuelingStation.simpleFluidContainer.popItems(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityFuelingStation::tick;
    }
}
