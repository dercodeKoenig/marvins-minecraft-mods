package advRocketry.Items;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Registry.Blocks;
import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteEnergyProducer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class ItemSolarPanel extends BlockItem implements SatelliteEnergyProducer {
    public ItemSolarPanel() {
        super(Blocks.SOLAR_PANEL.get(), new Properties());
    }

    @Override
    public double produceEnergy(Satellite satellite) {
        if(DimensionManager.INSTANCE_SERVER.get(satellite.parentDimensionId) instanceof PlanetDimension planet){
            double maxStarlight = 0;
            Vec3 planetPos = planet.getPosition(0);
            for(ResourceLocation starId : planet.getCurrentMainStars()){
                if(DimensionManager.INSTANCE_SERVER.get(starId) instanceof PlanetDimension star){
                    double distance = star.getPosition(0).distanceTo(planetPos);
                    double starlight = star.getRadiationIntensity() / (distance * distance);
                    maxStarlight = Math.max(starlight, maxStarlight);
                }
            }
            double multiplier = 3.5;
            return Math.min(10, maxStarlight * multiplier);
        }
        return 1;
    }
}
