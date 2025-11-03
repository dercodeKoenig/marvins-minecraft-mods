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
    public boolean canVisit() {return true;}

    @Override
    public boolean canRain() {return false;}

    @Override
    public boolean shouldRenderInSky() {return false;}


    @Override
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        return null;
    }

    @Override
    public Vec3 getPosition(float partialTick) {
        return null;
    }

    @Override
    public void createDimension() {

    }
}
