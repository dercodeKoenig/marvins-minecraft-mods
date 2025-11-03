package advRocketry.Dimension;

import advRocketry.utils.AxisDirections;
import advRocketry.utils.CelestialUtils;
import advRocketry.utils.ClientUtils;
import advRocketry.worldgen.BiomeConfig;
import advRocketry.worldgen.PlanetDimensionGeneration;
import advRocketry.worldgen.presets.HOT_DRY;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.OptionalLong;

import static advRocketry.utils.CelestialUtils.fromAU;
import static advRocketry.utils.CelestialUtils.fromEarthMasses;

public class PlanetDimension extends Dimension {

    float targetsealevel; // for ticking, rise or lower sea level
    float temperature; // should be autocalculated, will for example freeze water when cold or evaporate when too hot or superheated
    // all the gases need to be added too, gascomposition
    // maybe gases underground trapped / frozen that can be freed

    public PlanetDimension(PlanetDimensionProperties properties) {
        super(properties);
    }

    private PlanetDimensionProperties properties() {
        return (PlanetDimensionProperties) properties;
    }

    public void createDimension() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        System.out.println("creating dimension for " + getDimensionId());
        DynamicDimensionRegistry dynamicDimensionRegistry = DynamicDimensionRegistry.from(server);

        long seed = (server.overworld().getSeed() + (long) getDimensionId().hashCode());

        ChunkGenerator generator = PlanetDimensionGeneration.makeChunkGenerator(
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                getSeaLevel(),
                BiomeConfig.loadPreset(HOT_DRY.name),
                seed,
                properties().generateStructures
        );

        OptionalLong fixedTime = properties().targetDayLength <= 0 ? OptionalLong.of(-properties().targetDayLength) : OptionalLong.empty();
        DimensionType type = PlanetDimensionGeneration.makePlanetDimensionType(fixedTime);
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

    public boolean isKnown() {
        return properties().isKnown;
        // TODO: maybe make an item that stores the discovered planets per item and only with item you can select a hidden planet to travel to
        //      telescope can discover short distance planets fast (moons for example)
    }

    public boolean canVisit() {
        if (properties().dayTimeReference == null) {
            return false;
        }
        if(properties().radiationIntensity > 0){
            return false;
        }
        return true;
    }


    public boolean canRain() {
        return getAtmosphereDensity() > 0.5f;
    }

    public Vector3f getEmissiveColor() {
        return properties().emissiveColor;
    }

    public Vec3 getRotationAxis() {
        return properties().rotationAxis;
    }

    public float getEarthRadiusMultiplier() {
        return properties().earthRadiusMultiplier;
    }

    public float getGravitationalMultiplier() {
        return properties().gravitationalMultiplier;
    }

    public ResourceLocation getTexture() {
        return properties().texture;
    }

    public Vector3f getSkyColor() {
        return new Vector3f(properties().skyColor);
    }

    public Vector3f getSunRiseColor() {
        return new Vector3f(properties().sunRiseColor);
    }

    public Vector3f getFogColor() {
        return new Vector3f(properties().fogColor);
    }

    public float getAtmosphereDensity() {
        return properties().atmosphereDensity;
    }

    public float getRadiationIntensity() {
        return properties().radiationIntensity;
    }

    public boolean hasCustomSky() {
        return properties().hasCustomSky;
    }

    @Override
    public double getTerrainBrightness() {
        return 0;
    }

    @Override
    public Vector3f getCloudColor() {
        return null;
    }

    @Override
    public Vector3f computeTerrainFogColor() {
        return null;
    }

    public int getSeaLevel() {
        return properties().seaLevel;
    }

    public double getRotationAngle(float partialTick) {
        double actualDayTime = properties().dayTime + getDayTimePerTick() * partialTick;
        double rotation = actualDayTime / Level.TICKS_PER_DAY * 360;
        return rotation;
    }

    public Vec3 getPosition(float partialTick) {
        if (properties().parentDimensionId != null) {
            PlanetDimension parent = (PlanetDimension) DimensionManager.get(properties().parentDimensionId);
            double ticksPerOrbit = CelestialUtils.calculateOrbitalPeriodTicks(fromEarthMasses(getGravitationalMultiplier()), fromEarthMasses(parent.getGravitationalMultiplier()), fromAU(properties().orbitalDistanceToParent));
            double orbitalProgress = (GlobalTime.getGlobalTime() % ticksPerOrbit) + (GlobalTime.getGlobalTimeClientCorrection() % ticksPerOrbit);
            double orbitAngleDegrees = orbitalProgress * (360.0 / ticksPerOrbit) + properties().orbitalBaseOffsetDegrees;

            // 1. Define a simple, non-zero vector to use for the cross-product
            // This is an arbitrary direction, often chosen to align with a major axis.
            Vec3 arbitraryVector = new Vec3(0, 0, 1); // e.g., the Z-axis

            // 2. Find a starting vector orthogonal to the orbitAxis
            Vec3 startDirection = properties().orbitAxis.cross(arbitraryVector);

            // 3. Handle the edge case where orbitAxis is parallel to arbitraryVector (e.g., orbitAxis is <0,0,1>)
            // If the cross-product is zero length, orbitAxis and arbitraryVector are parallel.
            if (startDirection.length() < 0.0001d) {
                // Fallback: cross with a different axis (e.g., the X-axis)
                arbitraryVector = new Vec3(1, 0, 0);
                startDirection = properties().orbitAxis.cross(arbitraryVector);
            }

            // 4. Normalize the orthogonal vector and scale it to the orbital distance
            // This is your correct 'baseOffset' vector, originating at the parent and orthogonal to the rotation axis.
            Vec3 baseOffset = startDirection.normalize().scale(properties().orbitalDistanceToParent);

            // 5. Rotate the baseOffset around the orbitAxis by the current angle
            // baseOffset is now the vector V_start, and orbitAxis is the vector A.
            Vec3 rotatedOffset = CelestialUtils.rotate(baseOffset, properties().orbitAxis, orbitAngleDegrees);

            // 6. Add parent's position to get global position
            properties().position = parent.getPosition(partialTick).add(rotatedOffset);
        }
        return properties().position;
    }

    public float getLatitudeFromZPosition(double z) {
        double s = z / properties().latitude_len;
        return (float) Math.sin(s * Math.PI * 2) * 90;
    }

    public AxisDirections getGlobalAxisDirections(float partialTick) {
        // this should never be called on server !!!
        return getGlobalAxisDirections(partialTick, getLatitudeFromZPosition(ClientUtils.getSinglePlayer().position().z));
    }

    public AxisDirections getGlobalAxisDirections(float partialTick, double latitude) {
        // 1. Pick the correct perpendicular vector to axis
        Vec3 equatorRef = getEquatorReference(partialTick);

        Vec3 rotationAxis = properties().rotationAxis;

        // 2. Rotate the equatorRef by raw self-rotation
        Vec3 rotatedEquator = CelestialUtils.rotate(equatorRef, rotationAxis, getRotationAngle(partialTick));

        // 3. Get east vector
        Vec3 east = rotationAxis.cross(rotatedEquator).normalize();

        // 4 rotate equator reference around east by latitude
        Vec3 localUp = CelestialUtils.rotate(rotatedEquator, east, latitude).normalize();

        // 5 calculate new north
        Vec3 north = localUp.cross(east).normalize();

        return new AxisDirections(north, localUp);
    }

    /**
     * returns a reference vector for the equator, orthogonal to the rotation axis and the reference space object for day start
     */
    public Vec3 getEquatorReference(float partialTick) {
        // use main light source as reference for day start
        Dimension dayReference = DimensionManager.get(properties().dayTimeReference);
        Vec3 dayRefToPlanet = getPosition(partialTick).subtract(dayReference.getPosition(partialTick));
        Vec3 equatorReference = dayRefToPlanet.cross(properties().rotationAxis).scale(-1);
        return equatorReference;
    }


    /**
     * Computes the accumulated star intensity by relevant stars adjusted by the surface dot to the targets with a dot offset.
     * For example, clouds should stay bright a little longer while terrain goes dark already, so increase dot offset for clouds
     */
    public double getAccumulatedStarIntensity(float partialTick, float dotOffset, @Nullable Vec3 myPlanetPosition) {
        if (myPlanetPosition == null) myPlanetPosition = getPosition(partialTick);
        double totalStarIntensity = 0;
        for (ResourceLocation targetId : getCurrentMainStars()) {
            Dimension target = DimensionManager.get(targetId);
            Vec3 targetPosition = target.getPosition(partialTick);
            double distance = targetPosition.distanceTo(myPlanetPosition);
            double dotMultiplier = Math.max(0, (getSurfaceDotToTarget(target, partialTick, myPlanetPosition, targetPosition) + dotOffset) / (1 + dotOffset));
            double intensity = dotMultiplier * target.getRadiationIntensity() / (distance * distance);
            totalStarIntensity += intensity;
        }
        return totalStarIntensity;
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


    public float getDayTimePerTick() {
        if (properties().targetDayLength <= 0) {
            return 0;
        }
        return (float) Level.TICKS_PER_DAY / properties().targetDayLength;
    }

    public void trackDayTimeNormal() {
        properties().dayTime += getDayTimePerTick();
        properties().dayTime = properties().dayTime % Level.TICKS_PER_DAY;
    }

    public void tick() {
        super.tickStarCache();
    }

    public void serverTick(ServerTickEvent event) {
        tick();

        ServerLevel level = DimensionManager.getServerLevel(event.getServer(), getDimensionId());
        if (level != null) {
            if (properties().targetDayLength > 0) { // time runs normal, when <= 0 it is fixed time
                level.setDayTimePerTick(getDayTimePerTick());
            }
            properties().dayTime = level.getDayTime();
        } else {
            trackDayTimeNormal();
        }

        if (level != null) {
            if (!canRain()) {
                level.setWeatherParameters(100, 0, false, false);
            } else {
                //level.setWeatherParameters(0, 100, true, false);
                // TODO: custom weather logic
            }
        }
    }


    public void clientTick() {
        tick();
        Level level = ClientUtils.getPlayerLevel();
        if (level != null && getDimensionId() != null && getDimensionId().equals(level.dimension().location())) {
            properties().dayTime = level.getDayTime();
        } else {
            trackDayTimeNormal();
        }
    }
}