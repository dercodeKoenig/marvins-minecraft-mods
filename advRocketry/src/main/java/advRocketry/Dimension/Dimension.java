package advRocketry.Dimension;

import advRocketry.Render.PlanetRenderCache;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.CelestialUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;

import static advRocketry.utils.CelestialUtils.fromAU;
import static advRocketry.utils.CelestialUtils.fromEarthMasses;

public class Dimension {
    boolean requiresSaveProperties;
    DimensionProperties properties;
    public PlanetRenderCache planetRenderCache;
    public ClientOnly clientOnly;


    public Dimension(DimensionProperties properties) {
        this.properties = properties;
        if (FMLLoader.getDist().isClient()) {
            clientOnly = new ClientOnly();
            planetRenderCache = new PlanetRenderCache();
        }
        setRequiresSaveProperties();
    }

    public void setRequiresSaveProperties() {
        requiresSaveProperties = true;
    }

    // TODO:
    //  on random tick, choose new target sky and fog colors and slowly interpolate between them to make diverse sky effects
    //  maybe adjust colors +-up to 10% of the original color channel value?

    public ResourceLocation getDimensionId() {
        return properties.dimensionId;
    }

    public Vector4f getEmissiveColor() {
        return properties.emissiveColor;
    }

    public Vec3 getRotationAxis() {
        return properties.rotationAxis;
    }

    public double getEarthRadiusMultiplier() {
        return properties.earthRadiusMultiplier;
    }

    public double getEarthMassMultiplier() {
        return properties.earthMassMultiplier;
    }

    public ResourceLocation getTexture() {
        return properties.texture;
    }

    public float getReflectivity() {
        return properties.reflectivity;
    }

    public Vector3f getSkyColor() {
        return new Vector3f(properties.skyColor);
    }

    public Vector3f getSunRiseColor() {
        return new Vector3f(properties.sunRiseColor);
    }

    public Vector3f getFogColor() {
        return new Vector3f(properties.fogColor);
    }

    public float getAtmosphereDensity() {
        return properties.atmosphereDensity;
    }

    public float getLatitude() {
        if (FMLLoader.getDist().isDedicatedServer()) return 0;
        else return clientOnly.getLatitude();
    }

    public boolean shouldRenderPlanetInSky() {
        return properties.type == DimensionProperties.PlanetType.PLANET ||
                properties.type == DimensionProperties.PlanetType.STAR;
    }
    public boolean hasCustomSky(){
        return properties.hasCustomSky;
    }

    public float getDayTimePerTick() {
        return (float) Level.TICKS_PER_DAY / properties.targetDayLength;
    }

    public double getRotationAngle(float partialTick) {
        double actualDayTime = properties.dayTime + getDayTimePerTick() * partialTick;
        double rotation = actualDayTime / Level.TICKS_PER_DAY * 360;
        return rotation;
    }

    public Vec3 getPosition(float partialTick) {
        if (properties.parentDimensionId != null) {
            Dimension parent = DimensionManager.INSTANCE.dimensions.get(properties.parentDimensionId);
            double ticksPerOrbit = CelestialUtils.calculateOrbitalPeriodTicks(fromEarthMasses(properties.earthMassMultiplier), fromEarthMasses(parent.properties.earthMassMultiplier), fromAU(properties.orbitalDistanceToParent));
            double orbitalProgress = (GlobalTime.getGlobalTime() % ticksPerOrbit) + (GlobalTime.getGlobalTimeClientCorrection() % ticksPerOrbit);
            double orbitAngleDegrees = orbitalProgress * (360.0 / ticksPerOrbit) + properties.orbitalBaseOffsetDegrees;

            // 1. Define a simple, non-zero vector to use for the cross-product
            // This is an arbitrary direction, often chosen to align with a major axis.
            Vec3 arbitraryVector = new Vec3(0, 0, 1); // e.g., the Z-axis

            // 2. Find a starting vector orthogonal to the orbitAxis
            Vec3 startDirection = properties.orbitAxis.cross(arbitraryVector);

            // 3. Handle the edge case where orbitAxis is parallel to arbitraryVector (e.g., orbitAxis is <0,0,1>)
            // If the cross-product is zero length, orbitAxis and arbitraryVector are parallel.
            if (startDirection.length() < 0.0001d) {
                // Fallback: cross with a different axis (e.g., the X-axis)
                arbitraryVector = new Vec3(1, 0, 0);
                startDirection = properties.orbitAxis.cross(arbitraryVector);
            }

            // 4. Normalize the orthogonal vector and scale it to the orbital distance
            // This is your correct 'baseOffset' vector, originating at the parent and orthogonal to the rotation axis.
            Vec3 baseOffset = startDirection.normalize().scale(properties.orbitalDistanceToParent);

            // 5. Rotate the baseOffset around the orbitAxis by the current angle
            // baseOffset is now the vector V_start, and orbitAxis is the vector A.
            Vec3 rotatedOffset = CelestialUtils.rotate(baseOffset, properties.orbitAxis, orbitAngleDegrees);

            // 6. Add parent's position to get global position
            properties.position = parent.getPosition(partialTick).add(rotatedOffset);
        }
        return properties.position;
    }


    /**
     * calculates universe global coordinates for the local north east up coordinates of the planet
     */
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        // 1. Pick the correct perpendicular vector to axis
        Vec3 equatorRef = getEquatorReference(partialTick);

        Vec3 rotationAxis = properties.rotationAxis;

        // 2. Rotate the equatorRef by raw self-rotation
        Vec3 rotatedEquator = CelestialUtils.rotate(equatorRef, rotationAxis, getRotationAngle(partialTick));

        // 3. Get east vector
        Vec3 east = rotationAxis.cross(rotatedEquator).normalize();

        // 4 rotate equator reference around east by latitude
        double lat = getLatitude();
        Vec3 localUp = CelestialUtils.rotate(rotatedEquator, east, lat).normalize();

        // 5 calculate new north
        Vec3 north = localUp.cross(east).normalize();

        return new AxisDirections(north, east, localUp);
    }

    /**
     * computes the dot product between the surface normal at the observer and the target space object
     * allows to input precomputed positions to avoid recomputation
     */
    public double getSurfaceDotToTarget(Dimension target, float partialTick, @Nullable Vec3 myPlanetPosition, @Nullable Vec3 targetPosition) {
        Vec3 localUp = getGlobalAxisDirections(partialTick).up;

        if (targetPosition == null) targetPosition = target.getPosition(partialTick);
        if (myPlanetPosition == null) myPlanetPosition = getPosition(partialTick);

        Vec3 targetDirection = targetPosition.subtract(myPlanetPosition).normalize();
        double dot = localUp.dot(targetDirection);
        return dot;
    }

    /**
     * computes the accumulated brightness by relevant stars to be used for terrain shading
     */
    public double getAccumulatedWorldBrightness(float partialTick, float dotOffset, @Nullable Vec3 myPlanetPosition) {
//if(true)return 1;
        if (myPlanetPosition == null) myPlanetPosition = getPosition(partialTick);

        double astronomicalBrightness = 0;
        for (ResourceLocation targetId : planetRenderCache.significantLightSourcesCache.keySet()) {
            Dimension target = DimensionManager.get(targetId);
            Vec3 targetPosition = target.getPosition(partialTick);
            double distance = targetPosition.distanceTo(myPlanetPosition);
            double dotMultiplier = Math.max(0, (getSurfaceDotToTarget(target, partialTick, myPlanetPosition, targetPosition) + dotOffset) / (1 + dotOffset));
            double brightness = dotMultiplier * target.getEmissiveColor().w / (distance * distance);
            astronomicalBrightness += brightness;
        }
        return astronomicalBrightness;
    }

    /**
     * returns a reference vector for the equator, orthogonal to the rotation axis and the reference space object for day start
     */
    public Vec3 getEquatorReference(float partialTick) {
        // use main light source as reference for day start
        Dimension mainLightSource = DimensionManager.get(properties.dayTimeReference);
        Vec3 lightToPlanet = getPosition(partialTick).subtract(mainLightSource.getPosition(partialTick));
        Vec3 equatorReference = lightToPlanet.cross(properties.rotationAxis).scale(-1);
        return equatorReference;
    }


    public void trackDayTimeNormal() {
        properties.dayTime += getDayTimePerTick();
        properties.dayTime = properties.dayTime % Level.TICKS_PER_DAY;
    }

    public void serverTick(ServerTickEvent event) {
        ServerLevel level = DimensionManager.getServerLevel(event.getServer(), properties.dimensionId);
        if (level != null) {
            level.setDayTimePerTick(getDayTimePerTick());
            properties.dayTime = level.getDayTime();
        } else {
            trackDayTimeNormal();
        }
    }

    public class ClientOnly {
        public float getLatitude() {
            // player uses latitude based on location on planet
            Player p = Minecraft.getInstance().player;
            double z = p.position().z;
            double s = z / properties.latitude_len;
            float lat = (float) Math.sin(s * Math.PI * 2) * 90;
            return lat;
        }

        public void clientTick() {
            Level level = Minecraft.getInstance().level;
            if (level != null && properties.dimensionId.equals(level.dimension().location())) {
                properties.dayTime = level.getDayTime();
            } else {
                trackDayTimeNormal();
            }
            planetRenderCache.updateSignificantLightSourcesCache(Dimension.this);
        }
    }
}


/*
    public static float getSunAltitudeDegrees(DimensionProperties myPlanet, DimensionProperties lightSource, float partialTick) {
        double altitude = Math.asin(getSurfaceDotToPlanet(myPlanet, lightSource, partialTick, null, null));
        return (float) Math.toDegrees(altitude);
    }
 */

