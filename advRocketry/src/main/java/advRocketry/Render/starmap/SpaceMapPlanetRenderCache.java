package advRocketry.Render.starmap;

import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.PlanetRenderCache;

import java.util.ArrayList;

public class SpaceMapPlanetRenderCache extends PlanetRenderCache {
    // always visible
    @Override
    protected boolean updateCullFlags() {
        for (Entry e : sorted) {
            if (!e.visible) {
                e.visible = true;
                return true;
            }
        }
        return false;
    }
}
