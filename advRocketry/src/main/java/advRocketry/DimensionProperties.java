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

import java.util.*;

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
    public Vector3f sunRiseColor = new Vector3f(0.471f, 0.655f, 0.2f);
    public Vector3f fogColor = new Vector3f(1.0f, 1.0f, 1.0f);
    public Vector4f emissiveColor = new Vector4f(0, 0, 0, 0);
    public float reflectivity = 1f;
    public float atmosphereDensity = 1;

    public int latitude_len = 200000;                                        // how much you have to move in z direction to "go around the planet"

    public float dayTime;

    public LinkedHashMap<ResourceLocation, Double> cachedLightSources = new LinkedHashMap<>();

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

            if (false) { // debug / testing
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


    public Vector3f getFogColor() {
        float brightnessMultiplier = Minecraft.getInstance().level.getSkyDarken(0);
        return new Vector3f(fogColor.x * brightnessMultiplier, fogColor.y * brightnessMultiplier, fogColor.z * brightnessMultiplier);
    }

    void tick() {
        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"))) {
            skyColor = new Vector3f(0.53f, 0.81f, 0.98f);
            fogColor = new Vector3f(0.8f, 0.95f, 1.0f);
            sunRiseColor = new Vector3f(1.0f, 0.81f, 0.5f);
        }

        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "sun"))) {
            emissiveColor = new Vector4f(1,1,1f,1);
            reflectivity = 0;
        }
        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "sun1"))) {
            //emissiveColor = new Vector4f(1,1,1f,1);
            reflectivity = 0;
        }
        if (dimensionId.equals(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "sun2"))) {
            //emissiveColor = new Vector4f(1,1,1f,1);
            reflectivity = 0;
        }
    }

    public void trackDayTimeNormal() {
        dayTime += getDayTimePerTick();
        dayTime = dayTime % Level.TICKS_PER_DAY;
    }

    public void clientTick(ClientTickEvent event) {
        Level level = Minecraft.getInstance().level;
        if (level != null && dimensionId.equals(level.dimension().location())) {
            dayTime = level.getDayTime();
        } else {
            trackDayTimeNormal();
        }
        tick();
        updateCachedLightSourcesStep();
    }

    public void serverTick(ServerTickEvent event) {
        ServerLevel level = DimensionManager.getServerLevel(event.getServer(), dimensionId);
        if (level != null) {
            level.setDayTimePerTick(getDayTimePerTick());
            dayTime = level.getDayTime();
        } else {
            trackDayTimeNormal();
        }
    }


    private Iterator<DimensionProperties> dimIterator;
    private final int MAX_LIGHTSOURCES = 2;

    // updates the cached light sources that are considered for lighting calculations
    // for simplicity, only self emitted light is considered. if a moon reflects a lot of light, this would be ignored.
    public void updateCachedLightSourcesStep() {
        if (dimIterator == null || !dimIterator.hasNext()) {
            // Restart once we've gone through all dimensions
            dimIterator = DimensionManager.INSTANCE.dimensions.values().iterator();
        }

        if (dimIterator.hasNext()) {
            DimensionProperties props = dimIterator.next();
            ResourceLocation id = props.dimensionId;

            // skip if it is my id
            if (id.equals(dimensionId)) {
                return;
            }

            // Skip if it's already in the top list
            if (cachedLightSources.containsKey(id)) {
                return;
            }

            // skip if no color is emitted
            double emissiveBrightness = props.emissiveColor.w;
            if (emissiveBrightness <= 0) {
                return;
            }

            Vec3 myPos = getPosition(0);
            Vec3 targetPosition = props.getPosition(0);
            double distance = myPos.distanceTo(targetPosition);
            double brightness = emissiveBrightness / (distance * distance);

            // If we still have room, just add it
            if (cachedLightSources.size() < MAX_LIGHTSOURCES) {
                cachedLightSources.put(id, brightness);
            } else {
                // Find the dimmest currently stored and maybe replace it
                ResourceLocation weakestId = null;
                double weakestBrightness = Double.MAX_VALUE;

                for (Map.Entry<ResourceLocation, Double> entry : cachedLightSources.entrySet()) {
                    if (entry.getValue() < weakestBrightness) {
                        weakestBrightness = entry.getValue();
                        weakestId = entry.getKey();
                    }
                }

                // Replace if the new one is brighter
                if (brightness > weakestBrightness && weakestId != null) {
                    cachedLightSources.remove(weakestId);
                    cachedLightSources.put(id, brightness);
                }
            }
        }
    }
}
