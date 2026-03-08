package advRocketry.Items;

import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteMassScanner;
import advRocketry.Satellites.SatelliteOpticalTelescope;
import advRocketry.Satellites.SatellitePrimaryFunction;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemSatelliteMassScanner extends Item implements SatellitePrimaryFunction {
    public ItemSatelliteMassScanner() {
        super(new Properties());
    }

    @Override
    public Pair<Satellite, Pair<Boolean, String>> build(Satellite satellite, ItemStack satelliteIdChip) {
        if (satellite == null)
            return null;
        if (!((satelliteIdChip.getItem()) instanceof ItemSatelliteIdChip))
            return Pair.of(null, Pair.of(false, "wrong satellite id chip"));

        SatelliteMassScanner massScanner = new SatelliteMassScanner();
        massScanner.inventory = satellite.inventory;
        Pair<Boolean, String> res = massScanner.validateBuild();
        if (!res.getFirst()) {
            return Pair.of(null, res);
        }

        return Pair.of(massScanner, res);
    }
}
