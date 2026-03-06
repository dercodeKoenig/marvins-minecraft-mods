package ARLib.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.EnergyStorage;

public class BlockEntityBattery extends EnergyStorage {

    public BlockEntity parent;
    public boolean canExtract = true;
    public boolean canReceive = true;

    public BlockEntityBattery(BlockEntity parent, int capacity) {
        super(capacity);
        this.parent = parent;
    }

    public BlockEntityBattery(BlockEntity parent, int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
        this.parent = parent;
    }

    public BlockEntityBattery(BlockEntity parent, int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
        this.parent = parent;
    }

    public BlockEntityBattery(BlockEntity parent, int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
        this.parent = parent;
    }
    public void setEnergy(int e){
        energy = e;
        parent.setChanged();
    }
    public void setCapacity(int c){
        this.capacity = c;
        parent.setChanged();
    }

    public int receiveEnergy(int toReceive, boolean simulate) {
      int received = super.receiveEnergy(toReceive,simulate);
      if(received != 0 && !simulate)
          parent.setChanged();
      return received;
    }

    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(toExtract,simulate);
        if(extracted != 0 && !simulate)
            parent.setChanged();
        return extracted;
    }

    public boolean canExtract() {
        return super.canExtract() && canExtract;
    }

    public boolean canReceive() {
        return super.canReceive() && canReceive;
    }
}
