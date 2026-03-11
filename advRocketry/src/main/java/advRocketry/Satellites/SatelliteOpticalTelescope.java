package advRocketry.Satellites;

import advRocketry.Data.DataStack;
import advRocketry.Data.DataTypes;
import advRocketry.Registry.Items;
import com.mojang.datafixers.util.Pair;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SatelliteOpticalTelescope extends SatelliteDataCollectorBase {
    @Override
    public Pair<Boolean, String> validateBuild() {
        if (inventory.getStackInSlot(0).getItem() != Items.ITEM_SATELLITE_OPTICAL_TELESCOPE.get())
            return Pair.of(false, "missing optical sensor");
        return super.validateBuild();
    }

    public String getName(){
        return "Optical Telescope";
    }

    @Override
    double energyPerData() {
        return 100;
    }

    @Override
    String dataBaseTypeToGenerate() {
        return DataTypes.distance;
    }

    @Override
    int getMinDataGenTicks() {
        return 20;
    }
}
