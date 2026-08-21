package ARLib.blockentities;

import ARLib.utils.BlockEntityBattery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import static ARLib.ARLibRegistry.ENTITY_ENERGY_OUTPUT_BLOCK;

public class EntityEnergyOutputBlock extends EntityEnergyInputBlock {


    public EntityEnergyOutputBlock(BlockPos p_155229_, BlockState p_155230_) {
        super(ENTITY_ENERGY_OUTPUT_BLOCK.get(), p_155229_, p_155230_);
        // avoid having energy inserted by other mods
        // use insert internal method to insert
        this.energyStorage.canReceive = false;
    }

    @Override
    public void tick() {
        super.tick();

        // output energy to nearby energy handlers
        for (Direction i : Direction.allShuffled(level.random)) {
            if (energyStorage.getEnergyStored() == 0)
                break;
            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, getBlockPos().relative(i), i.getOpposite());
            if (neighbor != null) {
                int toMove = energyStorage.getEnergyStored();
                int received = neighbor.receiveEnergy(toMove, false);
                energyStorage.extractInternal(received, false);
            }
        }
    }
}
