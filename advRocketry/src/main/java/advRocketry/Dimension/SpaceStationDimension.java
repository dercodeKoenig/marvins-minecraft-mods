package advRocketry.Dimension;

import advRocketry.utils.AxisDirections;
import advRocketry.worldgen.SpaceDimensionGeneration;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

public class SpaceStationDimension extends Dimension {
    public SpaceStationDimension(DimensionProperties properties, DimensionManager dimensionManager) {
        super(properties, dimensionManager);
    }

    private SpaceStationDimensionProperties properties() {
        return (SpaceStationDimensionProperties) properties;
    }


    @Override
    public void createDimension() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        System.out.println("creating space station: " + getDimensionId());
        DynamicDimensionRegistry dynamicDimensionRegistry = DynamicDimensionRegistry.from(server);

        ChunkGenerator generator = SpaceDimensionGeneration.makeChunkGenerator();
        DimensionType type = SpaceDimensionGeneration.makeDimensionType();
        ServerLevel l = dynamicDimensionRegistry.loadDynamicDimension(properties.dimensionId, generator, type);
        if (l == null) {
            dynamicDimensionRegistry.createDynamicDimension(
                    properties.dimensionId,
                    generator,
                    type
            );
        }
    }

    @Override
    public boolean canVisit() {
        return true;
    }

    @Override
    public boolean hasEnoughOxygen() {
        return false;
    }

    @Override
    public boolean canRain() {
        return false;
    }

    @Override
    public float getGravitationalMultiplier() {
        return 0.1f;
    }

    @Override
    public Vector3f getEmissiveColor() {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f getSkyColor() {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f getSunRiseColor() {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f getFogColor() {
        return new Vector3f(0, 0, 0);
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
        return true;
    }

    @Override
    public double getTerrainBrightness(float partialTick) {
        return 1;
    }

    @Override
    public Vector3f getCloudColor(float partialTick) {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f computeTerrainFogColor(float partialTick) {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vec3 getPosition(float partialTick) {
        return properties().position;
    }

    @Override
    public Vec3 getMovement(float partialTick) {
        return Vec3.ZERO;
    }

    @Override
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        return new AxisDirections(
                new Vec3(0,0,1),
                new Vec3(0,1,0)
        );
    }

    @Override
    public void tick() {
        super.tickStarCache();
    }
}
