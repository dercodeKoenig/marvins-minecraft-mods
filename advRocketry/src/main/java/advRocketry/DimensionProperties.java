package advRocketry;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;


public class DimensionProperties {

    public ResourceLocation dimensionId = null;             // required
    public double size = 100;                               // optional with default
    public double mass = 100;                               // optional with default
    private Vec3 position = new Vec3(0, 0, 0);       // optional with default
    public Vec3 rotationAxis = new Vec3(0, 1, 0);   // optional with default
    public int targetDayLength = 24000;                     // optional with default
    public float dayTime;

    public ResourceLocation parentDimensionId = null;       // optional, overwrites position
    public Vec3 orbitAxis = new Vec3(0, 1, 0);      // optional with default
    public double orbitalDistanceToParent = 100;            // optional with default

    public ResourceLocation lightSourceDimensionId = null;  // required (reference for day start)

    public ResourceLocation texture = null;                 // required (planet texture)

    public Vector3f skyColor = new Vector3f(0.471f, 0.655f, 1.0f);  // optional with default
    public Vector3f fogColor = new Vector3f(0.8f, 0.98f, 1.0f);     // optional with default
    public float reflectivity = 1f;                                         // optional with default
    public Vector4f emissiveColor = new Vector4f(0, 0, 0, 0);    // optional with default
    public float atmosphereDensity = 1;                                     // optional with default

    public int latitude_len = 200000;                                        // optional with default, how much you have to move in z direction to "go around the planet"


    public DimensionProperties(ResourceLocation dimensionId) {
        this.dimensionId = dimensionId;
    }

    public Vec3 getEquatorReference(float partialTick) {
        // use main light source as reference for day start
        DimensionProperties mainLightSource = DimensionManager.get(lightSourceDimensionId);
        Vec3 lightToPlanet = getPosition(partialTick).subtract(mainLightSource.getPosition(partialTick));
        Vec3 equatorReference = lightToPlanet.cross(rotationAxis);
        return equatorReference;
    }

    @OnlyIn(Dist.CLIENT)
    public float getLatitude() {
        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            return 0; // server uses always equator
        }
        Player p = Minecraft.getInstance().player;
        double z = p.position().z;
        double s = z / latitude_len;
        float lat = (float) Math.sin(s * Math.PI * 2) * 90;
        return lat;
    }

    public float getDayTimeDeltaPerTick() {
        return (float) Level.TICKS_PER_DAY / targetDayLength;
    }

    public double getRotationAngle(float partialTick) {
        double actualDayTime = dayTime + getDayTimeDeltaPerTick() * partialTick;
        double rotation = actualDayTime / Level.TICKS_PER_DAY * 360;
        return rotation;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public Vec3 getPosition(float partialTick) {
        if (parentDimensionId != null) {
            DimensionProperties parent = DimensionManager.INSTANCE.dimensions.get(parentDimensionId);
            double ticksPerOrbit = CelestialUtils.calculateOrbitalPeriodTicks(mass, parent.mass, orbitalDistanceToParent);
            double orbitAngleDegrees = (DimensionManager.getGlobalTime() % ticksPerOrbit) * (360.0 / ticksPerOrbit);

            if (true) { // debug / testing
                orbitAngleDegrees = 0;
                if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon"))) {
                    orbitAngleDegrees = 80;
                }
                if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon2"))) {
                    orbitAngleDegrees = 40;
                }
                if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))) {
                    orbitAngleDegrees = 270;
                }
            }

            // 1. Define a simple, non-zero vector to use for the cross-product
            // This is an arbitrary direction, often chosen to align with a major axis.
            Vec3 arbitraryVector = new Vec3(0, 0, 1); // e.g., the Z-axis

            // 2. Find a starting vector orthogonal to the orbitAxis
            Vec3 startDirection = orbitAxis.cross(arbitraryVector);

            // 3. Handle the edge case where orbitAxis is parallel to arbitraryVector (e.g., orbitAxis is <0,0,1>)
            // If the cross-product is zero length, orbitAxis and arbitraryVector are parallel.
            if (startDirection.length() < 0.0001d) {
                // Fallback: cross with a different axis (e.g., the X-axis)
                arbitraryVector = new Vec3(1, 0, 0);
                startDirection = orbitAxis.cross(arbitraryVector);
            }

            // 4. Normalize the orthogonal vector and scale it to the orbital distance
            // This is your correct 'baseOffset' vector, originating at the parent and orthogonal to the rotation axis.
            Vec3 baseOffset = startDirection.normalize().scale(orbitalDistanceToParent);

            // 5. Rotate the baseOffset around the orbitAxis by the current angle
            // baseOffset is now the vector V_start, and orbitAxis is the vector A.
            Vec3 rotatedOffset = CelestialUtils.rotate(baseOffset, orbitAxis, orbitAngleDegrees);

            // 6. Add parent's position to get global position
            setPosition(parent.getPosition(partialTick).add(rotatedOffset));
        }
        return position;
    }

    public Vector3f getAtmosphereColor() {
        float brightnessMultiplier = (float) (2 * (0.5 - Minecraft.getInstance().level.getStarBrightness(0)));
        Vec3 minecraftColor = Minecraft.getInstance().level.getSkyColor(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), 0);
        //return new Vector3f(skyColor.x * brightnessMultiplier, skyColor.y * brightnessMultiplier, skyColor.z * brightnessMultiplier);
        return minecraftColor.toVector3f();
    }

    public Vector3f getFogColor() {
        float brightnessMultiplier = (float) (2 * (0.5 - Minecraft.getInstance().level.getStarBrightness(0)));
        return new Vector3f(fogColor.x * brightnessMultiplier, fogColor.y * brightnessMultiplier, fogColor.z * brightnessMultiplier);
    }

    void tick() {
        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))) {
            //currentGameTime = 6000;
            skyColor = new Vector3f(0.5f, 0.5f, 1);
            fogColor = new Vector3f(0.8f, 0.98f, 1.0f);
        }
    }

    // TODO: split every variable in server and client variable, use a special client tick to adjust client variables.
    // otherwise it shares variables between server and client thread and this will cause problems


    public void keepDayTimeSync(Level level) {
        if (level != null) {
            level.setDayTimePerTick(getDayTimeDeltaPerTick());
            float mcDayTime = level.getDayTime() + level.getDayTimeFraction();
            dayTime = mcDayTime;
        } else {
            dayTime += getDayTimeDeltaPerTick();
            dayTime = dayTime % Level.TICKS_PER_DAY;
        }
    }

    public void clientTick(ClientTickEvent event) {
        Level l = Minecraft.getInstance().level;
        if (l != null && l.dimension().location().equals(dimensionId))
            keepDayTimeSync(l);
        else keepDayTimeSync(null);

        tick();
    }

    public void serverTick(ServerTickEvent event) {
        ServerLevel level = DimensionManager.getServerLevel(event.getServer(), dimensionId);
        keepDayTimeSync(level);
        ///  TODO: server has a master clock for orbit calculations and will sync this to client
        //tick();
    }
}
