package advRocketry.Blocks;

import ARLib.blocks.BlockFluidInputBlock;
import advRocketry.BlockEntities.EntityOxygenVent;
import net.minecraft.core.BlockPos;
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

import static advRocketry.Registry.ENTITY_OXYGEN_VENT;

public class OxygenVent extends BlockFluidInputBlock{

    public OxygenVent() {
        super(Properties.of());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_OXYGEN_VENT.get().create(blockPos, blockState);
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityOxygenVent::tick;
    }

}
