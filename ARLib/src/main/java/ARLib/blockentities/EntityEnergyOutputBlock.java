package ARLib.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import static ARLib.ARLibRegistry.ENTITY_ENERGY_OUTPUT_BLOCK;

public class EntityEnergyOutputBlock extends EntityEnergyInputBlock {


    public EntityEnergyOutputBlock(BlockPos p_155229_, BlockState p_155230_) {
        super(ENTITY_ENERGY_OUTPUT_BLOCK.get(), p_155229_, p_155230_);
        energyStorage.canReceive = false;
        energyStorage.canExtract = true;
    }
}
