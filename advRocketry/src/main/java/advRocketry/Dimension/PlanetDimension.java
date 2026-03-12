package advRocketry.Dimension;

import advRocketry.GlobalTime;
import advRocketry.Utils.AxisDirections;
import advRocketry.Utils.CelestialUtils;
import advRocketry.Utils.ClientUtils;
import advRocketry.Worldgen.BiomeConfig;
import advRocketry.Worldgen.PlanetDimensionGeneration;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.OptionalLong;
import java.util.Set;

import static advRocketry.Utils.CelestialUtils.fromAU;
import static advRocketry.Utils.CelestialUtils.fromEarthMasses;

public class PlanetDimension extends Dimension {

    public Vec3 currentSpeed = new Vec3(0, 0, 0);
    public Vec3 lastPosition = new Vec3(0, 0, 0);

    public PlanetDimension(PlanetDimensionProperties properties, DimensionManager dimensionManager) {
        super(properties, dimensionManager);
        lastPosition = properties().position;
        currentSpeed = Vec3.ZERO;
    }

    public void updateDimensionProperties(DimensionProperties properties) {
        super.updateDimensionProperties(properties);
    }

    PlanetDimensionProperties properties() {
        return (PlanetDimensionProperties) properties;
    }

    public void createDimension() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        DynamicDimensionRegistry dynamicDimensionRegistry = DynamicDimensionRegistry.from(server);
        if (!dynamicDimensionRegistry.canCreateDimension(getDimensionId()))
            return;

        System.out.println("creating dimension for " + getDimensionId());

        ChunkGenerator generator = PlanetDimensionGeneration.makeChunkGenerator(
                Blocks.STONE.defaultBlockState(), // TODO: make this a property
                Blocks.WATER.defaultBlockState(),
                getSeaLevel(),
                BiomeConfig.loadPreset(properties().biomePreset),
                properties().generateStructures
        );

        OptionalLong fixedTime = properties().targetDayLength <= 0 ? OptionalLong.of(-properties().targetDayLength) : OptionalLong.empty();

        DimensionType type = PlanetDimensionGeneration.makePlanetDimensionType(fixedTime);
        ServerLevel l = dynamicDimensionRegistry.loadDynamicDimension(getDimensionId(), generator, type);
        if (l == null) {
            dynamicDimensionRegistry.createDynamicDimension(
                    getDimensionId(),
                    generator,
                    type
            );
            System.out.println("created dimension for " + getDimensionId());
        } else {
            System.out.println("loaded dimension for " + getDimensionId());
        }
        // maybe for terraforming to change biomes:
        // or try to set from a noise source? using the same biome source and level noise source?
        // ((PalettedContainer) l.getChunk(0,0).getSection(0).getBiomes()).get;

        // maybe get base height from this to copy top blocks without decoration?
        //NoiseColumn nc = l.getChunkSource().getGenerator().getBaseColumn(0,0,l,l.getChunkSource().randomState());


        // make a table of templates of hot -> cold, high sea level -> low sea level
        // terraformer will choose a template and create a virtual level to generate the new world and copy it

        // maybe when biome changing, pick flower features from biome?
    }

    // TODO:
    //  on random tick, choose new target sky and fog colors and slowly interpolate between them to make diverse sky effects
    //  maybe adjust colors +-up to 10% of the original color channel value?

    public boolean isKnown() {
        return properties().isKnown;
    }

    public boolean canVisit() {
        if (properties().dayTimeReference == null) {
            return false;
        }
        if (!properties().canVisit) {
            return false;
        }

        return true;
    }

    @Override
    public boolean hasEnoughOxygen() {
        return true; // TODO: improve this
    }

    public boolean canRain() {
        return getAtmosphereDensity() > 0.5f && getCurrentTemp() < 373 && getCurrentTemp() > 273;
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

    public Vector3f getReflectiveTextureTintColor() {
        return new Vector3f(properties().reflectiveTextureTintColor);
    }

    public boolean hasRings() {
        return properties().hasRingSystem;
    }

    public float getRadiationIntensity() {
        return properties().radiationIntensity;
    }

    public boolean hasCustomSky() {
        return properties().hasCustomSky;
    }

    public ResourceLocation getParentDimensionId() {
        return properties().parentDimensionId;
    }

    public float getorbitalDistanceToParent() {
        return properties().orbitalDistanceToParent;
    }

    public float getorbitalBaseOffsetDegrees() {
        return properties().orbitalBaseOffsetDegrees;
    }

    public int getDataRequiredForUnlock() {
        return properties().dataRequiredForUnlock;
    }

    public Vec3 getOrbitAxis() {
        return new Vec3(properties().orbitAxis.x, properties().orbitAxis.y, properties().orbitAxis.z);
    }

    public double getCurrentTemp() {
        return properties().currentTemp;
    }

    public PlanetDimensionProperties.GasProperty getGasProperty(String id) {
        if (!properties().atmosphereComposition.containsKey(id))
            properties().atmosphereComposition.put(id, new PlanetDimensionProperties.GasProperty(0, 0));
        return properties().atmosphereComposition.get(id);
    }

    public float getAtmosphereDensity() {
        float sum = 0;
        for (PlanetDimensionProperties.GasProperty gas : properties().atmosphereComposition.values()) {
            sum += gas.in_atm;
        }
        return sum;
    }

    public float getFrozenGasCoverage() {
        float sum = 0;
        for (PlanetDimensionProperties.GasProperty gas : properties().atmosphereComposition.values()) {
            sum += gas.frozen_surface;
        }
        if (getCurrentTemp() < 273) {
            // water is frozen and contributes
            sum += (float) Math.min(getSeaLevel() / 120.0, 0.8);
        }
        return Math.min(1, sum);
    }

    @Override
    public double getTerrainBrightness(float partialTick) {
        double brightness = getAccumulatedStarIntensity(partialTick, 0.2f, null);
        brightness = Math.clamp(Math.pow(brightness, 0.8), 0, 1);
        return brightness;
    }

    @Override
    public Vector3f getCloudColor(float partialTick) {
        double brightness = getAccumulatedStarIntensity(partialTick, 0.4f, null);
        brightness = Math.clamp(Math.pow(brightness, 0.8), 0.2, 1);
        return new Vector3f(properties().cloudColor).mul((float) brightness);
    }

    @Override
    public Vector3f computeTerrainFogColor(float partialTick) {
        double brightness = getAccumulatedStarIntensity(partialTick, 0.4f, null);
        brightness = Math.clamp(Math.pow(brightness, 0.8), 0, 1);
        return new Vector3f(properties().fogColor)
                .mul((float) brightness)
                .mul(getAtmosphereDensity() / (1 + getAtmosphereDensity()));
    }

    public int getSeaLevel() {
        return properties().seaLevel;
    }

    public double getOceanFraction() {
        double oceanFraction = Math.min(getSeaLevel() / 100.0, 1);
        return oceanFraction;
    }

    public double getHumidity() {
        if (getCurrentTemp() > 273.15) {
            double humidity = Math.pow(1.02, (getCurrentTemp() - 273.15)) * getOceanFraction() * 0.25;
            return humidity;
        } else {
            return 0;
        }
    }

    public double getRotationAngle(float partialTick) {
        double actualDayTime = properties().dayTime + getDayTimePerTick() * partialTick;
        double rotation = actualDayTime / Level.TICKS_PER_DAY * 360;
        return rotation;
    }

    public Vec3 getMovement() {
        return currentSpeed;
    }

    public Vec3 getPosition(float partialTick) {
        if (properties().parentDimensionId != null) {
            Dimension parent = dimensionManager.get(properties().parentDimensionId);
            if (parent == null) return properties().position;

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
     */
    public double getAccumulatedStarIntensity(float partialTick, float dotOffset, @Nullable Vec3 myPlanetPosition) {
        if (myPlanetPosition == null) myPlanetPosition = getPosition(partialTick);
        double totalStarIntensity = 0;
        for (ResourceLocation targetId : getCurrentMainStars()) {
            Dimension target = dimensionManager.get(targetId);
            if (target == null) continue;
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
        super.tickStarCache();

        Vec3 position = getPosition(0);
        currentSpeed = position.subtract(lastPosition);
        lastPosition = position;

        if (!isClientSide) {
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
                if (!canRain()) {
                    level.setWeatherParameters(100, 0, false, false);
                } else {
                    //level.setWeatherParameters(0, 100, true, false);
                    // TODO: custom weather logic
                }
            }

            if (level != null) {

                if (getDimensionId().equals(ResourceLocation.parse("minecraft:overworld")) ||
                        getDimensionId().equals(ResourceLocation.parse("adv_rocketry:venus"))
                )
                    // only tick temperature for planets we actually visit. for stars the logic does not work anyway
                    if (GlobalTime.getGlobalTime() % 1 == 0)
                        tickTemperature();

                tickTemperatureEvents();
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


    public void tickTemperature() {
        if (GlobalTime.getGlobalTime() % 100 == 0)
            System.out.println("\ntick " + getName() + ": " + getCurrentTemp());
        // Current state
        double currentTemp = getCurrentTemp();

        // --- UNIVERSAL GAME CONSTANTS ---
        // This is the Stefan-Boltzmann constant scaled for the game's energy units.
        // It determines how aggressively planets try to radiate heat away.
        final double EMISSION_CONSTANT = 0.0000000003;

        // 1. CALCULATE INCOMING ENERGY (Ein)
        double solarFlux = 0.0;
        Vec3 planetPos = getPosition(0);
        for (ResourceLocation starId : getCurrentMainStars()) {
            if (dimensionManager.get(starId) instanceof PlanetDimension star) {
                Vec3 starPos = star.getPosition(0);
                double distanceAU = starPos.distanceTo(planetPos);
                solarFlux += star.getRadiationIntensity() / (distanceAU * distanceAU);
            }
        }

        // Albedo (Reflectivity)
        double oceanFraction = getOceanFraction();
        double albedo = 0.3;
        albedo += (getFrozenGasCoverage() * 0.6);
        albedo += -(oceanFraction * 0.2);
        albedo = Math.max(0.05, Math.min(albedo, 0.9));
        //System.out.println(albedo);

        // The actual energy absorbed by the planet
        double energyIn = solarFlux * (1.0 - albedo);
        //System.out.println("energyIn:" + energyIn);

        // 2. CALCULATE INSULATION (Greenhouse Blanket)
        // Base insulation is 1.0 (a vacuum). Higher numbers mean heat struggles to escape.
        double insulation = 1.0;
        insulation += getGasProperty(GasRegistry.co2).in_atm * 5;
        insulation += getGasProperty(GasRegistry.methane).in_atm * 50;

        //System.out.println("insulation:" + insulation);

        // Water Vapor Feedback
        if (currentTemp > 273.15) {
            insulation += Math.min(getHumidity(), 50);
        }

        //System.out.println("insulation after water:" + insulation);

        // 3. CALCULATE OUTGOING ENERGY (Eout)
        // Stefan-Boltzmann Law: planets radiate heat proportional to T^4.
        // The insulation divides the outgoing energy, trapping it.
        double energyOut = (EMISSION_CONSTANT * Math.pow(currentTemp, 4)) / insulation;
        //System.out.println("energyOut:" + energyOut);

        // 4. CALCULATE THERMAL MASS (Inertia)
        // Water and thick atmospheres resist temperature changes.
        // This stops the temperature from dropping instantly if a player drains an ocean.
        double thermalMass = 1.0 + (oceanFraction * 10) + (getGravitationalMultiplier() * 10);
        thermalMass = 1; // TODO: remove after testing

        // 5. APPLY DELTA (The simulation step)
        // If Ein > Eout, the planet warms. If Eout > Ein, it cools.
        double deltaTemp = (energyIn - energyOut) / thermalMass;

        // Apply the change to the planet
        properties().currentTemp += deltaTemp;
    }

    public void tickTemperatureEvents() {
        // slowly reduced target sea level while too hot
        // water will simply be voided, it is way too complicated to handle it in atm
        // because it would heavily interfere with player placed water and would not allow a sea level changing satellite
        if (getCurrentTemp() > 375) {
            if (Math.random() < 0.1 && properties().seaLevel > 0)
                properties().seaLevel--;
        }
    }
}