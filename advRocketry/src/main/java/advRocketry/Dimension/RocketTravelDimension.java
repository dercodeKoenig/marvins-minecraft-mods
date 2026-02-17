package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.Rocket.EntityRocket;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.ClientUtils;
import advRocketry.worldgen.SpaceDimensionGeneration;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.util.HashMap;

public class RocketTravelDimension extends Dimension {

    public static ResourceLocation dimId = ResourceLocation.fromNamespaceAndPath(Main.MODID, "space_travel");

    public RocketTravelDimension(DimensionProperties properties, DimensionManager dimensionManager) {
        super(properties, dimensionManager);
        this.properties.name = "space travel dimension";
        this.properties.dimensionId = dimId;
    }

    public static ChunkPos getNextFreeChunkPos() {
        HashMap<ChunkPos, Long> rocketTravelForcedChunks = ForcedChunkManager.INSTANCE.forcedChunks.getOrDefault(dimId, new HashMap<>());
        int x = 0;
        while (true) {
            x += 50;
            ChunkPos p = new ChunkPos(x, 0);
            if (!rocketTravelForcedChunks.containsKey(p)) {
                return p;
            }
        }
    }

    public void createDimension() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        System.out.println("creating space travel dimension...");
        DynamicDimensionRegistry dynamicDimensionRegistry = DynamicDimensionRegistry.from(server);

        ChunkGenerator generator = SpaceDimensionGeneration.makeChunkGenerator();
        DimensionType type = SpaceDimensionGeneration.makeDimensionType();
        ServerLevel l = dynamicDimensionRegistry.loadDynamicDimension(dimId, generator, type);
        if (l == null) {
            dynamicDimensionRegistry.createDynamicDimension(
                    dimId,
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
    public boolean canRain() {
        return false;
    }

    @Override
    public ResourceLocation getDimensionId() {
        return dimId;
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
        return 0; // we use ambient light in space
    }

    @Override
    public Vector3f getCloudColor(float partialTick) {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f computeTerrainFogColor(float partialTick) {
        return getFogColor();
    }

    @Override
    public float getGravitationalMultiplier() {
        return 0f;
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
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        Player player = ClientUtils.getSinglePlayer();
        if (player != null) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof EntityRocket rocket) {
                return new AxisDirections(
                        rocket.universeHeading,
                        rocket.universeFront
                );
            }
        }
        return new AxisDirections(
                new Vec3(0, 0, -1),
                new Vec3(0, 1, 0)
        );
    }

    @Override
    public Vec3 getPosition(float partialTick) {
        Player player = ClientUtils.getSinglePlayer();
        if (player != null) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof EntityRocket rocket) {
                return rocket.universePosition;
            } else {
                return player.position().scale(0.001); // for debug flying around in creative
            }
        }
        return new Vec3(0, 0, 0);
    }

    @Override
    public void tick() {

    }
}
