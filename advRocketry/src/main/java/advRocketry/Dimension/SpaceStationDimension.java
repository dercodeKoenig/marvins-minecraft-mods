package advRocketry.Dimension;

import advRocketry.Config;
import advRocketry.Main;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.CelestialUtils;
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
                new Vec3(0, 0, 1),
                new Vec3(0, 1, 0)
        );
    }

    @Override
    public void tick() {
        super.tickStarCache();

        ///  debug
        properties().parentDimensionId = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        properties().orbitDistanceTarget = 0.001f;
        //properties().position = Vec3.ZERO;

        Vec3 targetOrbitAxis = new Vec3(0.1,1,0);

        Vec3 positionError = properties().position.subtract(lazyPosition);
        Vec3 newLazyPosition = lazyPosition.add(positionError.scale(0.05));
        this.movement = newLazyPosition.subtract(lazyPosition);
        this.lazyPosition = newLazyPosition;

        Vec3 position = properties().position;
        double orbitDistanceTarget = properties().orbitDistanceTarget;
        Vec3 movement = Vec3.ZERO;

        if (properties().parentDimensionId != null && dimensionManager.get(properties().parentDimensionId) instanceof PlanetDimension parentPlanet) {

            Vec3 parentPosition = parentPlanet.getPosition(0);
            double orbitDistance = position.distanceTo(parentPosition);

            // add parent movement
            movement = movement.add(parentPlanet.getMovement());
            //System.out.println("parent movement:"+movement);

            // add orbit movement at current orbit
            double requiredOrbitSpeed_m_per_s = CelestialUtils.getSpeedForOrbit(
                    CelestialUtils.fromEarthMasses(parentPlanet.getGravitationalMultiplier()),
                    CelestialUtils.fromAU(orbitDistance)
            );
            double requiredOrbitSpeed = CelestialUtils.toAU(requiredOrbitSpeed_m_per_s) / 20; // convert back to au and per tick
            requiredOrbitSpeed = requiredOrbitSpeed * 100; // TODO: remove this line
            Vec3 directionToParent = parentPosition.subtract(position).normalize();
            Vec3 right = targetOrbitAxis.cross(directionToParent).normalize();
            movement = movement.add(right.scale(requiredOrbitSpeed));
            //System.out.println("with orbit movement:"+movement);

            // add correction to move to target orbit position / distance
            Vec3 targetPosition = parentPosition.add(directionToParent.scale(-1).scale(orbitDistanceTarget));
            Vec3 errorToTargetPosition = targetPosition.subtract(position);
            if(errorToTargetPosition.length() > 0.000001) {
                // calculate the allowed movement speed
                double distanceToClosestPlanet = -1;
                for(Dimension dim : dimensionManager.dimensions.values()){
                    if(dim instanceof PlanetDimension planetDimension){
                        Vec3 otherPlanetPosition = planetDimension.getPosition(0);
                        double d = otherPlanetPosition.distanceTo(position);
                        if(distanceToClosestPlanet < 0 || d < distanceToClosestPlanet){
                            distanceToClosestPlanet = d;
                        }
                    }
                }
                double nearTargetMultiplier = Math.min(1, distanceToClosestPlanet / Config.INSTANCE.rocket_SpaceTravel_Distance_For_Max_Speed);
                double correctionSpeed = Config.INSTANCE.rocket_SpaceTravel_AU_Per_Second * nearTargetMultiplier+ Config.INSTANCE.rocket_SpaceTravel_Min_Speed;
                Vec3 correctionMovement = errorToTargetPosition;
                Vec3 correctionMovement2 = errorToTargetPosition.scale(100000).normalize().scale(correctionSpeed);
                if(errorToTargetPosition.length() > correctionMovement2.length())
                    correctionMovement = correctionMovement2;
                movement = movement.add(correctionMovement);
            }
        }

        properties().position = position.add(movement);
    }
}
