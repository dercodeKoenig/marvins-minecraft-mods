package advRocketry.Satellites;

import advRocketry.Data.DataTypes;
import advRocketry.Registry.Items;
import com.mojang.datafixers.util.Pair;

public class SatelliteMassScanner extends SatelliteDataCollectorBase {
    @Override
    public Pair<Boolean, String> validateBuild() {
        if (inventory.getStackInSlot(0).getItem() != Items.ITEM_SATELLITE_MASS_SCANNER.get())
            return Pair.of(false, "missing mass sensor");
        return super.validateBuild();
    }

    public String getName(){
        return "Mass Scanner";
    }

    @Override
    double energyPerData() {
        return 100;
    }

    @Override
    String dataBaseTypeToGenerate() {
        return DataTypes.mass;
    }

    @Override
    int getMinDataGenTicks() {
        return 20;
    }
}
