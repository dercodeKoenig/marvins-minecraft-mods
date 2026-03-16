package advRocketry.Registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class GasRegistry {

    // when a block is removed or added to the planet, the composition value should change by this much
    // i suggest: to change a value by 0.01 you have to mine 1000 blocks
    public static final float singleBlockWeight = 0.01f / 1000f;

    public static final String oxygen = "oxygen";
    public static final String hydrogen = "hydrogen";
    public static final String nitrogen = "nitrogen";
    public static final String methane = "methane";
    public static final String co2 = "co2";
    public static final String water = "water";

    public static HashMap<String, Gas> gases = new LinkedHashMap<>();

    static {
        gases.put(oxygen, new Gas(oxygen, 54, 90, Fluids.OXYGEN.get(), Blocks.OXYGEN_BLOCK.get(),0));
        gases.put(hydrogen, new Gas(hydrogen, 14, 20, Fluids.HYDROGEN.get(), Blocks.HYDROGEN_BLOCK.get(),0));
        gases.put(nitrogen, new Gas(nitrogen, 63, 77, Fluids.NITROGEN.get(), Blocks.NITROGEN_BLOCK.get(),0));
        gases.put(methane, new Gas(methane, 91, 111, Fluids.METHANE.get(), Blocks.METHANE_BLOCK.get(),20000));
        gases.put(co2, new Gas(co2, 195, 195, Fluids.CO2.get(), Blocks.CO2_BLOCK.get(),1000));
        gases.put(water, new Gas(water, 273, 373, net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.block.Blocks.WATER, 5000));
    }

    public static double getInsulationBonus(String gas, double in_atm) {
        return Math.log1p(in_atm * GasRegistry.gases.get(gas).greenhouseFactor);
    }

    public static class Gas {
        public final String id;
        public final Fluid fluid;
        public final Block fluidBlock;
        public final double greenhouseFactor;
        final int freezingTemp;
        final int boilingTemp;

        public Gas(String id, int freezingTemp, int boilingTemp, Fluid fluid, Block fluidBlock, double greenhouseFactor) {
            this.id = id;
            this.freezingTemp = freezingTemp;
            this.boilingTemp = boilingTemp;
            this.fluid = fluid;
            this.fluidBlock = fluidBlock;
            this.greenhouseFactor = greenhouseFactor;
        }


        public double getBoilingTemp(double atmDensity) {

            // Clamp density to prevent Math.log10(0) returning -Infinity.
            // 0.001 atm is basically a vacuum in this context.
            double safeAtm = Math.max(0.0001, atmDensity);

            // At 1 atm, log10(1) = 0. Boiling temp stays normal.
            // At 10 atm, log10(10) = 1. Boiling temp increases by 25 degrees.
            // At 0.1 atm, log10(0.1) = -1. Boiling temp drops by 25 degrees.
            return boilingTemp + (Math.log10(safeAtm) * 25.0);

        }

        public double getFreezeTemp(double atmDensity) {
            // In reality, freezing points barely care about pressure.
            // A simple static check is perfectly fine here.
            return freezingTemp;
        }
    }
}
