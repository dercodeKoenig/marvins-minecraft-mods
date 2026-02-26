package advRocketry.Blocks;

import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.BlockEntities.EntitySpaceStationAssembler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static advRocketry.Registry.ENTITY_ROCKET_ASSEMBLER;
import static advRocketry.Registry.ENTITY_SPACE_STATION_ASSEMBLER;


public class SpaceStationAssembler extends RocketAssembler implements EntityBlock {

    public SpaceStationAssembler() {
        super();
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_SPACE_STATION_ASSEMBLER.get().create(blockPos, blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntitySpaceStationAssembler::tick;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        BlockEntity be = level.getBlockEntity(pos);
        if(be instanceof EntitySpaceStationAssembler spaceStationAssembler){
            spaceStationAssembler.popInventory();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

}
