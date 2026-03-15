package advRocketry.Registry;

import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class GasRegistry {

    // when a block is removed or added to the planet, the composition value should change by this much
    // i suggest: to change a value by 0.01 you have to mine 1000 blocks
    public static final float singleBlockWeight = 0.01f / 1000f;

    public static final String oxygen =     "oxygen";
    public static final String hydrogen =   "hydrogen";
    public static final String nitrogen =   "nitrogen";
    public static final String methane =    "methane";
    public static final String co2 =        "co2";

    public static HashMap<String, Gas> gases = new LinkedHashMap<>();

    static {
        gases.put(oxygen, new Gas(oxygen, 50, 55, Fluids.OXYGEN.get()));
        gases.put(hydrogen, new Gas(hydrogen, 14, 19, Fluids.HYDROGEN.get()));
        gases.put(nitrogen, new Gas(nitrogen, 60, 65, Fluids.NITROGEN.get()));
        gases.put(methane, new Gas(methane, 85, 90, Fluids.METHANE.get(), 20000));
        gases.put(co2, new Gas(co2, 190, 195, Fluids.CO2.get(), 1000));
    }

    public static double getInsulationBonus(String gas, double in_atm){
        return Math.log1p(in_atm * GasRegistry.gases.get(gas).greenhouseFactor);
    }

    public static class Gas {
        public final String id;
        public final int freezingTemp;
        public final int sublimationTemp;
        public final Fluid fluid;
        public final double greenhouseFactor;

        public Gas(String id, int freezingTemp, int sublimationTemp, Fluid fluid) {
            this(id, freezingTemp, sublimationTemp, fluid, 0);
        }

        public Gas(String id, int freezingTemp, int sublimationTemp, Fluid fluid, double greenhouseFactor) {
            this.id = id;
            this.freezingTemp = freezingTemp;
            this.sublimationTemp = sublimationTemp;
            this.fluid = fluid;
            this.greenhouseFactor = greenhouseFactor;
        }
    }
}
