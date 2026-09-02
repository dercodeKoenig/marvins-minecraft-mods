package ARLib.utils;

import java.util.ArrayList;
import java.util.List;


public class MachineRecipe {
    public List<RecipePartWithProbability> inputs = new ArrayList<>();
    public int energyPerTick = 0;
    public List<RecipePartWithProbability> outputs = new ArrayList<>();
    public int outputEnergy = 0;
    public int ticksRequired = 1;

    public MachineRecipe copy(){
        MachineRecipe r = new MachineRecipe();
        for (RecipePartWithProbability p : inputs)
            r.inputs.add(new RecipePartWithProbability(p.id,p.amount,p.p));
        for (RecipePartWithProbability p : outputs)
            r.outputs.add(new RecipePartWithProbability(p.id,p.amount,p.p));
        r.ticksRequired = ticksRequired;
        r.energyPerTick = energyPerTick;
        r.outputEnergy = outputEnergy;
        return r;
    }

    public void computeRandomAmounts(){
        for (RecipePartWithProbability p : inputs)
            p.computeRandomAmount();
        for (RecipePartWithProbability p : outputs)
            p.computeRandomAmount();
    }
    public void addInput(String input_id_or_tag, int num, float p) {
        RecipePartWithProbability part = new RecipePartWithProbability(input_id_or_tag,num,p);
        inputs.add(part);
    }

    public void addOutput(String output_id, int num, float p) {
        RecipePartWithProbability part = new RecipePartWithProbability(output_id,num,p);
        outputs.add(part);
    }
}

