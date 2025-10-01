package advRocketry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;

public class DimensionManager {
    public static DimensionManager INSTANCE = new DimensionManager();

    public HashMap<ResourceLocation, DimensionProperties> dimensions = new HashMap<>();

    public DimensionManager(){
        registerDimensions();
    }


    public void serverTick(ServerTickEvent.Post event){
        for(DimensionProperties i : dimensions.values()){
            i.serverTick(event);
        }
    }
    public void clientTick(ClientTickEvent.Post event){
        for(DimensionProperties i : dimensions.values()){
            i.tick();
        }
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
        dimensions.put(sun.dimensionId, sun);


        DimensionProperties overworld = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
        overworld.parentDimensionId = sun.dimensionId;
        overworld.lightSourceDimensionId = sun.dimensionId;
        overworld.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/8k_earth_daymap.png");
        overworld.rotationAxis = new Vec3(0.5,1,0).normalize();
        overworld.targetDayLength = 48000;
        //overworld.targetDayLength = 3000;
        dimensions.put(overworld.dimensionId,overworld);


        DimensionProperties moon = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon"));
        moon.parentDimensionId = overworld.dimensionId;
        moon.lightSourceDimensionId = sun.dimensionId;
        moon.orbitalDistanceToParent = 20;
        moon.orbitAxis = new Vec3(0,1,0);
        moon.size = 30;
        moon.mass = 20;
        moon.targetDayLength = 1000;
        moon.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_512.png");
        dimensions.put(moon.dimensionId,moon);


        DimensionProperties moon2 = new DimensionProperties(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon2"));
        moon2.parentDimensionId = overworld.dimensionId;
        moon2.lightSourceDimensionId = sun.dimensionId;
        moon2.orbitalDistanceToParent = 30;
        moon2.orbitAxis = new Vec3(0.1,1,0).normalize();
        moon2.size = 20;
        moon2.mass = 30;
        moon2.targetDayLength = 4000;
        moon2.texture = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "textures/planet/moon_ico_512.png");
        dimensions.put(moon2.dimensionId,moon2);

    }
}
