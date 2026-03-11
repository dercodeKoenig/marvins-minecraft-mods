package advRocketry.Satellites;

import advRocketry.Data.DataTypes;
import advRocketry.Registry.Items;
import com.mojang.datafixers.util.Pair;

public class SatelliteCompositionScanner extends SatelliteDataCollectorBase {
    @Override
    public Pair<Boolean, String> validateBuild() {
        if (inventory.getStackInSlot(0).getItem() != Items.ITEM_SATELLITE_COMPOSITION_SCANNER.get())
            return Pair.of(false, "missing composition sensor");
        return super.validateBuild();
    }

    public String getName(){
        return "Composition Scanner";
    }

    @Override
    double energyPerData() {return 100;}

    @Override
    String dataBaseTypeToGenerate() {
        return DataTypes.composition;
    }

    @Override
    int getMinDataGenTicks() {
        return 20;
    }
}
