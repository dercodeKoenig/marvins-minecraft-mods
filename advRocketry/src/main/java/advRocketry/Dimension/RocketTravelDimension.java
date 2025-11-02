package advRocketry.Dimension;

import advRocketry.Rocket.EntityRocket;
import advRocketry.utils.AxisDirections;
import advRocketry.worldgen.SpaceDimensionGeneration;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;

public class RocketTravelDimension extends Dimension {

    public ClientOnly clientOnly;

    public RocketTravelDimension(DimensionProperties properties) {
        super(properties);
        if (FMLEnvironment.dist.isClient()) {
            clientOnly = new ClientOnly();
        }
    }

    @Override
    public ResourceLocation getDimensionId(){
        return SpaceTravelManager.dimId;
    }

    @Override
    public float getAtmosphereDensity() {
        return 0;
    }

    public DimensionProperties.PlanetType getType() {
        return DimensionProperties.PlanetType.DUMMY;
    }

    @Override
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        return new AxisDirections(
                new Vec3(0, 0, -1),
                new Vec3(1, 0, 0),
                new Vec3(0, 1, 0)
        );
    }

    @Override
    public Vec3 getPosition(float partialTick) {
        if(FMLEnvironment.dist.isClient())
            return clientOnly.getPosition();
        else return null;
    }

    @Override
    public double getAccumulatedWorldBrightness(float partialTick, float dotOffset, @Nullable Vec3 myPlanetPosition) {
        if (myPlanetPosition == null) myPlanetPosition = getPosition(partialTick);
        double astronomicalBrightness = 0;
        for (ResourceLocation targetId : getCurrentMainStars()) {
            Dimension target = DimensionManager.get(targetId);
            Vec3 targetPosition = target.getPosition(partialTick);
            double distance = targetPosition.distanceTo(myPlanetPosition);
            double dotMultiplier = 1;
            double brightness = dotMultiplier * target.getEmissiveColor().w / (distance * distance);
            astronomicalBrightness += brightness;
        }
        return astronomicalBrightness;
    }

    public class ClientOnly {
        public Vec3 getPosition() {
            Player player = Minecraft.getInstance().player;
            if(player != null) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof EntityRocket rocket) {
                    return rocket.universePosition;
                }
            }
            return  new Vec3(0,0,0);
        }
        public void clientTick(){
            planetRenderCache.updateSignificantLightSourcesCache(RocketTravelDimension.this);
        }
    }
}
