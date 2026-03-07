package advRocketry.Items;

import advRocketry.Registry.Blocks;
import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteEnergyProducer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class ItemSolarPanel extends BlockItem implements SatelliteEnergyProducer {
    public ItemSolarPanel() {
        super(Blocks.SOLAR_PANEL.get(), new Properties());
    }

    @Override
    public double produceEnergy(Satellite satellite) {
        return 1;
    }
}
