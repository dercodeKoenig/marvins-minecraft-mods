package advRocketry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;


public class DimensionProperties {

    ResourceLocation dimensionId;
    double scale;
    double mass;
    Vec3 position;
    Vec3 rotationAxis;
    int targetDayLength = 6000;
    double orbitalDistanceToParent;
    double selfRotationDegrees;
    double orbitAngleDegrees;
    ResourceLocation parentId;
    ResourceLocation LightSourceId;

    ResourceLocation planetTexture;

    Vec3 skyColor;
    Vec3 fogColor;

    float atmosphereDensity = 1;

    public DimensionProperties(){

    }

    double currentGameTime;

    void tickGameTime(){
        currentGameTime += (double)Level.TICKS_PER_DAY / (double)targetDayLength;
        if (currentGameTime > Level.TICKS_PER_DAY){
            currentGameTime -= Level.TICKS_PER_DAY;
        }

        if (parentId != null) {
            // TODO: add inverse orbits
            double parentMass = DimensionManager.INSTANCE.getDimensionProperties(parentId).mass;
            double rotationIncrement = CelestialUtils.calculateOrbitalPeriodTicks(mass, parentMass, orbitalDistanceToParent);
            orbitAngleDegrees += 360d / rotationIncrement;
            if (orbitAngleDegrees > 360d)
                orbitAngleDegrees -= 360d;
        }
    }
    public void serverTick(ServerTickEvent event){
        tickGameTime();

        MinecraftServer server = event.getServer();
        ServerLevel level = DimensionManager.getServerLevel(server, dimensionId);
        level.setDayTime((long) currentGameTime);
    }
}
