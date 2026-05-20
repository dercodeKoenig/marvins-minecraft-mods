package advRocketry.Dimension;

import advRocketry.Config;
import advRocketry.Utils.CelestialUtils;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class PlanetRenderCache {
    /// Depth sorts planets / stars for correct rendering without depth buffer problems
    /// Culls away distant planets so we dont waste draw calls on planets we can not see

    public static final PlanetRenderCache INSTANCE = new PlanetRenderCache();

    // The list we can sort by index
    protected final ArrayList<PlanetDimension> allSortedPlanets = new ArrayList<>();

    // A persistent set for O(1) contains checks (should stay in sync with the sorted list)
    protected final HashSet<PlanetDimension> knownPlanetsSet = new HashSet<>();

    // The final list handed to the renderer
    protected final ArrayList<PlanetDimension> visiblePlanets = new ArrayList<>();

    public ArrayList<PlanetDimension> getPlanetsToRenderInSky() {
        return visiblePlanets;
    }

    public void clearCache() {
        allSortedPlanets.clear();
        knownPlanetsSet.clear();
        visiblePlanets.clear();
    }

    public void updatePlanetsToRenderInSky(Vec3 myDimensionPosition) {

        // 1. Clean up removed dimensions using an iterator to keep both collections synced
        Iterator<PlanetDimension> it = allSortedPlanets.iterator();
        while (it.hasNext()) {
            PlanetDimension dim = it.next();
            if (!DimensionManager.INSTANCE_CLIENT.dimensions.containsKey(dim.getDimensionId())) {
                it.remove();
                knownPlanetsSet.remove(dim);
                System.out.println("Planet Render Cache remove dim: "+dim.getDimensionId());
            }
        }

        // 2. Add new dimensions (HashSet.add returns true only if the item wasn't already there)
        for (Dimension i : DimensionManager.INSTANCE_CLIENT.dimensions.values()) {
            if (i instanceof PlanetDimension p) {
                if (knownPlanetsSet.add(p)) {
                    allSortedPlanets.add(p);
                    System.out.println("Planet Render Cache add dim: "+p.getDimensionId());
                }
            }
        }

        // 3. One pass of bubble sort using Squared Distance
        for (int i = 0; i < allSortedPlanets.size() - 1; i++) {
            PlanetDimension p1 = allSortedPlanets.get(i);
            PlanetDimension p2 = allSortedPlanets.get(i + 1);

            double dist1 = p1.getPosition(0).distanceToSqr(myDimensionPosition);
            double dist2 = p2.getPosition(0).distanceToSqr(myDimensionPosition);

            if (dist1 < dist2) {
                allSortedPlanets.set(i, p2);
                allSortedPlanets.set(i + 1, p1);
            }
        }

        // 4. Build the visible list
        visiblePlanets.clear();
        double minApparentSize = 0.001;
        double cullThreshold = minApparentSize / 20.0;

        for (PlanetDimension dim : allSortedPlanets) {
            double dist = dim.getPosition(0).distanceTo(myDimensionPosition) * CelestialUtils.ASTRONOMICAL_UNIT;
            if(dist < 0.000001){
                visiblePlanets.add(dim);
                continue;
            }

            // the scale used in SkyRenderer
            double geometryScale = CelestialUtils.fromEarthRadius(dim.getEarthRadiusMultiplier()) * Config.INSTANCE.planet_Render_Scale_Multiplier;

            double apparentSizeRatio = geometryScale / dist;

            // Only add to the render list if it is close enough to be seen
            if (apparentSizeRatio >= cullThreshold) {
                visiblePlanets.add(dim);
            }
        }
    }
}