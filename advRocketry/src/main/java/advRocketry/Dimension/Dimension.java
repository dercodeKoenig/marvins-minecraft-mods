package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.Render.PlanetRenderCache;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.CelestialUtils;
import advRocketry.worldgen.BiomeConfig;
import advRocketry.worldgen.PlanetDimensionGeneration;
import advRocketry.worldgen.presets.HOT_VERYDRY;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;

import java.util.Optional;

import static advRocketry.utils.CelestialUtils.fromAU;
import static advRocketry.utils.CelestialUtils.fromEarthMasses;

public class Dimension {
    DimensionProperties properties;
    public PlanetRenderCache planetRenderCache;
    public ClientOnly clientOnly;

    float targetsealevel;
    float temperature;
    // all the gases need to be added too, gascomposition
    // maybe gases underground trapped / frozen that can be freed

    public Dimension(DimensionProperties properties) {
        this.properties = properties;
        if (FMLEnvironment.dist.isClient()) {
            clientOnly = new ClientOnly();
            planetRenderCache = new PlanetRenderCache();
        }

        if (getDimensionId().getNamespace().equals(Main.MODID) && canVisit()) {
            createDimension();
        }
    }

    public void createDimension() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        System.out.println("creating dimension for " + getDimensionId());
        DynamicDimensionRegistry dynamicDimensionRegistry = DynamicDimensionRegistry.from(server);

        ChunkGenerator generator = PlanetDimensionGeneration.makeChunkGenerator(
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                getSeaLevel(),
                properties.dimensionId.hashCode(),
                properties.generateStructures,
                BiomeConfig.loadPreset(HOT_VERYDRY.name)
        );
        DimensionType type = PlanetDimensionGeneration.makePlanetDimensionType();
        ServerLevel l = dynamicDimensionRegistry.loadDynamicDimension(properties.dimensionId, generator, type);
        if (l == null) {
            dynamicDimensionRegistry.createDynamicDimension(
                    properties.dimensionId,
                    generator,
                    type
            );
            System.out.println("created dimension for " + properties.dimensionId);
        } else {
            System.out.println("loaded dimension for " + properties.dimensionId);
        }
    }

    // TODO:
    //  on random tick, choose new target sky and fog colors and slowly interpolate between them to make diverse sky effects
    //  maybe adjust colors +-up to 10% of the original color channel value?

    public boolean canVisit() {
        if (properties.dayTimeReference == null) {
            return false;
        }
        if (getType() == DimensionProperties.PlanetType.PLANET)
            return true;
        return false;
    }

    public DimensionProperties.PlanetType getType() {
        return properties.type;
    }

    public ResourceLocation getDimensionId() {
        return properties.dimensionId;
    }

    public boolean canRain() {
        return getAtmosphereDensity() > 0.5f;
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

    public Vector3f getSkyColor() {
        return new Vector3f(properties.skyColor);
    }

    public Vector3f getSunRiseColor() {
        return new Vector3f(properties.sunRiseColor);
    }

    public Iterable<ResourceLocation> getCurrentMainStars() {
        return planetRenderCache.significantLightSourcesCache.keySet();
    }

    public Iterable<ResourceLocation> getPlanetsToRenderInSky() {
        return DimensionManager.INSTANCE.dimensions.keySet(); // TODO: use cache similar like the light source cache
    }

    public Vector3f getFogColor() {
        return new Vector3f(properties.fogColor);
    }

    public float getAtmosphereDensity() {
        return properties.atmosphereDensity;
    }

    public boolean shouldRenderInSky() {
        return getType() == DimensionProperties.PlanetType.PLANET ||
                getType() == DimensionProperties.PlanetType.STAR;
    }

    public boolean hasCustomSky() {
        return properties.hasCustomSky;
    }

    public int getSeaLevel() {
        return properties.sealevel;
    }

    public float getDayTimePerTick() {
        return (float) Level.TICKS_PER_DAY / properties.targetDayLength;
    }

    public double getRotationAngle(float partialTick) {
        double actualDayTime = properties.dayTime + getDayTimePerTick() * partialTick;
        double rotation = actualDayTime / Level.TICKS_PER_DAY * 360;
        return rotation;
    }

    /**
     * computes the accumulated brightness by relevant stars to be used for terrain shading
     */
    public double getAccumulatedWorldBrightness(float partialTick, float dotOffset, @Nullable Vec3 myPlanetPosition) {
//if(true)return 1;
        if (myPlanetPosition == null) myPlanetPosition = getPosition(partialTick);

        double astronomicalBrightness = 0;
        for (ResourceLocation targetId : getCurrentMainStars()) {
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


    public Vec3 getPosition(float partialTick) {
        if (properties.parentDimensionId != null) {
            Dimension parent = (Dimension) DimensionManager.get(properties.parentDimensionId); // you can only orbit dimensions, not space stations
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

    public float getLatitudeFromZPosition(double z) {
        double s = z / properties.latitude_len;
        float lat = (float) Math.sin(s * Math.PI * 2) * 90;
        return lat;
    }

    public float getLatitude() {
        if (FMLLoader.getDist().isDedicatedServer()) return 0;
        else return clientOnly.getLatitude();
    }

    /**
     * calculates universe global coordinates for the local north east up coordinates of the planet
     */
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        return getGlobalAxisDirections(partialTick, Optional.empty());
    }

    public AxisDirections getGlobalAxisDirections(float partialTick, Optional<Double> z) {
        // 1. Pick the correct perpendicular vector to axis
        Vec3 equatorRef = getEquatorReference(partialTick);

        Vec3 rotationAxis = properties.rotationAxis;

        // 2. Rotate the equatorRef by raw self-rotation
        Vec3 rotatedEquator = CelestialUtils.rotate(equatorRef, rotationAxis, getRotationAngle(partialTick));

        // 3. Get east vector
        Vec3 east = rotationAxis.cross(rotatedEquator).normalize();

        // 4 rotate equator reference around east by latitude
        double lat;
        if (z.isEmpty()) lat = getLatitude();
        else lat = getLatitudeFromZPosition(z.get());
        Vec3 localUp = CelestialUtils.rotate(rotatedEquator, east, lat).normalize();

        // 5 calculate new north
        Vec3 north = localUp.cross(east).normalize();

        return new AxisDirections(north, east, localUp);
    }

    /**
     * returns a reference vector for the equator, orthogonal to the rotation axis and the reference space object for day start
     */
    public Vec3 getEquatorReference(float partialTick) {
        // use main light source as reference for day start
        Dimension dayReference = DimensionManager.get(properties.dayTimeReference);
        Vec3 dayRefToPlanet = getPosition(partialTick).subtract(dayReference.getPosition(partialTick));
        Vec3 equatorReference = dayRefToPlanet.cross(properties.rotationAxis).scale(-1);
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

        if (level != null) {
            if (!canRain()) {
                level.setWeatherParameters(100, 0, false, false);
            }
        }
    }

    public class ClientOnly {
        public float getLatitude() {
            // player uses latitude based on location on planet
            Player p = Minecraft.getInstance().player;
            return getLatitudeFromZPosition(p.position().z);
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