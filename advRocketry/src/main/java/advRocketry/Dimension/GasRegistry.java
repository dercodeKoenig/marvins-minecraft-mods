package advRocketry.Dimension;

import advRocketry.Registry.Fluids;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashMap;

public class GasRegistry {

    // when a block is removed or added to the planet, the composition value should change by this much
    public static final float singleBlockWeight = 0.001f;

    public static final String oxygen = "oxygen";
    public static final String hydrogen = "hydrogen";
    public static final String nitrogen = "nitrogen";
    public static final String methane = "methane";
    public static final String co2 = "co2";

    public static HashMap<String, Gas> gases = new HashMap<>();

    static {
        gases.put(oxygen, new Gas(oxygen, 50, 55, Fluids.OXYGEN.get()));
        gases.put(hydrogen, new Gas(hydrogen, 14, 19, Fluids.HYDROGEN.get()));
        gases.put(nitrogen, new Gas(nitrogen, 60, 65, Fluids.NITROGEN.get()));
        gases.put(methane, new Gas(methane, 85, 90, Fluids.METHANE.get()));
        gases.put(co2, new Gas(co2, 190, 195, Fluids.CO2.get()));
    }

    public static class Gas {
        public final String id;
        public final int freezingTemp;
        public final int sublimationTemp;
        public final Fluid fluid;

        public Gas(String id, int freezingTemp, int sublimationTemp, Fluid fluid) {
            this.id = id;
            this.freezingTemp = freezingTemp;
            this.sublimationTemp = sublimationTemp;
            this.fluid = fluid;
        }
    }
}
