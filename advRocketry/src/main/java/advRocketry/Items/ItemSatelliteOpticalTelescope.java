package advRocketry.Items;

import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatellitePrimaryFunction;
import advRocketry.Satellites.SatelliteRegistry;
import advRocketry.utils.ItemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemSatelliteOpticalTelescope extends Item implements SatellitePrimaryFunction {
    public ItemSatelliteOpticalTelescope() {
        super(new Properties());
    }

    @Override
    public Satellite build(Satellite satellite) {
        return null;
    }
}
