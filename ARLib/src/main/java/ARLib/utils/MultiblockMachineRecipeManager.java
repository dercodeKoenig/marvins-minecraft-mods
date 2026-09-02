package ARLib.utils;

import ARLib.blockentities.*;
import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

import static ARLib.utils.ItemUtils.getFluidStackFromId;
import static ARLib.utils.ItemUtils.getItemStackFromIdOrTag;

public class MultiblockMachineRecipeManager<T extends EntityMultiblockMachineMaster> {


    public int progress;
    public MachineRecipe currentRecipe;
    public List<MachineRecipe> recipes = new ArrayList<>();
    public T master;

    public MultiblockMachineRecipeManager(T masterTile) {
        this.master = masterTile;
    }

    public void reset() {
        currentRecipe = null;
        progress = 0;
    }

    public ItemFluidStacks getNextProducedItems() {
        ItemFluidStacks r = new ItemFluidStacks();
        if (currentRecipe != null) {
            for (RecipePartWithProbability i : currentRecipe.outputs) {
                ItemStack istack = getItemStackFromIdOrTag(i.id, i.getRandomAmount(), master.getLevel().registryAccess());
                FluidStack fstack = getFluidStackFromId(i.id, i.getRandomAmount());
                if (istack != null)
                    r.itemStacks.add(istack);
                if (fstack != null)
                    r.fluidStacks.add(fstack);
            }
        }
        return r;
    }

    public void scanFornewRecipe(
            List<EntityItemInputBlock> itemInTiles,
            List<EntityItemOutputBlock> itemOutTiles,
            List<EntityFluidInputBlock> fluidInTiles,
            List<EntityFluidOutputBlock> fluidOutTiles
    ) {
        for (MachineRecipe r : recipes) {
            if (master.hasinputs(new ArrayList<>(r.inputs), fluidInTiles, itemInTiles) && master.canFitOutputs(new ArrayList<>(r.outputs), fluidOutTiles, itemOutTiles)) {
                currentRecipe = r.copy(); // make a copy because they can have different actual_num values for every new recipe
                currentRecipe.computeRandomAmounts(); // roll the dice to compute input / output to consume for given probability
                break;
            }
        }
    }

    // returns true if it was a processing tick, false if not. can be used to check if the machine is running
    public boolean update() {
        List<EntityItemInputBlock> itemInTiles = master.getItemInTiles();
        List<EntityItemOutputBlock> itemOutTiles = master.getItemOutTiles();
        List<EntityFluidInputBlock> fluidInTiles = master.getFluidInTiles();
        List<EntityFluidOutputBlock> fluidOutTiles = master.getFluidOutTiles();
        List<EntityEnergyInputBlock> energyInTiles = master.getEnergyInputTiles();
        if (currentRecipe == null) {
            scanFornewRecipe(itemInTiles, itemOutTiles, fluidInTiles, fluidOutTiles);
            return false;
        }
        if (master.hasinputs(new ArrayList<>(currentRecipe.inputs), fluidInTiles, itemInTiles) &&
                master.canFitOutputs(new ArrayList<>(currentRecipe.outputs), fluidOutTiles, itemOutTiles)) {
            if (master.getTotalEnergyStored(energyInTiles) >= currentRecipe.energyPerTick) {
                progress += 1;
                master.consumeEnergy(currentRecipe.energyPerTick, energyInTiles);
                if (progress >= currentRecipe.ticksRequired) {
                    master.consumeInput(currentRecipe.inputs, false, fluidInTiles, itemInTiles);
                    master.produceOutput(currentRecipe.outputs, fluidOutTiles, itemOutTiles);
                    master.produceEnergy(currentRecipe.outputEnergy, new ArrayList<>(master.getEnergyOutputTiles()));
                    reset();
                }
                return true;
            }
        } else {
            reset();
        }
        return false;
    }
}
