package advRocketry.Dimension;

import advRocketry.utils.AxisDirections;
import advRocketry.utils.CelestialUtils;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class SpaceStation extends Dimension {
    public SpaceStation(DimensionProperties properties) {
        super(properties);
    }

    @Override
    public boolean hasCustomSky() {
        return true;
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
    public Vec3 getRotationAxis() {
        return new Vec3(0, 0, 0);
    }

    @Override
    public Vector4f getEmissiveColor() {
        return new Vector4f(0, 0, 0, 0);
    }

    @Override
    public float getAtmosphereDensity() {
        return 0;
    }

    @Override
    public Vector3f getSkyColor() {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f getFogColor() {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f getSunRiseColor() {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public boolean canVisit() {
        return true;
    }

    @Override
    public boolean canRain() {
        return false;
    }

    @Override
    public double getEarthRadiusMultiplier() {
        return 0;
    }

    @Override
    public double getEarthMassMultiplier() {
        return 0;
    }

    @Override
    public ResourceLocation getTexture() {
        return null;
    }

    @Override
    public boolean shouldRenderInSky() {
        return false;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public float getDayTimePerTick() {
        return 0;
    }

    @Override
    public double getRotationAngle(float partialTick) {
        return 0;
    }

    @Override
    public void createDimension() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        System.out.println("creating dimension for " + getDimensionId());
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
            System.out.println("created dimension for " + properties.dimensionId);
        } else {
            System.out.println("loaded dimension for " + properties.dimensionId);
        }
    }
}
