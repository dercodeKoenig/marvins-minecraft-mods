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

import static advRocketry.CelestialUtils.*;


public class DimensionProperties {

    public ResourceLocation dimensionId = null;             // required
    public double earthRadiusMultiplier = 1;
    public double earthMassMultiplier = 1;
    private Vec3 position = new Vec3(0, 0, 0);
    public Vec3 rotationAxis = new Vec3(0, 1, 0);
    public int targetDayLength = 24000;

    public ResourceLocation parentDimensionId = null;       // optional, overwrites position
    public Vec3 orbitAxis = new Vec3(0, 1, 0);
    public double orbitalDistanceToParent = 1;

    public ResourceLocation lightSourceDimensionId = null;  // required (reference for day start)

    public ResourceLocation texture = null;                 // required (planet texture)

    public Vector3f skyColor = new Vector3f(0.471f, 0.655f, 1.0f);
    public Vector3f fogColor = new Vector3f(0.8f, 0.98f, 1.0f);
    public Vector4f emissiveColor = new Vector4f(0, 0, 0, 0);
    public float reflectivity = 1f;
    public float atmosphereDensity = 1;

    public int latitude_len = 200000;                                        // how much you have to move in z direction to "go around the planet"

    public float dayTime;


    public DimensionProperties(ResourceLocation dimensionId) {
        this.dimensionId = dimensionId;
    }

    public Vec3 getEquatorReference(float partialTick) {
        // use main light source as reference for day start
        DimensionProperties mainLightSource = DimensionManager.get(lightSourceDimensionId);
        Vec3 lightToPlanet = getPosition(partialTick).subtract(mainLightSource.getPosition(partialTick));
        Vec3 equatorReference = lightToPlanet.cross(rotationAxis).scale(-1);
        return equatorReference;
    }

    @OnlyIn(Dist.CLIENT)
    public float getLatitude() {
        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            // server uses always equator
            return 0;
        }
        // player uses latitude based on location on planet
        Player p = Minecraft.getInstance().player;
        double z = p.position().z;
        double s = z / latitude_len;
        float lat = (float) Math.sin(s * Math.PI * 2) * 90;
        return lat;
    }

    public float getDayTimePerTick() {
        return (float) Level.TICKS_PER_DAY / targetDayLength;
    }

    public double getRotationAngle(float partialTick) {
        double actualDayTime = dayTime + getDayTimePerTick() * partialTick;
        double rotation = actualDayTime / Level.TICKS_PER_DAY * 360;
        return rotation;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public Vec3 getPosition(float partialTick) {
        if (parentDimensionId != null) {
            DimensionProperties parent = DimensionManager.INSTANCE.dimensions.get(parentDimensionId);
            double ticksPerOrbit = CelestialUtils.calculateOrbitalPeriodTicks(fromEarthMasses(earthMassMultiplier), fromEarthMasses(parent.earthMassMultiplier), fromAU(orbitalDistanceToParent));
            double orbitAngleDegrees = (DimensionManager.getGlobalTime() % ticksPerOrbit) * (360.0 / ticksPerOrbit);

            //reflectivity = 0.3f;
            if (true) { // debug / testing
                if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon"))) {
                    orbitAngleDegrees = 60;
                }
                if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon2"))) {
                    orbitAngleDegrees = 90;
                }
                if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon3"))) {
                    orbitAngleDegrees = 180;
                }
                if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))) {
                    orbitAngleDegrees = 0;
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
        float brightnessMultiplier =        Minecraft.getInstance().level.getSkyDarken(0);
        return new Vector3f(skyColor.x * brightnessMultiplier, skyColor.y * brightnessMultiplier, skyColor.z * brightnessMultiplier);

        // TODO: make the mixin for the minecraft defaul method and use default method or make a custom copy to adjust for lightning and biome color
        // Vec3 minecraftColor = Minecraft.getInstance().level.getSkyColor(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), 0);
        //return minecraftColor.toVector3f();
    }

    public Vector3f getFogColor() {
        float brightnessMultiplier =        Minecraft.getInstance().level.getSkyDarken(0);
        return new Vector3f(fogColor.x * brightnessMultiplier, fogColor.y * brightnessMultiplier, fogColor.z * brightnessMultiplier);
    }

    void tick() {
        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))) {
            //currentGameTime = 6000;
            skyColor = new Vector3f(0.5f, 0.5f, 1);
            fogColor = new Vector3f(0.8f, 0.98f, 1.0f);
        }
        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "sun"))) {
            //currentGameTime = 6000;
            emissiveColor = new Vector4f(0.9f,0.9f,0.7f, 1f);
        }
    }

    public void trackDayTimeNormal(){
        dayTime += getDayTimePerTick();
        dayTime = dayTime % Level.TICKS_PER_DAY;
    }

    public void clientTick(ClientTickEvent event) {
        Level level = Minecraft.getInstance().level;
        if (level!=null && dimensionId.equals(level.dimension().location())){
            dayTime =level.getDayTime();
        }else{
            trackDayTimeNormal();
        }
        tick();
    }

    public void serverTick(ServerTickEvent event) {
        ServerLevel level = DimensionManager.getServerLevel(event.getServer(), dimensionId);
        if (level != null) {
            level.setDayTimePerTick(getDayTimePerTick());
            dayTime = level.getDayTime();
        }else{
            trackDayTimeNormal();
        }
    }
}
