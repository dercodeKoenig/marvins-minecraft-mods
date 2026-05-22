package advRocketry.Items;

import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteBiomeChanger;
import advRocketry.Satellites.SatelliteMassScanner;
import advRocketry.Satellites.SatellitePrimaryFunction;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemSatelliteBiomeChanger extends Item implements SatellitePrimaryFunction {
    public ItemSatelliteBiomeChanger() {
        super(new Properties());
    }

    @Override
    public Pair<Satellite, Pair<Boolean, String>> build(Satellite satellite, ItemStack satelliteIdChip) {
        if (satellite == null)
            return null;
        if (!((satelliteIdChip.getItem()) instanceof ItemBiomeChangerRemote))
            return Pair.of(null, Pair.of(false, "biome changer remote required"));

        SatelliteBiomeChanger biomeChanger = new SatelliteBiomeChanger();
        biomeChanger.inventory = satellite.inventory;
        Pair<Boolean, String> res = biomeChanger.validateBuild();
        if (!res.getFirst()) {
            return Pair.of(null, res);
        }
        return Pair.of(biomeChanger, res);
    }
}
