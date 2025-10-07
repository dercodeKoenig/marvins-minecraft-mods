package advRocketry.Dimension;

import advRocketry.utils.AxisDirections;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;

public interface IAdvRocketryDimension {

    boolean hasCustomSky(); // should the sky renderer render custom sky?

    AxisDirections getGlobalAxisDirections(float partialTick); // returns the dimensions up/north/east in universe space

    Vec3 getPosition(float partialTick); // returns the position in universe space

    ResourceLocation getDimensionId(); // the dimension id of the dimension

    Vector4f getEmissiveColor(); // the color emitted by the space object, rgb+intensity

    float getAtmosphereDensity(); // how dense / thick the atmosphere is

    Vector3f getSkyColor();

    Vector3f getFogColor();

    Vector3f getSunRiseColor();

    Iterable<ResourceLocation> getCurrentMainStars(); // cache for the main stars for render / light calculations

    Iterable<ResourceLocation> getPlanetsToRenderInSky(); // cache for the space objects to render in the sky

    boolean canVisit();

    /**
     * computes the accumulated brightness by relevant stars to be used for terrain shading
     */
    default double getAccumulatedWorldBrightness(float partialTick, float dotOffset, @Nullable Vec3 myPlanetPosition) {
//if(true)return 1;
        if (myPlanetPosition == null) myPlanetPosition = getPosition(partialTick);

        double astronomicalBrightness = 0;
        for (ResourceLocation targetId : getCurrentMainStars()) {
            IAdvRocketryDimension target = DimensionManager.get(targetId);
            Vec3 targetPosition = target.getPosition(partialTick);
            double distance = targetPosition.distanceTo(myPlanetPosition);
            double dotMultiplier = Math.max(0, (getSurfaceDotToTarget(target, partialTick, myPlanetPosition, targetPosition) + dotOffset) / (1 + dotOffset));
            double brightness = dotMultiplier * target.getEmissiveColor().w / (distance * distance);
            astronomicalBrightness += brightness;
        }
        return astronomicalBrightness;
    }



    /**
     * computes the dot product between the surface normal at the observer and the target space object
     * allows to input precomputed positions to avoid recomputation
     */
    default double getSurfaceDotToTarget(IAdvRocketryDimension target, float partialTick, @Nullable Vec3 myPlanetPosition, @Nullable Vec3 targetPosition) {
        Vec3 localUp = getGlobalAxisDirections(partialTick).up;

        if (targetPosition == null) targetPosition = target.getPosition(partialTick);
        if (myPlanetPosition == null) myPlanetPosition = getPosition(partialTick);

        Vec3 targetDirection = targetPosition.subtract(myPlanetPosition).normalize();
        double dot = localUp.dot(targetDirection);
        return dot;
    }


}