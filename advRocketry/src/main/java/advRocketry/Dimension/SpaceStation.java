package advRocketry.Dimension;

import advRocketry.utils.AxisDirections;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class SpaceStation implements IAdvRocketryDimension {
    @Override
    public boolean hasCustomSky() {
        return false;
    }

    @Override
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        return null;
    }

    @Override
    public Vec3 getPosition(float partialTick) {
        return null;
    }

    @Override
    public ResourceLocation getDimensionId() {
        return null;
    }

    @Override
    public Vector4f getEmissiveColor() {
        return null;
    }

    @Override
    public float getAtmosphereDensity() {
        return 0;
    }

    @Override
    public Vector3f getSkyColor() {
        return null;
    }

    @Override
    public Vector3f getFogColor() {
        return null;
    }

    @Override
    public Vector3f getSunRiseColor() {
        return null;
    }

    @Override
    public Iterable<ResourceLocation> getCurrentMainStars() {
        return null;
    }

    @Override
    public Iterable<ResourceLocation> getPlanetsToRenderInSky() {
        return null;
    }
// same as dimensionmanager but for space stations
}
