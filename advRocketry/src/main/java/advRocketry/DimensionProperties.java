package advRocketry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;


public class DimensionProperties {

    ResourceLocation dimensionId;
    double size = 100;
    double mass = 100;
    Vec3 position = new Vec3(0, 0, 0);
    Vec3 rotationAxis = new Vec3(0, 1, 0);
    int targetDayLength = 24000;
    double selfRotationDegrees;


    ResourceLocation parentDimensionId;
    Vec3 orbitAxis = new Vec3(0, 1, 0);
    double orbitalDistanceToParent = 100;
    double orbitAngleDegrees = 0;

    ResourceLocation LightSourceDimensionId;

    ResourceLocation texture;

    Vec3 skyColor = new Vec3(0.471, 0.655, 1.0);
    Vec3 fogColor = skyColor;

    float atmosphereDensity = 1;

    public DimensionProperties(ResourceLocation dimensionId) {
        this.dimensionId = dimensionId;
    }

    double currentGameTime;

    void tick() {
        currentGameTime += (double) Level.TICKS_PER_DAY / (double) targetDayLength;
        if (currentGameTime > Level.TICKS_PER_DAY) {
            currentGameTime -= Level.TICKS_PER_DAY;
        }
        //currentGameTime = 2000;
        selfRotationDegrees = currentGameTime / Level.TICKS_PER_DAY * 360 + orbitAngleDegrees + 180;
        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))){
            //System.out.println(selfRotationDegrees+":"+orbitAngleDegrees);
        }

        if (parentDimensionId != null) {

            DimensionProperties parent = DimensionManager.INSTANCE.dimensions.get(parentDimensionId);


            // TODO: add inverse orbits
            double parentMass = parent.mass;
            double rotationIncrement = CelestialUtils.calculateOrbitalPeriodTicks(mass, parentMass, orbitalDistanceToParent);
            orbitAngleDegrees += 360d / rotationIncrement;
            if (orbitAngleDegrees > 360d)
                orbitAngleDegrees -= 360d;
            //orbitAngleDegrees = 0;

            // 1. Define a simple, non-zero vector to use for the cross-product
            // This is an arbitrary direction, often chosen to align with a major axis.
            Vec3 arbitraryVector = new Vec3(0, 0, 1); // e.g., the Z-axis

            // 2. Find a starting vector orthogonal to the orbitAxis
            Vec3 orbitPlaneX = orbitAxis.cross(arbitraryVector);

            // 3. Handle the edge case where orbitAxis is parallel to arbitraryVector (e.g., orbitAxis is <0,0,1>)
            // If the cross-product is zero length, orbitAxis and arbitraryVector are parallel.
            if (orbitPlaneX.length() < 0.0001d) {
                // Fallback: cross with a different axis (e.g., the X-axis)
                arbitraryVector = new Vec3(1, 0, 0);
                orbitPlaneX = orbitAxis.cross(arbitraryVector);
            }

            // 4. Normalize the orthogonal vector and scale it to the orbital distance
            // This is your correct 'baseOffset' vector, originating at the parent and orthogonal to the rotation axis.
            Vec3 baseOffset = orbitPlaneX.normalize().scale(orbitalDistanceToParent);

            // 5. Rotate the baseOffset around the orbitAxis by the current angle
            // baseOffset is now the vector V_start, and orbitAxis is the vector A.
            Vec3 rotatedOffset = CelestialUtils.rotate(baseOffset, orbitAxis, orbitAngleDegrees);

            // 6. Add parent's position to get global position
            position = parent.position.add(rotatedOffset);
            //System.out.println(dimensionId+":"+position.x+":"+position.y+":"+position.z);
        }
    }

    public void serverTick(ServerTickEvent event) {
        tick();

        MinecraftServer server = event.getServer();
        ServerLevel level = DimensionManager.getServerLevel(server, dimensionId);
        if (level == null) return;
        level.setDayTime((long) currentGameTime);
    }
}
