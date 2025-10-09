package advRocketry.experiments;

import advRocketry.Dimension.DimensionManager;
import advRocketry.worldgen.PlanetDimensionGeneration;
import advRocketry.worldgen.SpaceDimensionGeneration;
import advRocketry.Main;
import advRocketry.Render.PlanetRenderCache;
import advRocketry.utils.AxisDirections;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class SpaceDimension {
// TODO: every rocket needs to go to its own space dimension because the depth buffer clears after drawin planets
//      keep 1 or 2 clean dimensions ready for space travel and delete them when no rocket is in them. kill players in space without rocket. kill rockets without destination / autopilot and delete dimension
//        the rockets dimension should equal the rockets uuid to identify it and if a player looses conection the dimension will keep existing and if the player rejoins he can be added to the rocket instantly
//            if the player is ever found in a dimension other than where the rocket is, the space dim fir tge rockeet is deleted and the rocket destroyed
//              space dims should be saved to disk
//                 if a rocket has NO players, it needs no space dim and should get a virtual arrival counter
//                 rocket registry with method to add rocket simply with destination and target ticks if a rocket has no players
//                 or keep for every rocket a dimension, save & load it, if players are inside tp them to where the rocket is incase they leave server and come back late
//          would need a static map that maps uuid to dimension and blockpos of the rocket
//          or do not keep any dimension loaded when no rocket inside and use login event to tp players to rockets, in this case it is fine to put empty rocket in its own dimension
//      or do not keep empty rockets in world and if players rejoin, tp them to the rockets destination
//  TODO: probably best if 1 space travel dim exists and all rockets share the dim, but  beeing fixed at a position like 0 0 0 or 1000 0 0
//        and keep the rockets chunk force loaded


// TODO OR: render planets as entities and not as sky objects and avoid the entire shit - but would not allow bloom
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
            //planetRenderCache.updateSignificantLightSourcesCache();
        }
    }
}


/*
    public static float getSunAltitudeDegrees(DimensionProperties myPlanet, DimensionProperties lightSource, float partialTick) {
        double altitude = Math.asin(getSurfaceDotToPlanet(myPlanet, lightSource, partialTick, null, null));
        return (float) Math.toDegrees(altitude);
    }
 */

