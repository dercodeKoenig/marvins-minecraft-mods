package NPCs.Npc.programs.Mining;

import NPCs.Npc.WorkerNPC;
import NPCs.Npc.programs.UnloadInventoryProgram;
import WorkSites.Quarry.EntityQuarry;
import net.minecraft.world.item.ItemStack;

import static NPCs.Utils.*;

public class UnloadInventoryToFarmProgram {
    public WorkerNPC worker;
    public UnloadInventoryProgram unloadInventoryProgram;
    ItemStack nextStackToUnload = ItemStack.EMPTY;

    boolean canUnloadToMain = false;
    boolean canUnloadToResources = false;
    boolean canUnloadToSpecialResources = false;

    public UnloadInventoryToFarmProgram(WorkerNPC worker) {
        this.worker = worker;
        unloadInventoryProgram = new UnloadInventoryProgram(worker);
    }

    public boolean recalculateHasWork(EntityQuarry farm) {

        canUnloadToMain = false;
        canUnloadToResources = false;
        canUnloadToSpecialResources = false;

        ItemStack unloadSR = UnloadInventoryProgram.getAnyItemToUnload(farm.specialResourcesInventory, worker);
        if(!unloadSR.isEmpty())
            canUnloadToSpecialResources = true;

        ItemStack unloadR= UnloadInventoryProgram.getAnyItemToUnload(farm.inputsInventory, worker);
        if(!unloadR.isEmpty())
            canUnloadToResources = true;

        ItemStack unloadM = UnloadInventoryProgram.getAnyItemToUnload(farm.mainInventory, worker);
        if(!unloadM.isEmpty())
            canUnloadToMain = true;

        nextStackToUnload = ItemStack.EMPTY;

        if(canUnloadToSpecialResources)
            nextStackToUnload = unloadSR;
        else if(canUnloadToResources)
            nextStackToUnload = unloadR;
        else if(canUnloadToMain)
            nextStackToUnload = unloadM;

        return !nextStackToUnload.isEmpty();
    }

    public int run(EntityQuarry farm) {

        // todo: maybe not scan every tick?
        recalculateHasWork(farm);

        if (nextStackToUnload.isEmpty())
            return EXIT_SUCCESS;

        int exit = EXIT_SUCCESS;
        if (canUnloadToSpecialResources)
            exit = unloadInventoryProgram.run(farm.specialResourcesInventory, farm.getBlockPos(), nextStackToUnload);

        else if (canUnloadToResources)
            exit = unloadInventoryProgram.run(farm.inputsInventory, farm.getBlockPos(), nextStackToUnload);

        else if (canUnloadToMain)
            exit = unloadInventoryProgram.run(farm.mainInventory, farm.getBlockPos(), nextStackToUnload);

        if(exit == EXIT_FAIL){
            return EXIT_FAIL;
        }

        // it will return exit success if it was able to unload one item but in this case, the program should keep running and not exit
        return SUCCESS_STILL_RUNNING;
    }

}
