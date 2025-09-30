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
    Vec3 position = new Vec3(0, 0, 0);
    Vec3 rotationAxis = new Vec3(0, 1, 0);
    int targetDayLength = 24000;
    double rotationDegrees;

    ResourceLocation parentDimensionId;
    Vec3 orbitAxis = new Vec3(0, 1, 0);
    double orbitalDistanceToParent = 100;
    double orbitAngleDegrees = 0;

    ResourceLocation lightSourceDimensionId;

    ResourceLocation texture;

    Vec3 skyColor = new Vec3(0.471, 0.655, 1.0);
    Vec3 fogColor = skyColor;

    float atmosphereDensity = 1;

    public DimensionProperties(ResourceLocation dimensionId) {
        this.dimensionId = dimensionId;
    }

    double currentGameTime;

    // TODO delta tick stuff!!
    public double getSelfRotationDegrees(double deltatick){
        double d = getDayTimeDeltaPerTick() * deltatick;
        return (d+currentGameTime) / Level.TICKS_PER_DAY * 360 + orbitAngleDegrees + 90;
    }
    public double getOrbitDegrees(double deltatick){
        double d = getOrbitDeltaPerTick();
        return orbitAngleDegrees + d;
    }
    public double getDayTimeDeltaPerTick(){
        return (double) Level.TICKS_PER_DAY / (double) targetDayLength;
    }
    public double getOrbitDeltaPerTick(){
        DimensionProperties parent = DimensionManager.INSTANCE.dimensions.get(parentDimensionId);
        if (parent == null)return 0;
        return 360d / CelestialUtils.calculateOrbitalPeriodTicks(mass, parent.mass, orbitalDistanceToParent);
    }

    void tick() {
        currentGameTime += getDayTimeDeltaPerTick();
        if (currentGameTime > Level.TICKS_PER_DAY) {
            currentGameTime -= Level.TICKS_PER_DAY;
        }
        if(dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))) {
            currentGameTime = 6000;
        }

        if (parentDimensionId != null) {

            DimensionProperties parent = DimensionManager.INSTANCE.dimensions.get(parentDimensionId);
            // TODO: add inverse orbits
            orbitAngleDegrees += getOrbitDeltaPerTick();
            if (orbitAngleDegrees > 360d)
                orbitAngleDegrees -= 360d;

            orbitAngleDegrees = 0;
            if(dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon"))){
                orbitAngleDegrees = 90;
            }
            if(dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon2"))){
                orbitAngleDegrees = 40;
            }
            if(dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))){
                orbitAngleDegrees = 180;
            }

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


        // detect and adjust if the time changes by command or sleep or overwrite time
        long defaultGameTime = level.getDayTime() % Level.TICKS_PER_DAY;
        double directDiff = Math.abs(currentGameTime - defaultGameTime);
        double wrapAroundDiff = Level.TICKS_PER_DAY - directDiff;
        double smallestDifference = Math.min(directDiff, wrapAroundDiff);
        if (smallestDifference > 10* Math.max(1, getDayTimeDeltaPerTick())) {
            System.out.println("adjust current time to "+defaultGameTime+" from "+currentGameTime);
            currentGameTime = defaultGameTime;
        }
        else
            level.setDayTime((long) currentGameTime);
    }
}
