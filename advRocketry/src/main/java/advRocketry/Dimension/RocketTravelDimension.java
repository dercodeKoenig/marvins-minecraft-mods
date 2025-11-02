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

    public AxisDirections getDefaultAxisDirections() {
        return new AxisDirections(
                new Vec3(0, 0, -1),
                new Vec3(1, 0, 0),
                new Vec3(0, 1, 0)
        );
    }

    @Override
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        if(FMLEnvironment.dist.isClient()) return clientOnly.getGlobalAxisDirections();
        return getDefaultAxisDirections();
    }

    @Override
    public Vec3 getPosition(float partialTick) {
        if(FMLEnvironment.dist.isClient())
            return clientOnly.getPosition();
        else return null;
    }

    @Override
    public double getSurfaceDotToTarget(Dimension target, float partialTick, @Nullable Vec3 myPlanetPosition, @Nullable Vec3 targetPosition) {
        return 1;
    }

    public class ClientOnly {
        public Vec3 getPosition() {
            Player player = Minecraft.getInstance().player;
            if(player != null) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof EntityRocket rocket) {
                    return rocket.universePosition;
                }else{
                    return player.position().scale(0.01); // for debug flying around in creative
                }
            }
            return  new Vec3(0,0,0);
        }
        public AxisDirections getGlobalAxisDirections(){
            Player player = Minecraft.getInstance().player;
            if(player != null) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof EntityRocket rocket) {
                    return new AxisDirections(
                            rocket.universeHeading,
                            rocket.universeHeading.cross(rocket.universeFront),
                            rocket.universeFront
                    );
                }
            }
            return getDefaultAxisDirections();
        }
        public void clientTick(){
            planetRenderCache.updateSignificantLightSourcesCache(RocketTravelDimension.this);
        }
    }
}
