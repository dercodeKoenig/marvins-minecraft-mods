package advRocketry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;


public class DimensionProperties {

    ResourceLocation dimensionId;
    double size = 100;
    double mass = 100;
    Vec3 position = new Vec3(0,0,0);
    Vec3 rotationAxis = new Vec3(0,1,0);
    int targetDayLength = 24000;
    double selfRotationDegrees;


    ResourceLocation parentDimensionId;
    Vec3 orbitAxis = new Vec3(0,1,0);
    double orbitalDistanceToParent = 100;
    double orbitAngleDegrees = 0;

    ResourceLocation LightSourceDimensionId;

    ResourceLocation texture;

    Vec3 skyColor  = new Vec3(0.471, 0.655, 1.0);
    Vec3 fogColor = skyColor ;

    float atmosphereDensity = 1;

    public DimensionProperties(ResourceLocation dimensionId){
        this.dimensionId = dimensionId;
    }

    double currentGameTime;

    void tick(){
        currentGameTime += (double)Level.TICKS_PER_DAY / (double)targetDayLength;
        if (currentGameTime > Level.TICKS_PER_DAY){
            currentGameTime -= Level.TICKS_PER_DAY;
        }

        selfRotationDegrees = currentGameTime / Level.TICKS_PER_DAY * 360 - orbitAngleDegrees; // this should adjust that time = 0 will always be sunrise

        if (parentDimensionId != null) {

            DimensionProperties parent = DimensionManager.INSTANCE.dimensions.get(parentDimensionId);


            // TODO: add inverse orbits
            double parentMass = parent.mass;
            double rotationIncrement = CelestialUtils.calculateOrbitalPeriodTicks(mass, parentMass, orbitalDistanceToParent);
            orbitAngleDegrees += 360d / rotationIncrement;
            if (orbitAngleDegrees > 360d)
                orbitAngleDegrees -= 360d;

            // Calculate 3D position by rotating around the orbit axis
            // Start with a base position at the orbital distance
            Vec3 baseOffset = new Vec3(orbitalDistanceToParent, 0, 0);

            // Rotate the offset around the orbit axis by the current orbit angle
            Vec3 rotatedOffset = CelestialUtils.rotate(baseOffset, orbitAxis, orbitAngleDegrees);

            // Add parent's position to get global position
            position = parent.position.add(rotatedOffset);
        }
    }
    public void serverTick(ServerTickEvent event){
        tick();

        MinecraftServer server = event.getServer();
        ServerLevel level = DimensionManager.getServerLevel(server, dimensionId);
        if (level == null) return;
        level.setDayTime((long) currentGameTime);
    }
}
