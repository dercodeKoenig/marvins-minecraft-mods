package advRocketry.Registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class GasRegistry {

    // TODO: methane and other fluids should be able to form source blocks,
    //  needs to be integrated in source create event
    // TODO: remove canGasMine, all planets should be mineable when atm > 2, fuel consumption based on G, maybe need heat shield upgrade blocks or rocket could die
    /*
    Oxygen: farmed in mass with atmosphere compressor / co2 -> oxygen from algae
    Methane: pump from underground deposits with laser drill or pump from methane oceans on other planets
    Hydrogen: get it in mass from gas mining
    co2: you probably dont need it in mass but maybe a volcanic planet can help.
    water: farm it from other planets or produce with fuel cell hydrogen + oxygen
    nitrogen: well thats a problem. maybe import from other planets? but is not a consumable so should be fine
     */

    public static final String oxygen = "oxygen";
    public static final String hydrogen = "hydrogen";
    public static final String nitrogen = "nitrogen";
    public static final String methane = "methane";
    public static final String co2 = "co2";
    public static final String water = "water";

    public static HashMap<String, Gas> gases = new LinkedHashMap<>();

    static {
        gases.put(hydrogen, new Gas(hydrogen, 14, 20, Fluids.HYDROGEN.get(), Blocks.HYDROGEN_BLOCK.get(),0));
        gases.put(oxygen, new Gas(oxygen, 54, 90, Fluids.OXYGEN.get(), Blocks.OXYGEN_BLOCK.get(),0));
        gases.put(nitrogen, new Gas(nitrogen, 63, 77, Fluids.NITROGEN.get(), Blocks.NITROGEN_BLOCK.get(),0));
        gases.put(methane, new Gas(methane, 91, 111, Fluids.METHANE.get(), Blocks.METHANE_BLOCK.get(),20000));
        gases.put(co2, new Gas(co2, 195, 195, Fluids.CO2.get(), Blocks.CO2_BLOCK.get(),1000));
        gases.put(water, new Gas(water, 273, 373, net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.block.Blocks.WATER, 1000));
    }

    public static double getRawGreenhouseValue(String gas, double in_atm) {
        Gas g = gases.get(gas);
        return in_atm * g.greenhouseFactor;
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
            double safeAtm = Math.max(0.0001, atmDensity);

            // Dynamic slope: Ensures every gas reaches its freeze point (triple point boundary)
            // at roughly ~0.006 to 0.01 atm of pressure.
            double slope = (boilingTemp - freezingTemp) / 2.2;

            return Math.max(boilingTemp + (Math.log10(safeAtm) * slope), getFreezeTemp(atmDensity));
        }

        public double getFreezeTemp(double atmDensity) {
            // In reality, freezing points barely care about pressure.
            // A simple static check is perfectly fine here.
            return freezingTemp;
        }
    }
}
