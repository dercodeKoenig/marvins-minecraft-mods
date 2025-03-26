package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class guiModuleEnergy extends guiModuleVerticalProgressBar {

    final IEnergyStorage energyStorage;

    public int maxEnergy;
    public int energy;
    int last_energy;
    int last_maxEnergy;

    int last_update = 0;
    @Override
    public void serverTick(){
        last_update+=1;
        energy = energyStorage.getEnergyStored();
        maxEnergy = energyStorage.getMaxEnergyStored();
        // update every x ticks
        if ((energy != last_energy || last_maxEnergy != energyStorage.getMaxEnergyStored())&& last_update > 2){
            last_update = 0;
            last_energy = energy;
            last_maxEnergy = energyStorage.getMaxEnergyStored();

            setHoverInfoAndSync(energy + "/" + maxEnergy + "RF");
            setProgressAndSync((float)energy/maxEnergy);
        }
    }


    public guiModuleEnergy(int id, IEnergyStorage energyStorage, IGuiHandler guiHandler, int x, int y){
        super(id,guiHandler,x,y);
        this.energyStorage = energyStorage;
    }
}
