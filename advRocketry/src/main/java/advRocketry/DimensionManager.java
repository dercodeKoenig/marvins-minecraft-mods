package advRocketry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;

public class DimensionManager {
    public static DimensionManager INSTANCE = new DimensionManager();

    private HashMap<ResourceLocation, DimensionProperties> dimensions = new HashMap<>();

    public DimensionManager(){
        registerDimensions();
    }


    public void serverTick(ServerTickEvent.Post event){
        for(DimensionProperties i : dimensions.values()){
            i.serverTick(event);
        }
    }
    public void clientTick(ClientTickEvent.Post event){

    }

    public DimensionProperties getDimensionProperties(ResourceLocation dimensionId){
        return dimensions.get(dimensionId);
    }

    public static ServerLevel getServerLevel(MinecraftServer server, ResourceLocation dimensionId){
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
    }


    public void registerDimensions(){
        DimensionProperties overworld = new DimensionProperties();
    }
}
