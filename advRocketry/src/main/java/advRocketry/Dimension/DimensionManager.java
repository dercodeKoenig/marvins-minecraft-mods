package advRocketry.Dimension;

import ARLib.network.SimpleNetworkPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;

public class DimensionManager{
    public static DimensionManager INSTANCE = new DimensionManager();
    public static Dimension get(ResourceLocation key){
        return INSTANCE.dimensions.get(key);
    }
    public static long getGlobalTime() {
        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            return INSTANCE.universalTimeServer;
        }else{
            return INSTANCE.universalTimeClient;
        }
    }

    public HashMap<ResourceLocation, Dimension> dimensions = new HashMap<>();
    public long universalTimeServer = 0;
    public long universalTimeClient = 0; // should be synced to client by server, also add a float to track and interpolate away difference

    public DimensionManager(){
        registerDimensions();
        SimpleNetworkPacket.registerReceiver(TimeSync.PACKAGE_ID_SYNCTIME, new TimeSync());
    }

    public void serverTick(ServerTickEvent.Post event){
        for(Dimension i : dimensions.values()){
            i.serverTick(event);
        }
        universalTimeServer += 1;
        if(universalTimeServer % 200 == 0){
            PacketDistributor.sendToAllPlayers(new SimpleNetworkPacket(TimeSync.PACKAGE_ID_SYNCTIME,String.valueOf(universalTimeServer)));
        }
    }
    public void clientTick(ClientTickEvent.Post event){
        for(Dimension i : dimensions.values()){
            i.clientTick();
        }
        universalTimeClient += 1;
    }

    public static ServerLevel getServerLevel(MinecraftServer server, ResourceLocation dimensionId){
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
    }


    public void registerDimensions(){

        dimensions.clear();

        DimensionProperties sun = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "sun"));
        sun.earthMassMultiplier = 200;
        sun.earthRadiusMultiplier = 100;
        sun.rotationAxis = new Vec3(0,1,0).normalize();
        sun.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/sun_grayscale_ico_1k.png");
        sun.emissiveColor = new Vector4f(0.9f,0.9f,0.7f, 1f);
        sun.reflectivity = 0f;
        dimensions.put(sun.dimensionId, new Dimension(sun));

        DimensionProperties overworld = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
        overworld.parentDimensionId = sun.dimensionId;
        overworld.lightSourceDimensionId = sun.dimensionId;
        overworld.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/8k_earth_daymap.png");
        overworld.rotationAxis = new Vec3(0.5,1,0).normalize();
        overworld.targetDayLength = 12000;
        overworld.skyColor = new Vector3f(0.53f, 0.81f, 0.92f);
        dimensions.put(overworld.dimensionId,new Dimension(overworld));


        DimensionProperties moon = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon"));
        moon.parentDimensionId = overworld.dimensionId;
        moon.lightSourceDimensionId = sun.dimensionId;
        moon.orbitalDistanceToParent = 0.00257;
        moon.orbitAxis = new Vec3(0,1,0);
        moon.earthRadiusMultiplier = 0.272;
        moon.earthMassMultiplier = 0.3;
        moon.targetDayLength = 1000;
        moon.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_512.png");
        dimensions.put(moon.dimensionId,new Dimension(moon));


        DimensionProperties moon2 = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon2"));
        moon2.parentDimensionId = overworld.dimensionId;
        moon2.lightSourceDimensionId = sun.dimensionId;
        moon2.orbitalDistanceToParent = 0.005;
        moon2.orbitAxis = new Vec3(0.1,1,0).normalize();
        moon2.earthRadiusMultiplier = 0.2;
        moon2.earthMassMultiplier = 0.2;
        moon2.targetDayLength = 4000;
        moon2.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_512.png");
        dimensions.put(moon2.dimensionId,new Dimension(moon2));


        DimensionProperties moon3 = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon3"));
        moon3.parentDimensionId = moon2.dimensionId;
        moon3.lightSourceDimensionId = sun.dimensionId;
        moon3.orbitalDistanceToParent = 0.001;
        moon3.orbitAxis = new Vec3(0,0,1).normalize();
        moon3.rotationAxis = new Vec3(1,0,0);
        moon3.earthRadiusMultiplier = 0.1;
        moon3.earthMassMultiplier = 0.1;
        moon3.targetDayLength = 1000;
        moon3.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_512.png");
        dimensions.put(moon3.dimensionId,new Dimension(moon3));



        DimensionProperties sun1 = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "sun1"));
        sun1.earthMassMultiplier = 200;
        sun1.earthRadiusMultiplier = 100;
        sun1.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/sun_grayscale_ico_1k.png");
        sun1.emissiveColor = new Vector4f(0.9f,0.5f,0f, 1f);
        sun1.reflectivity = 0f;
        sun1.position = new Vec3(0,0,2);
        dimensions.put(sun1.dimensionId, new Dimension(sun1));

        DimensionProperties sun2 = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "sun2"));
        sun2.earthMassMultiplier = 200;
        sun2.earthRadiusMultiplier = 100;
        sun2.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/sun_grayscale_ico_1k.png");
        sun2.emissiveColor = new Vector4f(0f,0.5f,0.9f, 1f);
        sun2.reflectivity = 0f;
        sun2.position = new Vec3(1,0,1);
        dimensions.put(sun2.dimensionId, new Dimension(sun2));





    }

    public class TimeSync implements SimpleNetworkPacket.SimpleNetworkDataReceiver {
        public static String PACKAGE_ID_SYNCTIME = "DimensionManager_TimeSync";
        @Override
        public void readClient(String data) {
            universalTimeClient = Long.parseLong(data);
        }
    }

}
