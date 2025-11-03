package advRocketry.Render;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PlanetRenderCache {

    static ArrayList<PlanetDimension> planetsToRenderInSky = new ArrayList<>();

    public static ArrayList<PlanetDimension> getPlanetsToRenderInSky() {
        return planetsToRenderInSky;
    }

    // depth sorts planets for correct rendering using bubble sort
    // bubble sort is good because it can be distributed over many ticks and will approach target sort fast
    public static void updatePlanetsToRenderInSky() {

        Level level = Minecraft.getInstance().level;
        if(level == null) return;
        Dimension myDimension = DimensionManager.get(level.dimension().location());
        if(myDimension == null) return;

        HashSet<Dimension> allDimensions = new HashSet<>(DimensionManager.INSTANCE.dimensions.values());
        planetsToRenderInSky.removeIf((dimension) -> !allDimensions.contains(dimension));

        HashSet<Dimension> planetsToRenderInSkySet = new HashSet<>(planetsToRenderInSky);

        for (Dimension i : allDimensions) {
            if(i instanceof PlanetDimension p) {
                if (!planetsToRenderInSkySet.contains(i)) {
                    planetsToRenderInSky.add(p);
                    System.out.println("planet render cache adding " + i.getDimensionId() + " to render list");
                }
            }
        }

        Vec3 myPos = myDimension.getPosition(0);

        // to avoid recalculating the distance 2 times
        HashMap<PlanetDimension, Double> distanceToObserverMap = new HashMap<>();
        for (int i = 0; i < planetsToRenderInSky.size(); i++) {
            PlanetDimension dim = planetsToRenderInSky.get(i);
            double distance = dim.getPosition(0).distanceTo(myPos);
            distanceToObserverMap.put(dim, distance);
        }

        for (int i = 0; i < planetsToRenderInSky.size() - 1; i++) {
            double firstDistance = distanceToObserverMap.get(planetsToRenderInSky.get(i));
            double secondDistance = distanceToObserverMap.get(planetsToRenderInSky.get(i + 1));
            if (firstDistance < secondDistance) {
                PlanetDimension temp = planetsToRenderInSky.get(i);
                planetsToRenderInSky.set(i, planetsToRenderInSky.get(i + 1));
                planetsToRenderInSky.set(i + 1, temp);
                System.out.println("planet render cache switching " + planetsToRenderInSky.get(i) + " with " + planetsToRenderInSky.get(i + 1));
            }
        }
    }
}
