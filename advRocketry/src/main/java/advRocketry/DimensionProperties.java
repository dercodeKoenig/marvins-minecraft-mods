package advRocketry;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;


public class DimensionProperties {

    public ResourceLocation dimensionId;
    public double size = 100;
    public double mass = 100;
    public Vec3 position = new Vec3(0, 0, 0);
    public Vec3 rotationAxis = new Vec3(0, 1, 0);
    public int targetDayLength = 24000;
    public double dayTime;

    public ResourceLocation parentDimensionId;
    public Vec3 orbitAxis = new Vec3(0, 1, 0);
    public double orbitalDistanceToParent = 100;
    public double orbitAngleDegrees = 0;

    public ResourceLocation lightSourceDimensionId;

    public ResourceLocation texture;

    public Vector3f skyColor = new Vector3f(0.471f, 0.655f, 1.0f);
    public Vector3f fogColor = new Vector3f(0.8f, 0.98f, 1.0f);
    public float reflectivity = 1f;
    public Vector4f emissiveColor = new Vector4f(0, 0, 0, 0);
    public float atmosphereDensity = 1;

    public int latitude_len = 20000;


    public DimensionProperties(ResourceLocation dimensionId) {
        this.dimensionId = dimensionId;
    }

    public double getSelfRotationDegrees(double deltatick) {
        double d = getDayTimeDeltaPerTick() * deltatick;
        double result = (d + dayTime) / Level.TICKS_PER_DAY * 360 + orbitAngleDegrees + 90;
        return result;
    }
    public double getRawSelfRotationDegrees(double deltatick) {
        double d = getDayTimeDeltaPerTick() * deltatick;
        return (d + dayTime) / Level.TICKS_PER_DAY * 360+90;
    }


    public float getLatitude() {
        Player p = Minecraft.getInstance().player;
        double z = p.position().z;
        double s = z / latitude_len;
        float lat = (float) Math.sin(s * Math.PI * 2) * 90;
        return lat;
    }

    public double getOrbitDegrees(double deltatick) {
        double d = getOrbitDeltaPerTick();
        return orbitAngleDegrees + d;
    }

    public float getDayTimeDeltaPerTick() {
        return (float) Level.TICKS_PER_DAY / targetDayLength;
    }

    public double getOrbitDeltaPerTick() {
        DimensionProperties parent = DimensionManager.INSTANCE.dimensions.get(parentDimensionId);
        if (parent == null) return 0;
        return 360d / CelestialUtils.calculateOrbitalPeriodTicks(mass, parent.mass, orbitalDistanceToParent);
    }

    public Vector3f getAtmosphereColor() {
        float brightnessMultiplier = (float) (2 * (0.5 - Minecraft.getInstance().level.getStarBrightness(0)));
        Vec3 minecraftColor =  Minecraft.getInstance().level.getSkyColor(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),0);
        //return new Vector3f(skyColor.x * brightnessMultiplier, skyColor.y * brightnessMultiplier, skyColor.z * brightnessMultiplier);
        return minecraftColor.toVector3f();
    }

    public Vector3f getFogColor() {
        float brightnessMultiplier = (float) (2 * (0.5 - Minecraft.getInstance().level.getStarBrightness(0)));
        return new Vector3f(fogColor.x * brightnessMultiplier, fogColor.y * brightnessMultiplier, fogColor.z * brightnessMultiplier);
    }

    void tick() {
        dayTime += getDayTimeDeltaPerTick();
        if (dayTime > Level.TICKS_PER_DAY)
            dayTime -= Level.TICKS_PER_DAY;

        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))) {
//            System.out.println(currentGameTime+":"+getSelfRotationDegrees(0));
        }

        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))) {
            //currentGameTime = 6000;
            skyColor = new Vector3f(0.5f, 0.5f, 1);
            fogColor = new Vector3f(0.8f, 0.98f, 1.0f);
        }

        if (parentDimensionId != null) {

            DimensionProperties parent = DimensionManager.INSTANCE.dimensions.get(parentDimensionId);
            // TODO: add inverse orbits
            orbitAngleDegrees += getOrbitDeltaPerTick();
            if (orbitAngleDegrees > 360d)
                orbitAngleDegrees -= 360d;

            orbitAngleDegrees = 0;
            if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon"))) {
                orbitAngleDegrees = 80;
            }
            if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon2"))) {
                orbitAngleDegrees = 40;
            }
            if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))) {
                orbitAngleDegrees =180;
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

        }
    }

    // TODO: split every variable in server and client variable, use a special client tick to adjust client variables.
    // otherwise it shares variables between server and client thread and this will cause problems


    public void keepDayTimeSync(Level level){
        if (level != null) {
            level.setDayTimePerTick(getDayTimeDeltaPerTick());

            // detect and adjust if the time changes by command or sleep or overwrite time
            long mcDayTime = level.getDayTime() % Level.TICKS_PER_DAY;
            double directDiff = Math.abs(dayTime - mcDayTime);
            double wrapAroundDiff = Level.TICKS_PER_DAY - directDiff;
            double smallestDifference = Math.min(directDiff, wrapAroundDiff);
            if (smallestDifference > 5 * Math.max(1, getDayTimeDeltaPerTick())) {
                System.out.println(dimensionId + " game time adjusted to dimension game time: " + dayTime + "->" + mcDayTime);
                dayTime = mcDayTime;
            }
        }
    }

    public void clientTick(ClientTickEvent event) {
        Level l = Minecraft.getInstance().level;
        if (l != null && l.dimension().location().equals(dimensionId))
            keepDayTimeSync(l);
        tick();
    }

    public void serverTick(ServerTickEvent event) {
        ServerLevel level = DimensionManager.getServerLevel(event.getServer(), dimensionId);
        keepDayTimeSync(level);
        //tick();
    }
}
