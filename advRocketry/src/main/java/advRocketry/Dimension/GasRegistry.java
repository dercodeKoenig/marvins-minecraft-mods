package advRocketry.Dimension;

import advRocketry.Registry.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashMap;

public class GasRegistry {
    public static final String oxygen = "oxygen";
    public static final String hydrogen = "hydrogen";
    public static final String nitrogen = "nitrogen";
    public static final String methane = "methane";
    public static final String co2 = "co2";

    public static HashMap<String, Gas> gases = new HashMap<>();

    static {
        gases.put(oxygen, new Gas(oxygen, 50, 55, 0));
        gases.put(hydrogen, new Gas(hydrogen, 14, 19, 0));
        gases.put(nitrogen, new Gas(nitrogen, 60, 65, 0));
        gases.put(methane, new Gas(methane, 85, 90, 25));
        gases.put(co2, new Gas(co2, 190, 195, 1));
    }

    public static double calculateGreenhouseBoost(HashMap<String, Double> atmosphericComposition) {
        double totalBoost = 0;
        for (String id : atmosphericComposition.keySet()) {
            Gas gas = gases.get(id);
            double concentration = atmosphericComposition.get(id);
            totalBoost += concentration * gas.greenhouseFactor;
        }
        return totalBoost;
    }

    public static class Gas {
        String id;
        int freezingTemp;
        int sublimationTemp;
        double greenhouseFactor;

        public Gas(String id, int freezingTemp, int sublimationTemp, double greenhouseFactor) {
            this.id = id;
            this.freezingTemp = freezingTemp;
            this.sublimationTemp = sublimationTemp;
            this.greenhouseFactor = greenhouseFactor;
        }
    }
}
