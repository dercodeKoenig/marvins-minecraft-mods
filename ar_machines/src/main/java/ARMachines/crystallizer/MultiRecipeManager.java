package ARMachines.crystallizer;

import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import ARLib.utils.MachineRecipe;
import ARLib.utils.MultiblockMachineRecipeManager;
import ARLib.utils.RecipePart;
import ARLib.utils.RecipePartWithProbability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MultiRecipeManager<T extends EntityMultiblockMachineMaster> {

    public List<MultiblockMachineRecipeManager<T>> recipeManagers;

    public MultiRecipeManager(List<MultiblockMachineRecipeManager<T>> recipeManagers) {
        this.recipeManagers = recipeManagers;
    }

    public List<RecipePartWithProbability> getReservedInputs() {
        List<RecipePartWithProbability> reservedInputs = new ArrayList<>();
        for (MultiblockMachineRecipeManager<T> i : recipeManagers) {
            if (i.currentRecipe != null) {
                reservedInputs.addAll(i.currentRecipe.inputs);
            }
        }
        return reservedInputs;
    }

    public List<RecipePartWithProbability> getReservedOutputs() {
        List<RecipePartWithProbability> reservedOutputs = new ArrayList<>();
        for (MultiblockMachineRecipeManager<T> i : recipeManagers) {
            if (i.currentRecipe != null) {
                reservedOutputs.addAll(i.currentRecipe.outputs);
            }
        }
        return reservedOutputs;
    }

    public List<RecipePartWithProbability> addedIdsAndNumsIgnoreP(List<RecipePartWithProbability> list) {
        Map<String, Integer> ids_nums = new HashMap<>();
        for (RecipePartWithProbability i : list) {
            if (ids_nums.containsKey(i.id)) {
                ids_nums.put(i.id, ids_nums.get(i.id) + i.amount);
            } else {
                ids_nums.put(i.id, i.amount);
            }
        }
        List<RecipePartWithProbability> list2 = new ArrayList<>();
        for (String i : ids_nums.keySet()) {
            int num = ids_nums.get(i);
            RecipePartWithProbability newPart = new RecipePartWithProbability(i, num, 0);
            list2.add(newPart);
        }
        return list2;
    }

    public void scanFornewRecipe(MultiblockMachineRecipeManager<T> m) {
        List<RecipePartWithProbability> reservedOutputs = getReservedOutputs();
        List<RecipePartWithProbability> reservedInputs = getReservedInputs();

        for (int i = 0; i < m.recipes.size(); i++) {
            MachineRecipe r = m.recipes.get(i);
            List<RecipePartWithProbability> totalRequiredInputs = new ArrayList<>(reservedInputs);
            List<RecipePartWithProbability> totalRequiredOutputs = new ArrayList<>(reservedOutputs);
            totalRequiredInputs.addAll(r.inputs);
            totalRequiredOutputs.addAll(r.outputs);
            totalRequiredOutputs = addedIdsAndNumsIgnoreP(totalRequiredOutputs);
            totalRequiredInputs = addedIdsAndNumsIgnoreP(totalRequiredInputs);

            if (m.master.hasinputs(new ArrayList<RecipePart>(totalRequiredInputs), m.master.getFluidInTiles(), m.master.getItemInTiles()) && m.master.canFitOutputs(new ArrayList<RecipePart>(totalRequiredOutputs), m.master.getFluidOutTiles(), m.master.getItemOutTiles())) {
                m.currentRecipe = r.copy();
                m.currentRecipe.computeRandomAmounts();
                break;
            }
        }

    }

    public List<Boolean> update() {
        List<Boolean> rets = new ArrayList<>();

        for (MultiblockMachineRecipeManager<T> i : recipeManagers) {
            if (i.currentRecipe == null) {
                scanFornewRecipe(i);
                rets.add(false);
            } else {
                List<RecipePartWithProbability> reservedOutputs = getReservedOutputs();
                List<RecipePartWithProbability> reservedInputs = getReservedInputs();

                if (i.master.hasinputs(new ArrayList<>(reservedInputs), i.master.getFluidInTiles(), i.master.getItemInTiles()) && i.master.canFitOutputs(new ArrayList<>(reservedOutputs), i.master.getFluidOutTiles(), i.master.getItemOutTiles())) {
                    if (i.master.getTotalEnergyStored(i.master.getEnergyInputTiles()) >= i.currentRecipe.energyPerTick) {
                        ++i.progress;
                        i.master.consumeEnergy(i.currentRecipe.energyPerTick, i.master.getEnergyInputTiles());
                        if (i.progress == i.currentRecipe.ticksRequired) {
                            i.master.consumeInput(i.currentRecipe.inputs, false, i.master.getFluidInTiles(), i.master.getItemInTiles());
                            i.master.produceOutput(i.currentRecipe.outputs, i.master.getFluidOutTiles(), i.master.getItemOutTiles());
                            i.reset();
                        }
                        rets.add(true);
                    } else {
                        rets.add(false);
                    }
                } else {
                    i.reset();
                    rets.add(false);
                }
            }
        }
        return rets;
    }
}
