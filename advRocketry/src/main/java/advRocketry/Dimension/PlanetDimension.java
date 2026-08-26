package advRocketry.Dimension;

import advRocketry.Config;
import advRocketry.GlobalTime;
import advRocketry.Registry.GasRegistry;
import advRocketry.Render.SkyRenderer;
import advRocketry.Utils.AxisDirections;
import advRocketry.Utils.CelestialUtils;
import advRocketry.Utils.ClientUtils;
import advRocketry.Utils.RenderUtils;
import advRocketry.Worldgen.BiomeConfig;
import advRocketry.Worldgen.PlanetDimensionGeneration;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;

import static advRocketry.Utils.CelestialUtils.fromAU;
import static advRocketry.Utils.CelestialUtils.fromEarthMasses;

public class PlanetDimension extends Dimension {

    Vec3 currentSpeed = Vec3.ZERO;
    int lastRoundedTemp;
    boolean requiresSync = true;

    public PlanetDimension(DimensionProperties properties, DimensionManager dimensionManager) {
        super(properties, dimensionManager);
        if (FMLEnvironment.dist.isClient()) {
            // i can not do it in this class because it would load abstract texture to server and it crashes
            // by wrapping it in another class it will not trigger server crash unless the class is loaded
            SkyRenderer.ensureMipmapTexture(getTexture());
        }
    }

    public void registerLoadedChunk(ChunkPos pos) {
        loadedChunks.put(pos.toLong(), new ChunkInfo());
    }

    public void setRequiresSync() {
        requiresSync = true;
    }

    public void setRaining(int seconds) {
        // when chunks are currently increasing sea level, it should rain!
        ServerLevel level = level();
        if (level != null)
            level.setWeatherParameters(0, 20 * seconds, true, false);
    }

    public void setClearWeather() {
        // when chunks are currently increasing sea level, it should rain!
        ServerLevel level = level();
        if (level != null)
            level.setWeatherParameters(20 * 1000, 0, false, false);
    }

    public void updateDimensionProperties(DimensionProperties properties) {

        // the daytime can very easily go out of sync, but on client it is only used to rotate the planet correctly
        // it does not matter what the time is on the client, but sync can cause the rotation to jump and it looks bad
        // so ignore the time on sync, just keep original daytime
        if (dimensionManager.isClientSide) {
            ((PlanetDimensionProperties) properties).dayTime = properties().dayTime;
        }

        super.updateDimensionProperties(properties);

        // VERY important, because your position is usually 0 0 0 at start when you orbit another planet
        // now, it can take 2 ticks until all planets have received their position but in tick 0 any planet might query the position of a star.
        // for example temperature wants distance to star, but when all planets are at 0 0 0 first tick, this is 0 and /0 = nan
        // this runs on server when dimensions are reloaded from main config
        if (!dimensionManager.isClientSide) {
            tickPosition(); // first tick sets position
            tickPosition(); // second tick resets movement
        }
    }

    private PlanetDimensionProperties properties() {
        return (PlanetDimensionProperties) properties;
    }

    public void createDimension() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        DynamicDimensionRegistry dynamicDimensionRegistry = DynamicDimensionRegistry.from(server);
        if (!dynamicDimensionRegistry.canCreateDimension(getDimensionId()))
            return;

        System.out.println("creating dimension for " + getDimensionId());

        BlockState seaFluid = Blocks.WATER.defaultBlockState();
        PlanetDimensionProperties.GasProperty water = getGasProperty(GasRegistry.water);
        water.maybeAdjustWorldgenSeaLevel(GasRegistry.water, this); // make it calculate initial sea level
        int seaLevel = water.worldGenSeaLevel;

        if (properties().customSeaFluid != null) {
            seaFluid = BuiltInRegistries.BLOCK.get(properties().customSeaFluid).defaultBlockState();
            seaLevel = properties().customSeaFluidLevel;
        }

        ChunkGenerator generator = PlanetDimensionGeneration.makeChunkGenerator(
                Blocks.STONE.defaultBlockState(), // TODO: make this a property
                seaFluid,
                seaLevel + 1, // the sea level in the world generator is actually the block above the sea level
                BiomeConfig.loadPreset(properties().biomePreset),
                properties().generateStructures
        );

        OptionalLong fixedTime = properties().targetDayLength <= 0 ? OptionalLong.of(-properties().targetDayLength) : OptionalLong.empty();

        DimensionType type = PlanetDimensionGeneration.makePlanetDimensionType(fixedTime);
        ServerLevel l = dynamicDimensionRegistry.loadDynamicDimension(getDimensionId(), generator, type);
        if (l == null) {
            l = dynamicDimensionRegistry.createDynamicDimension(
                    getDimensionId(),
                    generator,
                    type
            );
            System.out.println("created dimension for " + getDimensionId());
        } else {
            System.out.println("loaded dimension for " + getDimensionId());
        }
    }

    // TODO:
    //  on random tick, choose new target sky and fog colors and slowly interpolate between them to make diverse sky effects
    //  maybe adjust colors +-up to 10% of the original color channel value?

    public boolean canVisit() {
        if (properties().dayTimeReference == null) {
            return false;
        }
        if (!properties().canVisit) {
            return false;
        }
        if (isStar())
            return false;

        return true;
    }

    public Set<SurvivalProblem> getSurvivalProblems() {
        Set<SurvivalProblem> problems = new HashSet<>();
        double pressure = getAtmosphereDensity();
        if (pressure < 0.7)
            problems.add(SurvivalProblem.TOO_LOW_PRESSURE);

        if (pressure > 1.6)
            problems.add(SurvivalProblem.TOO_MUCH_PRESSURE);

        double oxygen = getGasProperty(GasRegistry.oxygen).in_atm;
        if (oxygen < 0.15 * pressure || pressure == 0)
            problems.add(SurvivalProblem.TOO_LITTLE_O2);
        if (oxygen > 0.7 * pressure)
            problems.add(SurvivalProblem.TOO_MUCH_O2);

        double co2 = getGasProperty(GasRegistry.co2).in_atm;
        if (co2 > 0.03 * pressure)
            problems.add(SurvivalProblem.TOO_MUCH_CO2);

        if (getCurrentTemp() < 273 - 50)
            problems.add(SurvivalProblem.TOO_COLD);
        if (getCurrentTemp() > 273 + 50)
            problems.add(SurvivalProblem.TOO_HOT);

        return problems;
    }

    public boolean hasEnoughOxygenToBurn() {
        return getGasProperty(GasRegistry.oxygen).in_atm > 0.1;
    }

    public Vector3f getEmissiveColor() {
        return properties().emissiveLightColor;
    }

    public float getGravitationalMultiplier() {
        return properties().gravitationalMultiplier;
    }

    public Vector3f getSkyColor() {
        return new Vector3f(properties().skyColor);
    }

    public float getSkyDarken() {
        return properties().skyDarken;
    }

    public Vector3f getSunRiseColor() {
        return new Vector3f(properties().sunRiseColor);
    }

    public Vector3f getFogColor() {
        return new Vector3f(properties().fogColor);
    }

    public float getRadiationIntensity() {
        return properties().radiationIntensity;
    }

    public boolean hasCustomSky() {
        return properties().hasCustomSky;
    }

    public double getCurrentTemp() {
        return properties().currentTemp;
    }

    public float computeCloudValue() {
        if (properties().cloudValueOverwrite >= 0)
            return Math.clamp(properties().cloudValueOverwrite, 0, 1);

        float totalCloud = 0;

        // 1. Get the density (0.0 for vacuum, 1.0 for Earth-like)
        double atmosphereFactor = getAtmosphereDensity();

        // 2. If there's no air, there are no clouds. Period.
        if (atmosphereFactor < 0.01) return 0;

        for (String gas : GasRegistry.gases.keySet()) {
            double maxCapacity = calculateVaporCapacity(getCurrentTemp(), gas);
            double overSaturation = getHumidity(gas) - maxCapacity * 0.7;

            if (overSaturation > 0) {
                // 3. Scale the clouds by how much atmosphere there is to hold them
                totalCloud += (float) (overSaturation * 3 * atmosphereFactor);
            }
        }

        return Math.min(1.0f, totalCloud);
    }

    public double computeTerrainBrightness(float partialTick) {
        double brightness = getAccumulatedStarIntensity(partialTick, 0.1f, 0.8f, null);
        brightness = Math.clamp(brightness, 0, 1); // renderer mixin does *0.8+0.2
        return brightness;
    }

    public Vector3f computeRawCloudColor() {
        // TODO return properties cloud color if not null, else compute based on atm composition
        return new Vector3f(properties().cloudColor);
    }

    public Vector3f computeTerrainCloudColor(float partialTick) {
        double brightness = getAccumulatedStarIntensity(partialTick, 0.4f, 0.8f, null);
        brightness = Math.clamp(brightness, 0.2, 1);
        return computeRawCloudColor().mul((float) brightness);
    }

    // color is linear hdr, needs tone mapping and gamma correction
    public Vector3f computeTerrainFogColor(float partialTick) {
        // fog color calculation should match the sky color calculation in the atm shader
        double brightness = getAccumulatedStarIntensity(partialTick, 0.2f, 1f, null);
        double atmDensity = getAtmosphereDensity();
        double extinction = Math.exp(-atmDensity);
        return RenderUtils.gamma_reverse(properties().fogColor)
                .mul((float) brightness)
                .mul((float) atmDensity)
                .mul((float) (extinction));
    }

    public float getAtmosphereDensity() {
        float sum = 0;
        for (PlanetDimensionProperties.GasProperty gas : properties().atmosphereComposition.values()) {
            sum += gas.in_atm;
        }
        return sum;
    }

    public Vec3 getMovement() {
        return currentSpeed;
    }

    public Vec3 getPosition(float partialTick) {
        return properties().position.subtract(getMovement().scale(1 - partialTick));
    }

    public AxisDirections getGlobalAxisDirections(float partialTick) {
        // this should never be called on server !!!
        return getGlobalAxisDirections(partialTick, getLatitudeFromZPosition(ClientUtils.getSinglePlayer().position().z));
    }

    public Vector3f getReflectiveTextureTintColor() {
        return new Vector3f(properties().reflectiveTextureTintColor);
    }

    public Vector3f getEmissiveTextureTintColor() {
        return new Vector3f(properties().emissiveTextureTintColor);
    }

    public boolean hasRings() {
        return properties().hasRingSystem;
    }

    public boolean isStar() {
        return getRadiationIntensity() > 0;
    }

    public ResourceLocation getTexture() {
        return properties().texture;
    }

    public Vec3 getRotationAxis() {
        return properties().rotationAxis;
    }

    public float getEarthRadiusMultiplier() {
        return properties().earthRadiusMultiplier;
    }

    public boolean isKnown() {
        return properties().isKnown;
    }

    public String getDescription() {
        return properties().description;
    }

    public ResourceLocation getParentDimensionId() {
        return properties().parentDimensionId;
    }

    public float getOrbitalDistanceToParent() {
        return properties().orbitalDistanceToParent;
    }

    public float getOrbitalBaseOffsetDegrees() {
        return properties().orbitalBaseOffsetDegrees;
    }

    public int getDataRequiredForUnlock() {
        return properties().dataRequiredForUnlock;
    }

    public Vec3 getOrbitAxis() {
        return new Vec3(properties().orbitAxis.x, properties().orbitAxis.y, properties().orbitAxis.z);
    }

    public PlanetDimensionProperties.GasProperty getGasProperty(String id) {
        if (!properties().atmosphereComposition.containsKey(id))
            properties().atmosphereComposition.put(id, new PlanetDimensionProperties.GasProperty(0, 0, 0, 0));
        return properties().atmosphereComposition.get(id);
    }

    public Set<String> getGasMiningOptions() {
        HashSet<String> set = new HashSet<>();
        for (String gas : GasRegistry.gases.keySet()) {
            if (getGasProperty(gas).in_atm > 2)
                set.add(gas);
        }
        return set;
    }

    public HashMap<String, Double> getLaserDrillOres() {
        return properties().laserOres;
    }

    public float getFrozenGasCoverage() {
        double sum = 0;
        for (PlanetDimensionProperties.GasProperty gas : properties().atmosphereComposition.values()) {
            // when all liquid -> ß
            // when all frozen -> frozen_surface
            // in between: ice will initially accumulate at poles with very little coverage
            // this also works against the albedo snowball effect because initial ice has very little influence on albedo
            sum += gas.frozen_surface * gas.frozen_surface / (gas.liquid + gas.frozen_surface + 0.0001);
        }
        return Math.min(1, (float) sum);
    }

    // how much of the planet do we consider ocean?
    // if gas is null, it will take the max from all gases available
    public double getOceanFraction(@Nullable String gasId) {

        double maxSeaLevel = 0;
        if (gasId != null)
            maxSeaLevel = getGasProperty(gasId).getSeaLevel();
        else {
            for (String gas : GasRegistry.gases.keySet()) {
                maxSeaLevel = Math.max(maxSeaLevel, getGasProperty(gas).getSeaLevel());
            }
        }

        double relativeSeaLevel = maxSeaLevel / PlanetDimensionProperties.GasProperty.maxSeaLevel;
        // sea level can be negative because stupid minecraft now has negative height...
        relativeSeaLevel = Math.clamp(relativeSeaLevel, 0, 1);

        // adjust for custom fluid
        if (properties().customSeaFluid != null) {
            double heightAboveCustomFluidLevel = maxSeaLevel - properties().customSeaFluidLevel;
            if (heightAboveCustomFluidLevel >= 0)
                // if sea level is just slightly above custom fluid level (eg 0.2 blocks), ocean fraction is signifiantly reduced
                relativeSeaLevel *= Math.min(1, heightAboveCustomFluidLevel);
            else {
                // custom fluid is above sea level, no oceans
                relativeSeaLevel = 0;
            }
        }
        return relativeSeaLevel;
    }

    public double calculateVaporCapacity(double currentTemp, String gas) {
        // You'll need to make sure your Gas properties include a boiling point!
        double boilingTemp = GasRegistry.gases.get(gas).getBoilingTemp(getAtmosphereDensity());

        // Calculate the difference between current temp and boiling temp
        double tempDifference = currentTemp - boilingTemp;

        // Exponential curve: capacity drops drastically in the cold,
        // but grows massively as you approach/exceed boiling temp.
        double baseCapacity = Math.exp(tempDifference / 100.0);

        // Cap it so we don't get infinite capacity at crazy high temperatures
        return Math.min(baseCapacity, 1);
    }

    public double getSumHumidity() {
        double sum = 0;
        for (String gas : properties().atmosphereComposition.keySet()) {
            sum += getHumidity(gas);
        }
        return sum;
    }

    public double getHumidity(String gas) {
        // 1. How much vapor CAN the air hold at this temperature?
        double maxCapacity = calculateVaporCapacity(getCurrentTemp(), gas);

        // 2. How much surface liquid/ice is exposed to the air to evaporate?
        // An ocean fraction of 1.0 means it easily reaches max capacity.
        // A desert planet (0.01) means very little actually evaporates.
        double surfaceExposure = getOceanFraction(gas);

        // 3. The actual amount of vapor in the air
        // *2 to have over-saturation in the air on high sea level so we can form clouds
        double actualHumidity = maxCapacity * surfaceExposure * 2;

        return actualHumidity;
    }

    public double getRotationAngle(float partialTick) {
        double actualDayTime = properties().dayTime + getDayTimePerTick() * partialTick;
        double rotation = actualDayTime / Level.TICKS_PER_DAY * 360;
        return rotation;
    }

    public float getLatitudeFromZPosition(double z) {
        // 1. Normalize z into a 0.0 to 1.0 range based on the full length
        // Use modulo to handle wrapping if z exceeds the latitude_len
        double s = (z / properties().latitude_len) % 1.0;
        if (s < 0) s += 1.0; // Ensure positive value for negative z

        float latitude;

        // 2. Map the 0-1 range to a linear "up and down" motion
        if (s < 0.25) {
            // Phase 1: 0 to 0.25 -> Maps 0 to 90
            latitude = (float) (s * 4 * 90);
        } else if (s < 0.75) {
            // Phase 2: 0.25 to 0.75 -> Maps 90 down to -90
            latitude = (float) ((0.5 - s) * 4 * 90);
        } else {
            // Phase 3: 0.75 to 1.0 -> Maps -90 back to 0
            latitude = (float) ((s - 1.0) * 4 * 90);
        }

        return latitude;
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
        // dayReference CAN NEVER NE NULL !!!!
        // if a star is deleted for whatever reason it should be replaced with a dummy dimension at the same position
        Dimension dayReference = dimensionManager.get(properties().dayTimeReference);
        Vec3 dayRefToPlanet = getPosition(partialTick).subtract(dayReference.getPosition(partialTick));
        Vec3 equatorReference = dayRefToPlanet.cross(properties().rotationAxis).scale(-1);
        return equatorReference;
    }


    /**
     * Computes the accumulated star intensity by relevant stars adjusted by the surface dot to the targets with a dot offset.
     * For example, clouds should stay bright a little longer while terrain goes dark already, so increase dot offset for clouds
     * Pow factor makes for a more slow start when > 1 or a quick start and flat elevated curve when < 1
     */
    public double getAccumulatedStarIntensity(float partialTick, float dotOffset, float dotPow, @Nullable Vec3 myPlanetPosition) {
        if (myPlanetPosition == null) myPlanetPosition = getPosition(partialTick);
        double totalStarIntensity = 0;
        for (ResourceLocation targetId : getCurrentMainStars()) {
            Dimension target = dimensionManager.get(targetId);
            if (target == null) continue;
            Vec3 targetPosition = target.getPosition(partialTick);
            double distanceToSqr = targetPosition.distanceToSqr(myPlanetPosition);
            double dot = getSurfaceDotToTarget(target, partialTick, myPlanetPosition, targetPosition);
            double dotMultiplier = (dot + dotOffset) / (1 + dotOffset);// offset
            dotMultiplier = Math.max(0, dotMultiplier);// clip
            dotMultiplier = Math.pow(dotMultiplier, dotPow);// pow
            double intensity = dotMultiplier * target.getRadiationIntensity() / distanceToSqr;
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
        if (properties().targetDayLength <= 0) return 0;
        return (float) Level.TICKS_PER_DAY / properties().targetDayLength;
    }

    public void trackDayTimeNormal() {
        if (properties().targetDayLength > 0) {
            properties().dayTime += getDayTimePerTick();
            properties().dayTime = properties().dayTime % Level.TICKS_PER_DAY;
        } else {
            properties().dayTime = -properties().targetDayLength;
        }
    }

    public void tick() {
        super.tick();

        tickPosition();

        if (!isClientSide) {

            if (GlobalTime.getGlobalTime() % 20 == 0 && requiresSync) {
                requiresSync = false;
                dimensionManager.syncDimensionProperties(this);
            }

            ServerLevel level = DimensionManager.getServerLevel(getDimensionId());
            if (level != null) {
                if (properties().targetDayLength > 0) { // time runs normal, when <= 0 it is fixed time
                    level.setDayTimePerTick(getDayTimePerTick());
                    properties().dayTime = level.dayTime();
                } else
                    properties().dayTime = -properties().targetDayLength;
            } else {
                trackDayTimeNormal();
            }

            if (level != null) {
                if (computeCloudValue() < 0.1) {
                    // can not rain / snow without clouds
                    setClearWeather();
                }
            }


            tickTemperature();

            tickGasProperties();

            if (level != null) {
                PlanetEvents.tick(this, properties(), level);
            }
        }

        if (isClientSide) {
            Dimension myDimension = ClientUtils.getPlayerDimension();
            if (myDimension != null && myDimension.getDimensionId().equals(this.getDimensionId())) {
                if (properties().targetDayLength > 0)
                    properties().dayTime = ClientUtils.getPlayerLevel().dayTime();
                else
                    properties().dayTime = -properties().targetDayLength;
            } else {
                trackDayTimeNormal();
            }
        }
    }

    public void tickChunk(ChunkPos pos) {
        super.tickChunk(pos);
        int ticksToRun = 0;

        ChunkInfo info = (ChunkInfo) loadedChunks.get(pos.toLong());
        if (!info.completedFirstTick) {
            // maybe transform new chunks
            // all neighbors need to be generated or it causes LAAAAAG
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (!level().hasChunk(pos.x + x, pos.z + z)) {
                        return;
                    }
                }
            }
            info.completedFirstTick = true;
            tasks.add(() -> {
                if (!shouldTickChunk(pos))
                    return;
                PlanetEvents.runInitialTerraformingTasks(this, level(), pos.x, pos.z);
            });
            ticksToRun = 16 * 16; // on first tick, queue entire chunk for scanning
        }

        // terraforming is rare and rarely needs checking
        // if only 1 chunk finds work it will notify neighbors and itself to queue more updates
        // it should be more efficient to process multiple positions in the same chunk over randomly in random chunks
        if (info.openTasks != 0)
            return; // do not queue more tasks while the chunk has still uncompleted tasks in queue
        if (Math.random() < 0.005)
            ticksToRun = Math.max(1, ticksToRun);     // sometimes check for work
        if (info.hasWorkCurrently) {
            ticksToRun = Math.max(16 * 16, ticksToRun); // queue the entire chunk
        }
        info.hasWorkCurrently = false; // reset it and wait for tick to switch it on again

        for (int i = 0; i < ticksToRun; i++) {
            info.openTasks++;
            tasks.add(() -> {
                info.tickIndex++;
                info.openTasks--;
                if(!shouldTickChunk(pos))
                    return;
                if (PlanetEvents.maybePerformTerraformingTicks(this, level(), pos, info.tickIndex)) {
                    // if this tick had work, notify this chunk and the neighbors that there is work to do
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            long neighborPos = new ChunkPos(pos.x + x, pos.z + z).toLong();
                            if (loadedChunks.containsKey(neighborPos)) {
                                ((ChunkInfo) loadedChunks.get(neighborPos)).hasWorkCurrently = true;
                            }
                        }
                    }
                }
            });
        }
    }

    // by ticking the position once and interpolating between last and current position it will
    // reduce computation all the time we require the dimension. the movement will still be smooth, just not a perfect circle
    public void tickPosition() {
        Vec3 lastPosition = properties().position;

        if (properties().parentDimensionId != null) {
            Dimension parent = dimensionManager.get(properties().parentDimensionId);
            if (parent != null) {
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
                properties().position = parent.getPosition(0).add(rotatedOffset);
            }
        }

        currentSpeed = properties().position.subtract(lastPosition);
    }

    public void tickTemperature() {

        int roundedTemp = (int) (properties().currentTemp * 100);

        if (lastRoundedTemp != roundedTemp) {
            setRequiresSync();
            lastRoundedTemp = roundedTemp;
        }

        if (isStar()) {
            properties().currentTemp = getRadiationIntensity() * 3000;
            return;
        }

        // Current state
        double currentTemp = getCurrentTemp();

        // --- UNIVERSAL GAME CONSTANTS ---
        // This is the Stefan-Boltzmann constant scaled for the game's energy units.
        // It determines how aggressively planets try to radiate heat away.
        final double EMISSION_CONSTANT = 0.0000000004;

        // 1. CALCULATE INCOMING ENERGY (Ein)
        double solarFlux = 0.0;
        Vec3 planetPos = getPosition(0);
        for (ResourceLocation starId : getCurrentMainStars()) {
            if (dimensionManager.get(starId) instanceof PlanetDimension star) {
                Vec3 starPos = star.getPosition(0);
                double distanceAU = starPos.distanceTo(planetPos);
                solarFlux += star.getRadiationIntensity() / (distanceAU * distanceAU + 0.00001);
            }
        }

        // Albedo (Reflectivity, not perfect but close enough)
        double oceanFraction = getOceanFraction(null); // (uses highest sea level of all gases, includes solid + liquid)
        double frozenFraction = getFrozenGasCoverage();
        // base albedo
        double albedo = 0.3;
        // ice reflects light
        albedo += (frozenFraction * 0.5);
        // oceans are dark ( usually, unless frozen, but getFrozenGasCoverage offsets this)
        albedo -= (oceanFraction * 0.15);
        // clouds reflect light
        albedo += Math.clamp(computeCloudValue(), 0, 1) * 0.5;
        // final value clip
        albedo = Math.max(0.05, Math.min(albedo, 0.95));

        // The actual energy absorbed by the planet
        double energyIn = solarFlux * (1.0 - albedo) + properties().baseEnergyGain;

        // 2. CALCULATE INSULATION (Greenhouse Blanket)

        // Base insulation is 1.0 (a vacuum). Higher numbers mean heat struggles to escape.
        double totalRawGreenhouse = 0.0;
        for (String id : List.of(GasRegistry.co2, GasRegistry.methane, GasRegistry.water)) {
            double vapor_atm_estimate = getHumidity(id) / 300.0;
            double total_atm = getGasProperty(id).in_atm + vapor_atm_estimate;
            totalRawGreenhouse += GasRegistry.getRawGreenhouseValue(id, total_atm);
        }

        double insulation = 1.0 + Math.log1p(totalRawGreenhouse);


        // 3. CALCULATE OUTGOING ENERGY (Eout)
        // Stefan-Boltzmann Law: planets radiate heat proportional to T^4.
        // The insulation divides the outgoing energy, trapping it.
        double energyOut = (EMISSION_CONSTANT * Math.pow(currentTemp, 4)) / insulation;

        // 4. CALCULATE THERMAL MASS (Inertia)
        // Water and thick atmospheres resist temperature changes.
        // This stops the temperature from dropping instantly if a player drains an ocean.
        double thermalMass = 1.0 + (oceanFraction * 10) + (getGravitationalMultiplier() * 100);
        thermalMass *= Config.INSTANCE.planet_Heat_Capacity_Multiplier;
        //thermalMass = 0.1; // TODO: remove after testing

        // 5. APPLY DELTA (The simulation step)
        // If Ein > Eout, the planet warms. If Eout > Ein, it cools.
        double deltaTemp = (energyIn - energyOut) / thermalMass;

        // Apply the change to the planet
        properties().currentTemp += deltaTemp;
    }

    public void tickGasProperties() {
        double temp = getCurrentTemp();
        double atmDensity = getAtmosphereDensity();

        for (String gasId : GasRegistry.gases.keySet()) {
            PlanetDimensionProperties.GasProperty property = getGasProperty(gasId);
            GasRegistry.Gas gas = GasRegistry.gases.get(gasId);

            property.tick(gas, this, temp, atmDensity);
        }
    }

    public static class ChunkInfo extends Dimension.ChunkInfo {
        boolean completedFirstTick = false;
        boolean hasWorkCurrently = false;
        int openTasks = 0;
        long tickIndex = 0;
    }
}