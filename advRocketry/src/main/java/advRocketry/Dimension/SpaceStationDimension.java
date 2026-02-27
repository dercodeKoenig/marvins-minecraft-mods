package advRocketry.Dimension;

import advRocketry.Config;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.CelestialUtils;
import advRocketry.utils.SpaceNavigation;
import advRocketry.worldgen.SpaceDimensionGeneration;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

public class SpaceStationDimension extends Dimension {

    private Vec3 lazyPosition = Vec3.ZERO; // interpolate toward position for smooth movement / sync
    private Vec3 movement = Vec3.ZERO;
    private boolean isInOrbit;

    private int ticksInSpaceTravel = 0;

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

    public ResourceLocation getParentDimensionId(){
        return properties().parentDimensionId;
    }
    public boolean isInOrbit(){
        return isInOrbit;
    }

    @Override
    public void tick() {
        super.tickStarCache();

        tickRotation();

        ///  debug
        //properties().parentDimensionId = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        properties().parentDimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "venus");
        properties().orbitDistanceTarget = 0.2f;
        Vec3 targetOrbitAxis = new Vec3(0.1, 1, 0);

        Vec3 positionError = properties().position.subtract(lazyPosition);
        Vec3 newLazyPosition = lazyPosition.add(positionError.scale(0.05));
        this.movement = newLazyPosition.subtract(lazyPosition);
        this.lazyPosition = newLazyPosition;

        Vec3 position = properties().position;
        Vec3 movement = Vec3.ZERO;

        if (properties().parentDimensionId != null && dimensionManager.get(properties().parentDimensionId) instanceof PlanetDimension parentPlanet) {
            // station has a target - fly there!

            Vec3 parentPosition = parentPlanet.getPosition(0);
            double distance = position.distanceTo(parentPosition);
            double planetRenderRadiusAU = getPlanetRenderRadiusAU(parentPlanet);

            Vec3 directionToParent = parentPosition.subtract(position).normalize();
            Vec3 right = targetOrbitAxis.cross(directionToParent).normalize();

            double orbitDistanceTarget = planetRenderRadiusAU * (1.5 + 10 * properties().orbitDistanceTarget);
            Vec3 targetPosition = parentPosition.add(directionToParent.scale(-1).scale(orbitDistanceTarget));

            boolean isCloseEnoughForOrbit = distance < planetRenderRadiusAU * 12;

            if (isCloseEnoughForOrbit) {
                isInOrbit = true;
                ticksInSpaceTravel = 0;

                // add parent movement
                movement = movement.add(parentPlanet.getMovement());

                // add orbit movement at current orbit
                double requiredOrbitSpeed_m_per_s = CelestialUtils.getSpeedForOrbit(
                        CelestialUtils.fromEarthMasses(parentPlanet.getGravitationalMultiplier()),
                        CelestialUtils.fromAU(distance)
                );
                double requiredOrbitSpeed = CelestialUtils.toAU(requiredOrbitSpeed_m_per_s) / 20; // convert back to au and per tick
                //requiredOrbitSpeed = requiredOrbitSpeed * 100; // TODO: remove this line
                movement = movement.add(right.scale(requiredOrbitSpeed));

                // add correction to move to target orbit position / distance
                Vec3 errorToTargetPosition = targetPosition.subtract(position);
                if (errorToTargetPosition.length() > 0.000001) {
                    double correctionSpeedMax = Config.INSTANCE.rocket_SpaceTravel_Min_Speed;
                    Vec3 errorToTargetPositionSpeedLimited = errorToTargetPosition.scale(100000).normalize().scale(correctionSpeedMax);
                    if (errorToTargetPosition.length() > errorToTargetPositionSpeedLimited.length())
                        errorToTargetPosition = errorToTargetPositionSpeedLimited;
                    movement = movement.add(errorToTargetPosition);
                }
            }

            if (!isCloseEnoughForOrbit) {
                isInOrbit = false;
                ticksInSpaceTravel++;

                // space navigation logic to move to target position
                Vec3 travelTarget = SpaceNavigation.getNextTargetAvoidPlanetCollision(targetPosition, position, dimensionManager, parentPlanet);

                Vec3 targetPositionRelative = travelTarget.subtract(position);

                double maxSpeed = Config.INSTANCE.rocket_SpaceTravel_AU_Per_Second / 20;
                double distanceForMaxSpeed = Config.INSTANCE.rocket_SpaceTravel_Distance_For_Max_Speed;

                double nearTargetMultiplier = Math.min(1, targetPositionRelative.length() / distanceForMaxSpeed);
                maxSpeed *= nearTargetMultiplier; // slow down when near target

                double justStartedMultiplier = Math.min(1, ticksInSpaceTravel / (20 * 10));
                maxSpeed *= justStartedMultiplier; // slow down when just started

                double e = Config.INSTANCE.rocket_SpaceTravel_Min_Speed;
                double offTargetMultiplier = Math.max(0, targetPositionRelative.normalize().dot(properties().front) - 0.98) * 50;

                double speed = maxSpeed * offTargetMultiplier + e;

                setTargetFront(targetPositionRelative);

                movement = properties().front.scale(speed);
            }
        } else {
            // station has no target
            ticksInSpaceTravel = 0;
            isInOrbit = false;
        }

        properties().position = position.add(movement);
    }

    public void setTargetFront(Vec3 targetFront) {
        properties().targetFront = targetFront.normalize();
    }

    // mostly copied from rocket controller
    public void tickRotation() {
        double rotationRate = 0.002;
        Vec3 rotationCorrection;
        if (properties().targetFront.dot(properties().front) > -0.99) {
            rotationCorrection = properties().targetFront.subtract(properties().front).scale(rotationRate);
            if (rotationCorrection.length() > rotationRate)
                rotationCorrection = rotationCorrection.normalize().scale(rotationRate);
        } else
            rotationCorrection = properties().up.subtract(properties().front).normalize().scale(rotationRate);

        properties().front = properties().front.add(rotationCorrection).normalize();

        // always try to head up, TODO; replace with target up
        Vec3 targetUpValid = properties().front.cross(new Vec3(0,1,0).cross(properties().front)).normalize();
        if (targetUpValid.dot(properties().up) < -0.9)
            targetUpValid = properties().front.cross(properties().up);
        rotationCorrection = targetUpValid.subtract(properties().up).scale(rotationRate * 0.5f);
        Vec3 newUp = properties().up.add(rotationCorrection).normalize();

        Vec3 right = properties().front.cross(newUp).normalize();
        properties().up = right.cross(properties().front).normalize();
    }

    double getPlanetRenderRadiusAU(PlanetDimension planet) {
        return CelestialUtils.toAU(CelestialUtils.fromEarthRadius(planet.getEarthRadiusMultiplier())) * Config.INSTANCE.planet_Render_Scale_Multiplier;
    }
}
