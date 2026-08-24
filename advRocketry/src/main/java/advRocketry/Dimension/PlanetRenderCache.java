package advRocketry.Dimension;

import advRocketry.Config;
import advRocketry.Utils.CelestialUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class PlanetRenderCache {

    public static final PlanetRenderCache INSTANCE = new PlanetRenderCache();

    protected static final double AU_SQ = CelestialUtils.ASTRONOMICAL_UNIT * CelestialUtils.ASTRONOMICAL_UNIT;
    protected static final double MIN_APPARENT_SIZE = 0.001;
    protected static final double CULL_SQ = Math.pow(MIN_APPARENT_SIZE / 5.0, 2);
    protected static final double SHOW_SQ = Math.pow(MIN_APPARENT_SIZE / 4.0, 2); // slightly stricter than CULL_SQ -> hysteresis
    protected final HashMap<ResourceLocation, Entry> known = new HashMap<>();
    protected final ArrayList<Entry> sorted = new ArrayList<>();
    protected final ArrayList<PlanetDimension> visiblePlanets = new ArrayList<>();

    public ArrayList<PlanetDimension> getPlanetsToRenderInSky() {
        return visiblePlanets;
    }

    public void updatePlanetsToRenderInSky(Vec3 myDimensionPosition) {
        boolean structureChanged = syncMembership();

        for (Entry e : sorted) {
            e.distSq = e.planet.getPosition(0).distanceToSqr(myDimensionPosition) * AU_SQ;
        }

        boolean orderChanged = onePass();
        boolean visibilityChanged = updateCullFlags();

        if (structureChanged || orderChanged || visibilityChanged) {
            visiblePlanets.clear();
            for (Entry e : sorted) if (e.visible) visiblePlanets.add(e.planet);
        }
    }

    protected boolean syncMembership() {
        boolean changed = false;
        Iterator<Entry> it = sorted.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            ResourceLocation id = e.planet.getDimensionId();
            Dimension current = DimensionManager.INSTANCE_CLIENT.dimensions.get(id);
            if (current != e.planet) { // gone, or replaced under the same id
                it.remove();
                known.remove(id);
                changed = true;
                System.out.println("Planet Render Cache remove dim: " + id);

            }
        }
        for (Dimension d : DimensionManager.INSTANCE_CLIENT.dimensions.values()) {
            if (d instanceof PlanetDimension p) {
                ResourceLocation id = p.getDimensionId();
                if (!known.containsKey(id)) {
                    Entry e = new Entry(p);
                    known.put(id, e);
                    sorted.add(e);
                    changed = true;
                    System.out.println("Planet Render Cache add dim: " + id);
                }
            }
        }
        return changed;
    }

    protected boolean onePass() {
        boolean swapped = false;
        int n = sorted.size();
        for (int i = 0; i < n - 1; i++)
            if (sorted.get(i).distSq < sorted.get(i + 1).distSq) {
                Collections.swap(sorted, i, i + 1);
                swapped = true;
            }
        return swapped;
    }

    protected boolean updateCullFlags() {
        boolean changed = false;
        for (Entry e : sorted) {
            boolean shouldShow;
            if (e.distSq < 1e-12) {
                shouldShow = true;
            } else {
                double scale = CelestialUtils.fromEarthRadius(e.planet.getEarthRadiusMultiplier())
                        * Config.INSTANCE.planet_Render_Scale_Multiplier;
                double ratioSq = (scale * scale) / e.distSq;
                shouldShow = e.visible ? ratioSq >= CULL_SQ : ratioSq >= SHOW_SQ; // hysteresis
            }
            if (shouldShow != e.visible) {
                e.visible = shouldShow;
                changed = true;
            }
        }
        return changed;
    }

    public static final class Entry {
        public final PlanetDimension planet;
        public double distSq;
        public boolean visible;

        Entry(PlanetDimension p) {
            this.planet = p;
        }
    }
}