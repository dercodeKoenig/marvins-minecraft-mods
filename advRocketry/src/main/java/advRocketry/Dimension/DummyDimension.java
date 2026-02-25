package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.utils.AxisDirections;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DummyDimension extends Dimension {

    public DummyDimension(DummyDimensionProperties properties, DimensionManager dimensionManager) {
        super(properties, dimensionManager);
    }

    @Override
    public void createDimension() {

    }

    @Override
    public boolean canVisit() {
        return false;
    }

    @Override
    public boolean canRain() {
        return false;
    }

    @Override
    public float getGravitationalMultiplier() {
        return 0;
    }

    @Override
    public Vector3f getEmissiveColor() {
        return null;
    }

    @Override
    public Vector3f getSkyColor() {
        return null;
    }

    @Override
    public Vector3f getSunRiseColor() {
        return null;
    }

    @Override
    public Vector3f getFogColor() {
        return null;
    }

    @Override
    public float getAtmosphereDensity() {
        return 0;
    }

    @Override
    public float getRadiationIntensity() {
        return 0;
    }

    @Override
    public boolean hasCustomSky() {
        return false;
    }

    @Override
    public double getTerrainBrightness(float partialTick) {
        return 0;
    }

    @Override
    public Vector3f getCloudColor(float partialTick) {
        return null;
    }

    @Override
    public Vector3f computeTerrainFogColor(float partialTick) {
        return null;
    }

    @Override
    public Vec3 getPosition(float partialTick) {
        Vec3 pos =  ((DummyDimensionProperties)properties).position;
        return new Vec3(pos.x, pos.y, pos.z);
    }

    @Override
    public Vec3 getMovement(float partialTick) {
        return Vec3.ZERO;
    }

    @Override
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        return null;
    }

    @Override
    public void tick() {

    }
}