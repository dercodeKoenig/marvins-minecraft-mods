package advRocketry.Dimension;

import advRocketry.Config;
import advRocketry.GlobalTime;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.CelestialUtils;
import advRocketry.utils.ClientUtils;
import advRocketry.utils.SpaceNavigation;
import advRocketry.worldgen.SpaceDimensionGeneration;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.util.Objects;

import static advRocketry.utils.CelestialUtils.getPlanetRenderRadiusAU;

public class SpaceStationDimension extends Dimension {

    private static double lerpFactor = 0.01;

    // interpolate toward target for smooth movement / sync
    private Vec3 lazyPosition;
    private Vec3 lazyFront;
    private Vec3 lazyUp;

    // the client will stop ticking the station if not in the level
    // the server will no longer send updates to the player when not in the level
    // this is there to detect if the last update was a longer time ago and then we instantly
    // fill the interpolation targets
    private long lastPropertiesSyncTime = 0;

    private Vec3 movement = Vec3.ZERO;
    private boolean isInOrbit;
    private boolean isInSpaceTravel;

    public SpaceStationDimension(DimensionProperties properties, DimensionManager dimensionManager) {
        super(properties, dimensionManager);
        lazyFront = properties().front;
        lazyUp = properties().up;
        lazyPosition = properties().position;
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
        // don't ask me why, 180 is just the offset that works
        double angleDeg = properties().frontFacing.toYRot() + 180;
        Vec3 frontRotatedToFacing = CelestialUtils.rotate(getFront(), getUp(), angleDeg);
        return new AxisDirections(
                frontRotatedToFacing,
                getUp()
        );
    }

    @Override
    public void updateDimensionProperties(DimensionProperties properties) {
        super.updateDimensionProperties(properties);
        if (lastPropertiesSyncTime + 20 * 60 < GlobalTime.getGlobalTime()) {
            // last update was never or long time ago
            // maybe initial station creation or player just joined the level
            // do instant update on interpolation targets
            lazyPosition = properties().position;
            lazyFront = properties().front;
            lazyUp = properties().up;
            System.out.println("client skip interpolation, likely during level change: " + properties.dimensionId);
        }
        lastPropertiesSyncTime = GlobalTime.getGlobalTime();
    }

    public void setFrontFacing(Direction facing){
        properties().frontFacing = facing;
        dimensionManager.syncDimensionProperties(this);
    }

    public ResourceLocation getParentDimensionId() {
        return properties().parentDimensionId;
    }

    public boolean isInOrbit() {
        return isInOrbit;
    }

    public boolean isInSpaceTravel() {
        return isInSpaceTravel;
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
            properties().position = getTargetPosition(planetRenderRadiusAU, parentPosition);
        } else {
            if (position == null)
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

    public void setTargetPlanet(ResourceLocation targetPlanet) {
        if (!Objects.equals(properties().parentDimensionId, targetPlanet)) {
            properties().lastParentDimensionId = properties().parentDimensionId;
            properties().parentDimensionId = targetPlanet;
            dimensionManager.syncDimensionProperties(this);
        }
    }

    public float getTargetOrbitDistance() {
        return properties().orbitDistanceTarget;
    }

    public void setTargetOrbitDistance(float targetDistance) {
        if (Math.abs(properties().orbitDistanceTarget - targetDistance) > 0.00001) {
            properties().orbitDistanceTarget = targetDistance;
            dimensionManager.syncDimensionProperties(this);
        }
    }

    public Vec3 getTargetOrbitAxis() {
        return properties().orbitAxisTarget;
    }

    public void setTargetOrbitAxis(Vec3 orbitAxis) {
        Vec3 normalizedOrbit = orbitAxis.normalize();
        if (normalizedOrbit.dot(properties().orbitAxisTarget) < 0.9999) {
            properties().orbitAxisTarget = normalizedOrbit;
            dimensionManager.syncDimensionProperties(this);
        }
    }

    void setTargetFront(Vec3 targetFront, boolean sync) {
        properties().targetFront = targetFront.normalize();
        if (sync)
            dimensionManager.syncDimensionProperties(this);
    }

    void setTargetUp(Vec3 targetUp, boolean sync) {
        properties().targetUp = targetUp.normalize();
        if (sync)
            dimensionManager.syncDimensionProperties(this);
    }

    public Vec3 getFront() {
        return lazyFront;
    }

    public Vec3 getUp() {
        return lazyUp;
    }

    public void setRotationSettings(double yaw, double roll, double pitch, SpaceStationDimensionProperties.RotationMode mode) {
        boolean requiresUpdate = false;
        if (properties().yaw != yaw)
            requiresUpdate = true;
        if (properties().roll != roll)
            requiresUpdate = true;
        if (properties().pitch != pitch)
            requiresUpdate = true;
        if (properties().rotationMode != mode)
            requiresUpdate = true;

        properties().yaw = yaw;
        properties().roll = roll;
        properties().pitch = pitch;
        properties().rotationMode = mode;

        if (requiresUpdate)
            dimensionManager.syncDimensionProperties(this);
    }

    public Vec3 getRotationSettings() {
        return new Vec3(properties().yaw, properties().roll, properties().pitch);
    }

    public SpaceStationDimensionProperties.RotationMode getRotationMode() {
        return properties().rotationMode;
    }

    @Override
    public void tick() {

        // server and client can get out of sync because stations can move and are not fixed in orbit like a planet
        // planets only variably for position is the global time, but here it is more difficult
        // i will send the properties to the client every few seconds
        // but only to the players on this dimension
        if (GlobalTime.getGlobalTime() % (20 * 10) == 0) {
            dimensionManager.syncDimensionProperties(this, true);
        }
        if (dimensionManager.isClientSide && !Objects.equals(getDimensionId(), ClientUtils.getPlayerLevel().dimension().location()))
            // skip this code on client when it is not on this dimension for performance reason
            // there is nothing in here that is required while the player is on a different dimension
            // during level load it should request a dimension sync to get the current state
            return;

        super.tickStarCache();

        tickRotation();
        tickPosition();

        Vec3 positionError = properties().position.subtract(lazyPosition);
        Vec3 newLazyPosition = lazyPosition.add(positionError.scale(lerpFactor));
        this.movement = newLazyPosition.subtract(lazyPosition);
        this.lazyPosition = newLazyPosition;

    }

    public void tickPosition() {
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

            Vec3 targetPosition = getTargetPosition(planetRenderRadiusAU, parentPosition);

            boolean isCloseEnoughForOrbit = isCloseEnoughForOrbit(planetRenderRadiusAU, distanceToParent);

            if (isCloseEnoughForOrbit) {
                isInOrbit = true;
                isInSpaceTravel = false;

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
                isInSpaceTravel = true;

                // space navigation logic to move to target position
                Vec3 travelTarget = SpaceNavigation.getNextTargetAvoidPlanetCollision(targetPosition, position, dimensionManager, parentPlanet);

                Vec3 finalTargetPositionRelative = targetPosition.subtract(position);
                Vec3 nextTargetPositionRelative = travelTarget.subtract(position);

                double maxSpeed = Config.INSTANCE.station_SpaceTravel_AU_Per_Second / 20;
                double distanceForMaxSpeed = Config.INSTANCE.station_SpaceTravel_Distance_For_Max_Speed;

                double nearTargetMultiplier = Math.min(1, finalTargetPositionRelative.length() / distanceForMaxSpeed);
                maxSpeed *= nearTargetMultiplier; // slow down when near target

                Dimension lastParent = dimensionManager.get(properties().lastParentDimensionId);
                if (lastParent instanceof PlanetDimension lastParentPlanet) {
                    double distanceToOrigin = position.distanceTo(lastParentPlanet.getPosition(0));
                    double nearOriginMultiplier = Math.min(1, distanceToOrigin / distanceForMaxSpeed);
                    maxSpeed *= nearOriginMultiplier; // slow down when still near origin
                }

                double offTargetMultiplier = Math.max(0, finalTargetPositionRelative.normalize().dot(getFront()) - 0.98) * 50;

                double e = Config.INSTANCE.station_SpaceTravel_Min_Speed;
                double offNextTargetMultiplier = 1;
                if (nextTargetPositionRelative.length() > 0.0001)
                    offNextTargetMultiplier = Math.max(0, nextTargetPositionRelative.scale(10000).normalize().dot(getFront()) - 0.5) * 1.5;

                double speed = maxSpeed * offTargetMultiplier + e * offNextTargetMultiplier;

                setTargetFront(nextTargetPositionRelative, false);

                movement = getFront().scale(speed);
            }
        } else {
            // station has no target
            isInOrbit = false;
            isInSpaceTravel = false;
        }

        properties().position = position.add(movement);

        // avoid collision with other planets
        // for performance, i only check against closest planet.
        // unless the second closes planet has a giant radius it should work well
        PlanetDimension closestPlanet = null;
        Vec3 closestPlanetPosition = null;
        double closestDistance = Double.MAX_VALUE;
        for (Dimension dim : dimensionManager.dimensions.values()) {
            if (dim instanceof PlanetDimension planet) {
                Vec3 planetPosition = planet.getPosition(0);
                double distance = planetPosition.distanceTo(getPosition(0));
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlanet = planet;
                    closestPlanetPosition = planetPosition;
                }
            }
        }
        if (closestPlanet != null) {
            double planetRadius = CelestialUtils.getPlanetRenderRadiusAU(closestPlanet);
            if (closestDistance < planetRadius * 1.2) {
                // fix the position, we are to close
                // scale before normalize or numerical errors will break it!
                Vec3 planetToStation = getPosition(0).subtract(closestPlanetPosition);
                if(planetToStation.length() < 0.000001)
                    planetToStation = new Vec3(Math.random()*2-1,0,Math.random()*2-1);
                properties().position = closestPlanetPosition.add(planetToStation.scale(10000).normalize().scale(planetRadius * 1.25));
            }
        }
    }

    // mostly copied from rocket controller
    public void tickRotation() {
        double rotationRate = Config.INSTANCE.station_SpaceTravel_Rotation_Rate;
        Vec3 rotationCorrection;
        if (properties().targetFront.dot(properties().front) > -0.99)
            rotationCorrection = properties().targetFront.subtract(properties().front).scale(rotationRate);
        else
            rotationCorrection = properties().up.cross(properties().front).scale(rotationRate / 10);

        properties().front = properties().front.add(rotationCorrection).normalize();

        // calculate correct up
        Vec3 targetUpValid = properties().front.cross(properties().targetUp.cross(properties().front)).normalize();
        if (targetUpValid.dot(properties().up) < -0.9)
            targetUpValid = properties().front.cross(properties().up);

        rotationCorrection = targetUpValid.subtract(properties().up).scale(rotationRate * 0.5f);
        Vec3 newUpInvalid = properties().up.add(rotationCorrection).normalize();
        // newUpInvalid is not orthogonal to front, so it needs adjustment
        Vec3 right = properties().front.cross(newUpInvalid);
        properties().up = right.cross(properties().front).normalize();


        // interpolate the lazy values
        rotationCorrection = properties().front.subtract(lazyFront).scale(lerpFactor);
        lazyFront = lazyFront.add(rotationCorrection).normalize();
        rotationCorrection = properties().up.subtract(lazyUp).scale(lerpFactor);
        Vec3 lazyUpInvalid = lazyUp.add(rotationCorrection);
        lazyUp = lazyFront.cross(lazyUpInvalid.cross(lazyFront)).normalize();

        // move toward the target rotation given in the settings
        if (!isInSpaceTravel) {
            tickRotationFromSettings();
        }
    }

    public void tickRotationFromSettings() {
        // update rotation from selected orientation settings

        SpaceStationDimensionProperties.RotationMode rotationMode = properties().rotationMode;
        boolean isRelativeRotation = rotationMode == SpaceStationDimensionProperties.RotationMode.relative;
        boolean isAbsoluteRotation = rotationMode == SpaceStationDimensionProperties.RotationMode.absolute;
        if (isRelativeRotation || isAbsoluteRotation) {
            // base for absolute rotation
            Vec3 targetFront = new Vec3(0, 0, 1);
            Vec3 targetUp = new Vec3(0, 1, 0);
            if (properties().rotationMode == SpaceStationDimensionProperties.RotationMode.relative) {
                // calculate new base values
                Dimension parent = dimensionManager.get(properties().parentDimensionId);
                if (parent instanceof PlanetDimension parentPlanet) {
                    Vec3 parentPosition = parentPlanet.getPosition(0);
                    Vec3 parentToStation = properties().position.subtract(parentPosition);
                    targetUp = parentToStation.normalize();

                    Vec3 targetOrbitAxis = properties().orbitAxisTarget;
                    Vec3 right = targetOrbitAxis.cross(parentToStation.scale(-1)).normalize();
                    targetFront = right;

                    // this will make the planet to the side as normal, making it easier to use yaw for rotation
                    targetUp = targetFront.cross(targetUp);

                } else {
                    targetFront = null; // no turning because no parent planet
                }
            }
            if (targetFront != null) {
                // apply rotations
                // target front rotates around target up
                targetFront = CelestialUtils.rotate(targetFront, targetUp, -(properties().yaw * 360 - 180));
                // target up rotates around target front
                targetUp = CelestialUtils.rotate(targetUp, targetFront, properties().roll * 360 - 180);
                // both rotate around pitch
                Vec3 pitchAxis = targetFront.cross(targetUp).normalize();
                targetUp = CelestialUtils.rotate(targetUp, pitchAxis, properties().pitch * 360 - 180);
                targetFront = CelestialUtils.rotate(targetFront, pitchAxis, properties().pitch * 360 - 180);
                setTargetFront(targetFront, false);
                setTargetUp(targetUp, false);
                //System.out.println(properties().targetUp+":"+dimensionManager.isClientSide);
            }
        }
    }

    boolean isCloseEnoughForOrbit(double planetRenderRadiusAU, double distanceAU) {
        double maxR = Config.INSTANCE.station_Max_Orbit_R_Factor;
        return distanceAU < planetRenderRadiusAU * maxR * 1.2;
    }

    public double getOrbitDistanceTarget(double planetRenderRadiusAU, double orbitDistanceTarget) {
        double maxR = Config.INSTANCE.station_Max_Orbit_R_Factor;
        return planetRenderRadiusAU * (1.5 + maxR * orbitDistanceTarget);
    }

    Vec3 getTargetPosition(double planetRenderRadiusAU, Vec3 planetPosition) {
        Vec3 directionToPlanet = planetPosition.subtract(properties().position);
        double orbitDistanceTarget = getOrbitDistanceTarget(planetRenderRadiusAU, properties().orbitDistanceTarget);
        Vec3 targetOrbitAxis = properties().orbitAxisTarget;
        Vec3 equator = targetOrbitAxis.cross(targetOrbitAxis.cross(directionToPlanet)).normalize();
        Vec3 targetPosition = planetPosition.add(equator.scale(orbitDistanceTarget));
        return targetPosition;
    }
}
