package advRocketry.Dimension;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class StarCache {

    public int MAX_LIGHT_SOURCES = 4;
    LinkedHashMap<ResourceLocation, Double> significantLightSourcesCache = new LinkedHashMap<>();
    private Iterator<Dimension> dimIterator1;

    // updates the cached light sources that are considered for lighting calculations
    // for simplicity, only self emitted light is considered. if a moon reflects a lot of light, this would be ignored.
    public void updateSignificantLightSourcesCache(Dimension myDimension) {

        if (dimIterator1 == null || !dimIterator1.hasNext()) {
            // Restart once we've gone through all dimensions
            dimIterator1 = new ArrayList<>(myDimension.dimensionManager.dimensions.values()).iterator();

            // in case a dimension was deleted, remove it from cache
            for (ResourceLocation dimId : significantLightSourcesCache.keySet()){
                if(!myDimension.dimensionManager.dimensions.containsKey(dimId)){
                    significantLightSourcesCache.remove(dimId);
                    break;
                }
            }
        }

        if (dimIterator1.hasNext()) {
            Dimension otherDimension = dimIterator1.next();
            ResourceLocation id = otherDimension.getDimensionId();

            // skip if it is my id
            if (id.equals(myDimension.getDimensionId())) {
                return;
            }

            // Skip if it's already in the top list
            if (significantLightSourcesCache.containsKey(id)) {
                return;
            }

            // skip if no color is emitted from other dimension
            double emissiveBrightness = otherDimension.getRadiationIntensity();
            if (emissiveBrightness <= 0) {
                return;
            }

            Vec3 myPos = myDimension.getPosition(0);
            Vec3 targetPosition = otherDimension.getPosition(0);
            double distance = myPos.distanceTo(targetPosition);
            double brightness = emissiveBrightness / (distance * distance);

            // If we still have room, just add it
            if (significantLightSourcesCache.size() < MAX_LIGHT_SOURCES) {
                significantLightSourcesCache.put(id, brightness);
            } else {
                // Find the dimmest currently stored and maybe replace it
                ResourceLocation weakestId = null;
                double weakestBrightness = Double.MAX_VALUE;

                for (Map.Entry<ResourceLocation, Double> entry : significantLightSourcesCache.entrySet()) {
                    if (entry.getValue() < weakestBrightness) {
                        weakestBrightness = entry.getValue();
                        weakestId = entry.getKey();
                    }
                }

                // Replace if the new one is brighter
                if (brightness > weakestBrightness && weakestId != null) {
                    significantLightSourcesCache.remove(weakestId);
                    significantLightSourcesCache.put(id, brightness);
                }
            }
        }
    }
}
