package WorkSites.Warehouse;

import WorkSites.BlockWorkSiteBase;
import WorkSites.EntityWorkSiteBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static WorkSites.Registry.ENTITY_WAREHOUSE;

public class BlockWarehouse extends BlockWorkSiteBase {
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_WAREHOUSE.get().create(blockPos, blockState);
    }
}
