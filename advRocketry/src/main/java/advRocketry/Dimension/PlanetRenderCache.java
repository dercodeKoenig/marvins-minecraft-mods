package advRocketry.Dimension;

import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PlanetRenderCache {

    public ArrayList<PlanetDimension> planetsToRenderInSky = new ArrayList<>();

    public ArrayList<PlanetDimension> getPlanetsToRenderInSky() {
        return planetsToRenderInSky;
    }

    public static PlanetRenderCache INSTANCE = new PlanetRenderCache();

    // depth sorts planets for correct rendering using bubble sort
    // bubble sort is good because it can be distributed over many ticks and will approach target sort fast
    public void updatePlanetsToRenderInSky(Vec3 myDimensionPosition) {

        planetsToRenderInSky.removeIf((dimension) -> !DimensionManager.INSTANCE_CLIENT.dimensions.containsKey(dimension.getDimensionId()));

        HashSet<Dimension> planetsToRenderInSkySet = new HashSet<>(planetsToRenderInSky);

        for (Dimension i : DimensionManager.INSTANCE_CLIENT.dimensions.values()) {
            if(i instanceof PlanetDimension p) {
                if (!planetsToRenderInSkySet.contains(i)) {
                    planetsToRenderInSky.add(p);
                    System.out.println("planet render cache adding " + i.getDimensionId() + " to render list");
                }
            }
        }

        // to avoid recalculating the distance 2 times
        HashMap<PlanetDimension, Double> distanceToObserverMap = new HashMap<>();
        for (int i = 0; i < planetsToRenderInSky.size(); i++) {
            PlanetDimension dim = planetsToRenderInSky.get(i);
            double distance = dim.getPosition(0).distanceTo(myDimensionPosition);
            distanceToObserverMap.put(dim, distance);
        }

        for (int i = 0; i < planetsToRenderInSky.size() - 1; i++) {
            double firstDistance = distanceToObserverMap.get(planetsToRenderInSky.get(i));
            double secondDistance = distanceToObserverMap.get(planetsToRenderInSky.get(i + 1));
            if (firstDistance < secondDistance) {
                PlanetDimension temp = planetsToRenderInSky.get(i);
                planetsToRenderInSky.set(i, planetsToRenderInSky.get(i + 1));
                planetsToRenderInSky.set(i + 1, temp);
                // System.out.println("planet render cache switching " + planetsToRenderInSky.get(i) + " with " + planetsToRenderInSky.get(i + 1));
            }
        }
    }
}
