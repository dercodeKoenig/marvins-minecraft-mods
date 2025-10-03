package advRocketry;

import ARLib.network.SimpleNetworkPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    public static DimensionProperties get(ResourceLocation key){
        return INSTANCE.dimensions.get(key);
    }
    public static long getGlobalTime() {
        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            return INSTANCE.universalTimeServer;
        }else{
            return INSTANCE.universalTimeClient;
        }
    }

    public HashMap<ResourceLocation, DimensionProperties> dimensions = new HashMap<>();
    public long universalTimeServer = 0;
    public long universalTimeClient = 0; // should be synced to client by server

    public DimensionManager(){
        registerDimensions();
        SimpleNetworkPacket.registerReceiver(TimeSync.PACKAGE_ID_SYNCTIME, new TimeSync());
    }

    public void serverTick(ServerTickEvent.Post event){
        for(DimensionProperties i : dimensions.values()){
            i.serverTick(event);
        }
        universalTimeServer += 1;
        if(universalTimeServer % 200 == 0){
            PacketDistributor.sendToAllPlayers(new SimpleNetworkPacket(TimeSync.PACKAGE_ID_SYNCTIME,String.valueOf(universalTimeServer)));
        }
    }
    public void clientTick(ClientTickEvent.Post event){
        for(DimensionProperties i : dimensions.values()){
            i.clientTick(event);
        }
        universalTimeClient += 1;
    }

    public static ServerLevel getServerLevel(MinecraftServer server, ResourceLocation dimensionId){
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
    }


    public void registerDimensions(){

        DimensionProperties sun = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "sun"));
        sun.mass = 200;
        sun.size = 200;
        sun.rotationAxis = new Vec3(0,1,0).normalize();
        sun.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/8k_sun.png");
        sun.emissiveColor = new Vector4f(0.9f,0.9f,0.7f, 10000f);
        sun.reflectivity = 0f;
        dimensions.put(sun.dimensionId, sun);


        DimensionProperties overworld = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
        overworld.parentDimensionId = sun.dimensionId;
        overworld.lightSourceDimensionId = sun.dimensionId;
        overworld.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/8k_earth_daymap.png");
        overworld.rotationAxis = new Vec3(0.5,1,0).normalize();
        overworld.targetDayLength = 12000;
        overworld.skyColor = new Vector3f(0.53f, 0.81f, 0.92f);
        dimensions.put(overworld.dimensionId,overworld);


        DimensionProperties moon = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon"));
        moon.parentDimensionId = overworld.dimensionId;
        moon.lightSourceDimensionId = sun.dimensionId;
        moon.orbitalDistanceToParent = 20;
        moon.orbitAxis = new Vec3(0,1,0);
        moon.size = 30;
        moon.mass = 30;
        moon.targetDayLength = 1000;
        moon.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_512.png");
        dimensions.put(moon.dimensionId,moon);


        DimensionProperties moon2 = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon2"));
        moon2.parentDimensionId = overworld.dimensionId;
        moon2.lightSourceDimensionId = sun.dimensionId;
        moon2.orbitalDistanceToParent = 30;
        moon2.orbitAxis = new Vec3(0.1,1,0).normalize();
        moon2.size = 20;
        moon2.mass = 20;
        moon2.targetDayLength = 4000;
        moon2.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_512.png");
        dimensions.put(moon2.dimensionId,moon2);


        DimensionProperties moon3 = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon3"));
        moon3.parentDimensionId = moon2.dimensionId;
        moon3.lightSourceDimensionId = sun.dimensionId;
        moon3.orbitalDistanceToParent = 10;
        moon3.orbitAxis = new Vec3(0,0,1).normalize();
        moon3.rotationAxis = new Vec3(1,0,0);
        moon3.size = 10;
        moon3.mass = 10;
        moon3.targetDayLength = 1000;
        moon3.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_512.png");
        dimensions.put(moon3.dimensionId,moon3);

    }

    public class TimeSync implements SimpleNetworkPacket.SimpleNetworkDataReceiver {
        public static String PACKAGE_ID_SYNCTIME = "DimensionManager_TimeSync";
        @Override
        public void readClient(String data) {
            universalTimeClient = Long.parseLong(data);
        }
    }

}
