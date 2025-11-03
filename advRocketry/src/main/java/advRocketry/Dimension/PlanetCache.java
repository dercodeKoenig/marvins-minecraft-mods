package advRocketry.Dimension;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PlanetCache {

    LinkedHashMap<ResourceLocation, Double> significantLightSourcesCache = new LinkedHashMap<>();
    private Iterator<Dimension> dimIterator1;
    public int MAX_LIGHT_SOURCES = 4;

    static ArrayList<Dimension> planetsToRenderInSky = new ArrayList<>();

    public static ArrayList<Dimension> getPlanetsToRenderInSky() {
        return planetsToRenderInSky;
    }

    // depth sorts planets for correct rendering using bubble sort
    // bubble sort is good because it can be distributed over many ticks and will approach target sort fast
    public static void updatePlanetsToRenderInSky() {

        Level level = Minecraft.getInstance().level;
        if(level == null) return;
        Dimension myDimension =DimensionManager.get(level.dimension().location());
        if(myDimension == null) return;

        HashSet<Dimension> allDimensions = new HashSet<>(DimensionManager.INSTANCE.dimensions.values());

        planetsToRenderInSky.removeIf((dimension) -> !allDimensions.contains(dimension));

        HashSet<Dimension> planetsToRenderInSkySet = new HashSet<>(planetsToRenderInSky);

        for (Dimension i : allDimensions) {
            if (!planetsToRenderInSkySet.contains(i)) {
                planetsToRenderInSky.add(i);
                System.out.println("planet render cache adding " + i.getDimensionId() + " to render list");
            }
        }

        Vec3 myPos = myDimension.getPosition(0);

        // to avoid recalculating the distance 2 times
        HashMap<Dimension, Double> distanceToObserverMap = new HashMap<>();
        for (int i = 0; i < planetsToRenderInSky.size(); i++) {
            Dimension dim = planetsToRenderInSky.get(i);
            double distance = dim.getPosition(0).distanceTo(myPos);
            distanceToObserverMap.put(dim, distance);
        }

        for (int i = 0; i < planetsToRenderInSky.size() - 1; i++) {
            double firstDistance = distanceToObserverMap.get(planetsToRenderInSky.get(i));
            double secondDistance = distanceToObserverMap.get(planetsToRenderInSky.get(i + 1));
            if (firstDistance < secondDistance) {
                Dimension temp = planetsToRenderInSky.get(i);
                planetsToRenderInSky.set(i, planetsToRenderInSky.get(i + 1));
                planetsToRenderInSky.set(i + 1, temp);
                System.out.println("planet render cache switching " + planetsToRenderInSky.get(i) + " with " + planetsToRenderInSky.get(i + 1));
            }
        }
    }

    // updates the cached light sources that are considered for lighting calculations
    // for simplicity, only self emitted light is considered. if a moon reflects a lot of light, this would be ignored.
    public void updateSignificantLightSourcesCache(Dimension myDimension) {

        if (dimIterator1 == null || !dimIterator1.hasNext()) {
            // Restart once we've gone through all dimensions
            dimIterator1 = new ArrayList<>(DimensionManager.INSTANCE.dimensions.values()).iterator();
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
            double emissiveBrightness = otherDimension.getEmissiveColor().w;
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
