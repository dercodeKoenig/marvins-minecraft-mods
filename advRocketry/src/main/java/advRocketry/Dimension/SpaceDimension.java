package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.Render.PlanetRenderCache;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.CelestialUtils;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Random;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static advRocketry.utils.CelestialUtils.fromAU;
import static advRocketry.utils.CelestialUtils.fromEarthMasses;

public class SpaceDimension implements IAdvRocketryDimension {

    public static ResourceLocation spaceDimId =  ResourceLocation.fromNamespaceAndPath(Main.MODID, "space");
    public static Vec3 to_AU(Vec3 worldPos){
        return worldPos.scale(0.0001).add(new Vec3(0,-100,0));
    }

    public PlanetRenderCache planetRenderCache;
    public ClientOnly clientOnly;


    public SpaceDimension() {
        if (FMLEnvironment.dist.isClient()) {
            clientOnly = new ClientOnly();
            planetRenderCache = new PlanetRenderCache();
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(server != null) {
            System.out.println("creating dimension for space");
            DynamicDimensionRegistry dynamicDimensionRegistry = DynamicDimensionRegistry.from(server);
            dynamicDimensionRegistry.createDynamicDimension(
                    spaceDimId,
                    SpaceDimensionGeneration.makeChunkGenerator(),
                    PlanetDimensionGeneration.makePlanetDimensionType());
        }
    }

    public boolean canVisit() {
        return false;
    }

    public ResourceLocation getDimensionId() {
        return spaceDimId;
    }

    public Vector4f getEmissiveColor() {
        return new Vector4f(0,0,0,0);
    }

    public Vector3f getSkyColor() {
        return new Vector3f(0,0,0);
    }

    public Vector3f getSunRiseColor() {
        return new Vector3f(0,0,0);
    }

    public Iterable<ResourceLocation> getCurrentMainStars() {
        return planetRenderCache.significantLightSourcesCache.keySet();
    }

    public Iterable<ResourceLocation> getPlanetsToRenderInSky() {
        return DimensionManager.INSTANCE.dimensions.keySet(); // TODO: use cache similar like the light source cache
    }

    public Vector3f getFogColor() {
        return new Vector3f(0,0,0);
    }

    public float getAtmosphereDensity() {
        return 0;
    }

    public boolean hasCustomSky() {
        return true;
    }

    public Vec3 getPosition(float partialTick) {
        if(FMLEnvironment.dist.isClient()){
            return new ClientOnly().getPosition();
        }
        return new Vec3(0,0,0);
    }

    public AxisDirections getGlobalAxisDirections(float partialTick) {
        return new AxisDirections(new Vec3(0,0,-1), new Vec3(1,0,0), new Vec3(0,1,0));
    }

    public void serverTick(ServerTickEvent event) {

    }

    public class ClientOnly {
        public Vec3 getPosition() {
            Player p = Minecraft.getInstance().player;
            Vec3 pos = p.position();
            return to_AU(pos);
        }

        public void clientTick() {
            planetRenderCache.updateSignificantLightSourcesCache(SpaceDimension.this);
        }
    }
}


/*
    public static float getSunAltitudeDegrees(DimensionProperties myPlanet, DimensionProperties lightSource, float partialTick) {
        double altitude = Math.asin(getSurfaceDotToPlanet(myPlanet, lightSource, partialTick, null, null));
        return (float) Math.toDegrees(altitude);
    }
 */

