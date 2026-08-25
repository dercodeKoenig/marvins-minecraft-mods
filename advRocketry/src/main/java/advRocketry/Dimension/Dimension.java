package advRocketry.Dimension;

import advRocketry.GlobalTime;
import advRocketry.Main;
import advRocketry.Utils.AxisDirections;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class Dimension {
    protected DimensionProperties properties;

    protected StarCache starCache; // holds current main stars

    boolean isClientSide;
    public DimensionManager dimensionManager;

    public HashSet<Long> loadedChunks = new HashSet<>();

    public Dimension(DimensionProperties properties, DimensionManager dimensionManager) {
        this.properties = properties;
        this.dimensionManager = dimensionManager;
        this.isClientSide = dimensionManager.isClientSide;

        if (!isClientSide) {
            if (getDimensionId().getNamespace().equals(Main.MODID) && canVisit()) {
                createDimension();
            }
        }

        starCache = new StarCache();
    }

    public ServerLevel level() {
        return DimensionManager.getServerLevel(getDimensionId());
    }

    public DimensionProperties.DimensionType getType() {
        return properties.type;
    }

    public ResourceLocation getDimensionId() {
        return properties.dimensionId;
    }

    public String getName() {
        return properties.name;
    }

    abstract public void createDimension();

    abstract public boolean canVisit();

    abstract public Set<SurvivalProblem> getSurvivalProblems();

    abstract public boolean hasEnoughOxygenToBurn();

    abstract public float getGravitationalMultiplier();

    abstract public Vector3f getEmissiveColor();

    abstract public Vector3f getSkyColor();

    abstract public float getSkyDarken();

    abstract public Vector3f getSunRiseColor();

    abstract public Vector3f getFogColor();

    abstract public float getAtmosphereDensity();

    abstract public float getRadiationIntensity();

    abstract public boolean hasCustomSky();

    abstract public double computeTerrainBrightness(float partialTick);

    abstract public float computeCloudValue(); // how much cloud is there

    abstract public Vector3f computeTerrainCloudColor(float partialTick); // maybe based on atm composition?

    abstract public Vector3f computeTerrainFogColor(float partialTick);

    abstract public Vec3 getPosition(float partialTick);

    abstract public Vec3 getMovement();

    /**
     * calculates universe space coordinates for the local font up coordinates of the dimension
     */
    abstract public AxisDirections getGlobalAxisDirections(float partialTick);

    public void tick() {
        tickStarCache();

        if (!isClientSide) {
            //int ticked = 0;
            Iterator<Long> it = loadedChunks.iterator();
            ServerLevel level = level();
            while (it.hasNext()) {
                Long i = it.next();
                ChunkPos pos = new ChunkPos(i);
                if (level.hasChunk(pos.x, pos.z)) {
                    if (level.shouldTickBlocksAt(i)) {
                        tickChunk(level.getChunk(pos.x, pos.z));
                        //ticked++;
                    }
                } else {
                    it.remove();
                }
            }

            //if (GlobalTime.getGlobalTime() % 20 == 0 && loadedChunks.size() > 0) {
            //    System.out.println("loaded chunks: " + loadedChunks.size());
            //    System.out.println("ticked chunks: " + ticked);
            //}
        }
    }

    public void tickChunk(LevelChunk chunk){
        DimensionEvents.performRandomTickEvents(this, level(), chunk);
    }

    abstract public double getCurrentTemp();

    public Iterable<ResourceLocation> getCurrentMainStars() {
        return starCache.significantLightSourcesCache.keySet();
    }

    protected void tickStarCache() {
        starCache.updateSignificantLightSourcesCache(this);
    }

    public void updateDimensionProperties(DimensionProperties properties) {
        this.properties = properties;
    }

    public enum SurvivalProblem {
        TOO_HOT("too hot"),
        TOO_COLD("too cold"),
        TOO_LITTLE_O2("need more oxygen"),
        TOO_MUCH_O2("too much oxygen"),
        TOO_MUCH_PRESSURE("pressure too high"),
        TOO_LOW_PRESSURE("pressure too low"),
        TOO_MUCH_CO2("too much co2");

        public static final Set<SurvivalProblem> spaceProblems = new HashSet<>(List.of(TOO_LOW_PRESSURE, TOO_LITTLE_O2));

        public final String reason;

        SurvivalProblem(String reason) {
            this.reason = reason;
        }
    }
}