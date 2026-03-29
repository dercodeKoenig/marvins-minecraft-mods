package advRocketry.Utils;

import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class SpaceNavigation {
    /**
     * Calculates the next target to get to the final target.
     * It tries to avoid collision with other planets in the way
     * If there are no planets in the way it will return finalTarget
     *
     * @param finalTarget      position in AU
     * @param myPos            position in AU
     * @param dimensionManager
     * @return
     */
    public static Vec3 getNextTargetAvoidPlanetCollision(Vec3 finalTarget, Vec3 myPos, DimensionManager dimensionManager, @Nullable PlanetDimension targetPlanet) {
        // we only calculate possible collisions to the closest planet
        double closestDistance = Double.MAX_VALUE;
        Vec3 closestPlanetPosition = null;
        PlanetDimension closestPlanet = null;
        for (Dimension i : dimensionManager.dimensions.values()) {
            if (i instanceof PlanetDimension p) {
                if (i == targetPlanet) continue; // ignore target
                Vec3 planetPos = i.getPosition(0);
                double d = planetPos.distanceTo(myPos);
                if (d < closestDistance) {
                    closestPlanetPosition = planetPos;
                    closestPlanet = p;
                    closestDistance = d;
                }
            }
        }
        if (closestPlanetPosition == null)
            return finalTarget; // no planet exists. this is unlikely, why would you have no planet or star in your system?
        if(closestDistance > myPos.distanceTo(finalTarget))
            return finalTarget; // the target is closer than the other closest planet so no way we can hit it

        Vec3 travelDir = finalTarget.subtract(myPos).scale(1000).normalize();
        Vec3 toPlanet = closestPlanetPosition.subtract(myPos).scale(1000).normalize();
        if (travelDir.dot(toPlanet) < 0)
            return finalTarget; // planet is behind us, ignore it and move forward until another planet gets closer


        // check if we come too close to the planet and if so, navigate around it
        Vec3 closestPointOnRay = closestPointOnRay(myPos, finalTarget, closestPlanetPosition);
        // check the distance of the closes point to the planet position to see if we need to go around
        double closestDistanceOnRay = closestPointOnRay.distanceTo(closestPlanetPosition);

        double requiredDistance =
                CelestialUtils.toAU(
                        Config.INSTANCE.planet_Render_Scale_Multiplier
                                * closestPlanet.getEarthRadiusMultiplier()
                                * CelestialUtils.EARTH_RADIUS
                                * 3
                );


        if (closestDistanceOnRay < requiredDistance) {
            // need to navigate around
            Vec3 target = null;
            if (closestDistanceOnRay == 0) // go above and hope we dont fly along y axis
                target = closestPlanetPosition.add(new Vec3(0, requiredDistance, 0));
            else {
                if(myPos.distanceTo(closestPlanetPosition) < requiredDistance * 2){
                    // too close, go around ( use 2x because i think 1x causes it to flicker the target around )
                    Vec3 ortho = toPlanet.cross(travelDir).normalize();
                    Vec3 avoidanceVector = ortho.cross(toPlanet).normalize().scale(requiredDistance);
                    target = myPos.add(avoidanceVector);
                }else {
                    // static target
                    target = closestPlanetPosition.add(closestPointOnRay.subtract(closestPlanetPosition).scale(1000).normalize().scale(requiredDistance*1.5));
                }
            }
            return target;
        }

        return finalTarget;
    }


    /**
     * Computes the closest point on the ray starting at A, going through B, to point C.
     *
     * @param A Ray start
     * @param B Another point along the ray (defines direction)
     * @param C External point
     * @return Closest point on the ray to C
     */
    public static Vec3 closestPointOnRay(Vec3 A, Vec3 B, Vec3 C) {
        Vec3 v = B.subtract(A);        // direction
        Vec3 w = C.subtract(A);        // from A to C

        double vv = v.dot(v);
        if (vv == 0.0f) {
            // Degenerate ray: A and B are the same point
            return A;
        }

        // Projection factor t = (w·v) / (v·v)
        double t = w.dot(v) / vv;

        // Clamp to ray: t >= 0
        if (t < 0.0f) t = 0.0f;

        // Compute closest point
        return A.add(v.scale(t));
    }
}
