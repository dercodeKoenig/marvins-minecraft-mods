package advRocketry.Items;

import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteOpticalTelescope;
import advRocketry.Satellites.SatellitePrimaryFunction;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemSatelliteOpticalTelescope extends Item implements SatellitePrimaryFunction {
    public ItemSatelliteOpticalTelescope() {
        super(new Properties());
    }

    @Override
    public Pair<Satellite, Pair<Boolean, String>> build(Satellite satellite, ItemStack satelliteIdChip) {
        if (satellite == null)
            return null;
        if (!((satelliteIdChip.getItem()) instanceof ItemSatelliteIdChip))
            return Pair.of(null, Pair.of(false, "wrong satellite id chip"));
        SatelliteOpticalTelescope telescope = new SatelliteOpticalTelescope();
        telescope.inventory = satellite.inventory;
        Pair<Boolean, String> res = telescope.validateBuild();
        if (!res.getFirst()) {
            return Pair.of(null, res);
        }

        return Pair.of(telescope, res);
    }
}
