package advRocketry.Render.starmap;

import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.PlanetRenderCache;

import java.util.ArrayList;

public class SpaceMapPlanetRenderCache extends PlanetRenderCache {
    public static SpaceMapPlanetRenderCache INSTANCE = new SpaceMapPlanetRenderCache();


    public ArrayList<PlanetDimension> getPlanetsToRenderInSky() {
        return allSortedPlanets;
    }
}
