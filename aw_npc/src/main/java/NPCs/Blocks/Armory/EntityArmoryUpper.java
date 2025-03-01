package NPCs.Blocks.Armory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static NPCs.Registry.ENTITY_ARMORY_UPPER;


// this thing is really only to fix the IItemHandler Cap not available when using the upper block
// this is bad if a user clicks the upper half with the routing order and without this fix, it would never work
public class EntityArmoryUpper extends BlockEntity {
    public EntityArmoryUpper(BlockPos pos, BlockState blockState) {
        super(ENTITY_ARMORY_UPPER.get(), pos, blockState);
    }
}
