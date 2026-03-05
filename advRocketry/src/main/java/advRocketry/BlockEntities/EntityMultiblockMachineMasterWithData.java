package advRocketry.BlockEntities;

import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;

abstract public class EntityMultiblockMachineMasterWithData extends EntityMultiblockMachineMaster {

    public EntityMultiblockMachineMasterWithData(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }
}
