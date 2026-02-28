package advRocketry.Dimension;

import ARLib.network.SimpleNetworkPacket;
import advRocketry.Config;
import advRocketry.Main;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.CelestialUtils;
import advRocketry.utils.SpaceNavigation;
import advRocketry.worldgen.SpaceDimensionGeneration;
import com.google.gson.Gson;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.util.Objects;

import static advRocketry.utils.CelestialUtils.getPlanetRenderRadiusAU;

public class SpaceStationDimension extends Dimension {

    private Vec3 lazyPosition = Vec3.ZERO; // interpolate toward position for smooth movement / sync
    private Vec3 movement = Vec3.ZERO;
    private boolean isInOrbit;

    public SpaceStationDimension(DimensionProperties properties, DimensionManager dimensionManager) {
        super(properties, dimensionManager);
    }

    private SpaceStationDimensionProperties properties() {
        return (SpaceStationDimensionProperties) properties;
    }

    @Override
    public void createDimension() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        System.out.println("creating space station: " + getDimensionId());
        DynamicDimensionRegistry dynamicDimensionRegistry = DynamicDimensionRegistry.from(server);

        ChunkGenerator generator = SpaceDimensionGeneration.makeChunkGenerator();
        DimensionType type = SpaceDimensionGeneration.makeDimensionType();
        ServerLevel l = dynamicDimensionRegistry.loadDynamicDimension(properties.dimensionId, generator, type);
        if (l == null) {
            dynamicDimensionRegistry.createDynamicDimension(
                    properties.dimensionId,
                    generator,
                    type
            );
        }
    }

    @Override
    public boolean canVisit() {
        return true;
    }

    @Override
    public boolean hasEnoughOxygen() {
        return false;
    }

    @Override
    public boolean canRain() {
        return false;
    }

    @Override
    public float getGravitationalMultiplier() {
        return 0.1f;
    }

    @Override
    public Vector3f getEmissiveColor() {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f getSkyColor() {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f getSunRiseColor() {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f getFogColor() {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public float getAtmosphereDensity() {
        return 0;
    }

    @Override
    public float getRadiationIntensity() {
        return 0;
    }

    @Override
    public boolean hasCustomSky() {
        return true;
    }

    @Override
    public double getTerrainBrightness(float partialTick) {
        return 1;
    }

    @Override
    public Vector3f getCloudColor(float partialTick) {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vector3f computeTerrainFogColor(float partialTick) {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public Vec3 getPosition(float partialTick) {
        return lazyPosition;
    }

    @Override
    public Vec3 getMovement() {
        return movement;
    }

    @Override
    public AxisDirections getGlobalAxisDirections(float partialTick) {
        return new AxisDirections(
                properties().front,
                properties().up
        );
    }

    public ResourceLocation getParentDimensionId() {
        return properties().parentDimensionId;
    }

    public boolean isInOrbit() {
        return isInOrbit;
    }

    public boolean initialBlocksPlaced() {
        return properties().initialBlocksPlaced;
    }

    public void setInitialBlocksPlaced() {
        properties().initialBlocksPlaced = true;
        System.out.println("initial blocks placed for station " + getName());
        dimensionManager.syncDimensionProperties(this);
    }

    public boolean isPositionInitialized() {
        return properties().positionInitialized;
    }

    public void initializePosition(Vec3 position, ResourceLocation parentDimensionId) {

        if (parentDimensionId != null && dimensionManager.get(parentDimensionId) instanceof PlanetDimension parentPlanet) {
            Vec3 parentPosition = parentPlanet.getPosition(0);
            double planetRenderRadiusAU = getPlanetRenderRadiusAU(parentPlanet);
            properties().position = getTargetPosition(planetRenderRadiusAU,parentPosition);
        } else {
            if(position == null)
                throw new RuntimeException("position can not be null here!");
            properties().position = position;
        }

        lazyPosition = properties().position;
        properties().parentDimensionId = parentDimensionId;
        properties().positionInitialized = true;
        System.out.println("position initialized for station: " + getName());
        System.out.println("position:" + lazyPosition);
        System.out.println("parent:" + parentDimensionId);
        dimensionManager.syncDimensionProperties(this);
    }

    public void setTargetPlanet(ResourceLocation targetPlanet){
        if (!Objects.equals(properties().parentDimensionId, targetPlanet)){
            properties().lastParentDimensionId = properties().parentDimensionId;
            properties().parentDimensionId = targetPlanet;
            dimensionManager.syncDimensionProperties(this);
        }
    }

    public void setTargetOrbitDistance(float targetDistance){
        if(Math.abs(properties().orbitDistanceTarget - targetDistance) > 0.00001) {
            properties().orbitDistanceTarget = targetDistance;
            dimensionManager.syncDimensionProperties(this);
        }
    }

    public void setTargetOrbitAxis(Vec3 orbitAxis){
        Vec3 normalizedOrbit = orbitAxis.normalize();
        if(normalizedOrbit.dot(properties().orbitAxisTarget) < 0.9999) {
            properties().orbitAxisTarget = normalizedOrbit;
            dimensionManager.syncDimensionProperties(this);
        }
    }

    @Override
    public void tick() {
        super.tickStarCache();

        tickRotation();

        ///  debug
        if(!dimensionManager.isClientSide) {
            //setTargetPlanet(ResourceLocation.fromNamespaceAndPath("adv_rocketry", "venus"));
            setTargetPlanet(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
            setTargetOrbitDistance(0.5f);
        }

        Vec3 positionError = properties().position.subtract(lazyPosition);
        Vec3 newLazyPosition = lazyPosition.add(positionError.scale(0.01));
        this.movement = newLazyPosition.subtract(lazyPosition);
        this.lazyPosition = newLazyPosition;

        Vec3 position = properties().position;
        Vec3 movement = Vec3.ZERO;

        Dimension parent = dimensionManager.get(properties().parentDimensionId);
        if (parent instanceof PlanetDimension parentPlanet) {
            // station has a target - fly there!

            Vec3 targetOrbitAxis = properties().orbitAxisTarget;

            Vec3 parentPosition = parentPlanet.getPosition(0);
            double distanceToParent = position.distanceTo(parentPosition);
            double planetRenderRadiusAU = getPlanetRenderRadiusAU(parentPlanet);
            Vec3 directionToParent = parentPosition.subtract(position).normalize();
            Vec3 right = targetOrbitAxis.cross(directionToParent).normalize();

            Vec3 targetPosition = getTargetPosition(planetRenderRadiusAU,parentPosition);

            boolean isCloseEnoughForOrbit = isCloseEnoughForOrbit(planetRenderRadiusAU,distanceToParent);

            if (isCloseEnoughForOrbit) {
                isInOrbit = true;

                // add parent movement
                movement = movement.add(parentPlanet.getMovement());

                // add orbit movement at current orbit
                double requiredOrbitSpeed_m_per_s = CelestialUtils.getSpeedForOrbit(
                        CelestialUtils.fromEarthMasses(parentPlanet.getGravitationalMultiplier()),
                        CelestialUtils.fromAU(distanceToParent)
                );
                double requiredOrbitSpeed = CelestialUtils.toAU(requiredOrbitSpeed_m_per_s) / 20; // convert back to au and per tick
                requiredOrbitSpeed = requiredOrbitSpeed * Config.INSTANCE.planet_Render_Scale_Multiplier; // adjust for the inflated size
                movement = movement.add(right.scale(requiredOrbitSpeed));

                // add correction to move to target orbit position / distance
                Vec3 errorToTargetPosition = targetPosition.subtract(position);
                if (errorToTargetPosition.length() > 0.000001) {
                    double correctionSpeedMax = Config.INSTANCE.station_SpaceTravel_Min_Speed;
                    Vec3 errorToTargetPositionSpeedLimited = errorToTargetPosition.scale(100000).normalize().scale(correctionSpeedMax);
                    if (errorToTargetPosition.length() > errorToTargetPositionSpeedLimited.length())
                        errorToTargetPosition = errorToTargetPositionSpeedLimited;
                    movement = movement.add(errorToTargetPosition);
                }
            }

            if (!isCloseEnoughForOrbit) {
                isInOrbit = false;

                // space navigation logic to move to target position
                Vec3 travelTarget = SpaceNavigation.getNextTargetAvoidPlanetCollision(targetPosition, position, dimensionManager, parentPlanet);

                Vec3 finalTargetPositionRelative = targetPosition.subtract(position);
                Vec3 nextTargetPositionRelative = travelTarget.subtract(position);

                double maxSpeed = Config.INSTANCE.station_SpaceTravel_AU_Per_Second / 20;
                double distanceForMaxSpeed = Config.INSTANCE.station_SpaceTravel_Distance_For_Max_Speed;

                double nearTargetMultiplier = Math.min(1, finalTargetPositionRelative.length() / distanceForMaxSpeed);
                maxSpeed *= nearTargetMultiplier; // slow down when near target

                Dimension lastParent = dimensionManager.get(properties().lastParentDimensionId);
                if(lastParent instanceof PlanetDimension lastParentPlanet) {
                    double distanceToOrigin = position.distanceTo(lastParentPlanet.getPosition(0));
                    double nearOriginMultiplier = Math.min(1, distanceToOrigin / distanceForMaxSpeed);
                    maxSpeed *= nearOriginMultiplier; // slow down when still near origin
                }

                double offTargetMultiplier = Math.max(0, finalTargetPositionRelative.normalize().dot(properties().front) - 0.98) * 50;

                double e = Config.INSTANCE.station_SpaceTravel_Min_Speed;
                double offNextTargetMultiplier = 1;
                if(nextTargetPositionRelative.length() > 0.0001)
                    offNextTargetMultiplier = Math.max(0, nextTargetPositionRelative.scale(10000).normalize().dot(properties().front) - 0.5) * 1.5;

                double speed = maxSpeed * offTargetMultiplier + e * offNextTargetMultiplier;

                setTargetFront(nextTargetPositionRelative);

                movement = properties().front.scale(speed);
            }
        } else {
            // station has no target
            isInOrbit = false;
        }

        properties().position = position.add(movement);
    }

    public void setTargetFront(Vec3 targetFront) {
        properties().targetFront = targetFront.normalize();
    }

    public Vec3 getTargetPosition(double planetRenderRadiusAU, Vec3 planetPosition){
        Vec3 directionToPlanet = planetPosition.subtract(properties().position);
        double maxR = Config.INSTANCE. station_Max_Orbit_R_Factor;
        double orbitDistanceTarget = planetRenderRadiusAU * (1.5 + maxR * properties().orbitDistanceTarget);
        Vec3 targetOrbitAxis = properties().orbitAxisTarget;
        Vec3 equator = targetOrbitAxis.cross(targetOrbitAxis.cross(directionToPlanet)).normalize();
        Vec3 targetPosition = planetPosition.add(equator.scale(orbitDistanceTarget));
        return targetPosition;
    }
    public boolean isCloseEnoughForOrbit(double planetRenderRadiusAU, double distanceAU){
        double maxR = Config.INSTANCE. station_Max_Orbit_R_Factor;
        return distanceAU < planetRenderRadiusAU * maxR * 1.2;
    }

    // mostly copied from rocket controller
    public void tickRotation() {
        double rotationRate = Config.INSTANCE.station_SpaceTravel_Rotation_Rate;
        Vec3 rotationCorrection;
        if (properties().targetFront.dot(properties().front) > -0.99) {
            rotationCorrection = properties().targetFront.subtract(properties().front).scale(rotationRate);
        } else
            rotationCorrection = properties().up.cross(properties().front).scale(rotationRate / 10);

        properties().front = properties().front.add(rotationCorrection).normalize();

        // always try to head up, TODO; replace with target up
        Vec3 targetUpValid = properties().front.cross(new Vec3(0, 1, 0).cross(properties().front)).normalize();
        if (targetUpValid.dot(properties().up) < -0.9)
            targetUpValid = properties().front.cross(properties().up);
        rotationCorrection = targetUpValid.subtract(properties().up).scale(rotationRate * 0.5f);
        Vec3 newUp = properties().up.add(rotationCorrection).normalize();

        Vec3 right = properties().front.cross(newUp).normalize();
        properties().up = right.cross(properties().front).normalize();
    }

    @Override
    public void updateDimensionProperties(DimensionProperties properties){
        if(!properties().positionInitialized && ((SpaceStationDimensionProperties)properties).positionInitialized){
            // when dimension is initialized, to instant lerp to target
            lazyPosition = ((SpaceStationDimensionProperties) properties).position;
        }
        super.updateDimensionProperties(properties);
    }
}
