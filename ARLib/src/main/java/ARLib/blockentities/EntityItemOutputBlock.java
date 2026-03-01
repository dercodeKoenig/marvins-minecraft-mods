package ARLib.blockentities;


import ARLib.network.INetworkTagReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import static ARLib.ARLibRegistry.ENTITY_ITEM_OUTPUT_BLOCK;

public class EntityItemOutputBlock extends EntityItemInputBlock implements INetworkTagReceiver {

    public EntityItemOutputBlock(BlockPos pos, BlockState blockState) {
        super(ENTITY_ITEM_OUTPUT_BLOCK.get(),pos, blockState);
    }
}
