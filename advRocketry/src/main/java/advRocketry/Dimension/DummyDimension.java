package advRocketry.Dimension;

import advRocketry.Utils.AxisDirections;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;

public class DummyDimension extends Dimension {

    public DummyDimension(DimensionProperties properties, DimensionManager dimensionManager) {
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
    public Set<SurvivalProblem> getSurvivalProblems() {
        return SurvivalProblem.spaceProblems;
    }

    @Override
    public boolean hasEnoughOxygenToBurn(){return false;}

    @Override
    public float computeCloudValue() {
        return 0;
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
    public double computeTerrainBrightness(float partialTick) {
        return 0;
    }

    @Override
    public Vector3f computeTerrainCloudColor(float partialTick) {
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
    public Vec3 getMovement() {
        return Vec3.ZERO;
    }

    @Override
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        return null;
    }

    @Override
    public void tick() {

    }

    @Override
    public double getCurrentTemp() {
        return 0;
    }
}