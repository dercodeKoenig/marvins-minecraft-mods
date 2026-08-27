package advRocketry.Dimension;

import advRocketry.GlobalTime;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.Main;
import advRocketry.Registry.GasRegistry;
import advRocketry.Utils.AxisDirections;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;

public abstract class Dimension {
    public DimensionManager dimensionManager;
    public HashMap<Long, ChunkInfo> loadedChunks = new HashMap<>();
    public Queue<Runnable> tasks = new ArrayDeque<>();
    protected DimensionProperties properties;
    protected StarCache starCache; // holds current main stars
    boolean isClientSide;

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

    public void registerLoadedChunk(ChunkPos pos) {
        loadedChunks.put(pos.toLong(), new ChunkInfo());
    }

    public boolean shouldTickChunk(ChunkPos pos) {
        ChunkInfo info = loadedChunks.get(pos.toLong());
        if (info == null) return false;
        return info.shouldTick;
    }

    public void removeLoadedChunk(ChunkPos pos) {
        loadedChunks.remove(pos.toLong());
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

    abstract public double getCurrentTemp();

    abstract public float getRadiationIntensity();

    abstract public boolean hasCustomSky();

    abstract public double computeTerrainBrightness(float partialTick);

    abstract public float computeCloudValue(); // how much cloud is there

    abstract public Vector3f computeTerrainCloudColor(float partialTick); // maybe based on atm composition?

    abstract public Vector3f computeTerrainFogColor(float partialTick);

    abstract public Vec3 getPosition(float partialTick);

    abstract public Vec3 getMovement();

    // calculates universe space coordinates for the local font up coordinates of the dimension
    abstract public AxisDirections getGlobalAxisDirections(float partialTick);

    public void tick() {
        tickStarCache();
        runTasks();

        if (!isClientSide) {
            ServerLevel level = level();
            for (Long i : loadedChunks.keySet()) {
                ChunkPos pos = new ChunkPos(i);
                if (level.shouldTickBlocksAt(i)) {
                    loadedChunks.get(i).shouldTick = true;
                    tickChunk(pos);
                } else {
                    loadedChunks.get(i).shouldTick = false;
                }
            }
        }
    }

    public void runTasks() {
        if(tasks.isEmpty()) return;
        long start = System.nanoTime();
        float avgmspt = (float) ServerLifecycleHooks.getCurrentServer().getAverageTickTimeNanos() / 1000000;
        float targetmspt = 50;
        float maxMs = (targetmspt - avgmspt) / 3; // don't use all the budget
        int ticked = 0;
        while (!tasks.isEmpty()) {
            if (System.nanoTime() - start > maxMs * 1000 * 1000) {
                break;
            }
            Runnable task = tasks.poll();
            task.run();
            ticked++;
        }
        double elapsed = (System.nanoTime() - start);
        if (GlobalTime.getGlobalTime() % 50 == 0 && (!tasks.isEmpty() || ticked > 0)) {
            System.out.println("loaded tasks: " + tasks.size());
            System.out.println("ticked tasks: " + ticked);
            System.out.println("time: " + elapsed / 1000000);
            System.out.println(" ");
        }
    }

    public void tickChunk(ChunkPos pos) {
        double p = 0.1;
        if (Math.random() < p) {
            DimensionEvents.performRandomTickEvents(this, level(), pos);
        }
    }

    public Iterable<ResourceLocation> getCurrentMainStars() {
        return starCache.significantLightSourcesCache.keySet();
    }

    public boolean shouldFreezeBlocks(String gasId, @Nullable BlockPos pos) {
        double temp = getCurrentTemp();
        double pressure = getAtmosphereDensity();
        if (pos != null && LifeSupportSystem.isTemperatureRegulated(level(), pos))
            temp = 300;
        if (pos != null && LifeSupportSystem.isPressurized(level(), pos))
            pressure = Math.max(pressure, 1);
        return temp < GasRegistry.gases.get(gasId).getFreezeTemp(pressure);
    }

    public boolean shouldBoilBlocks(String gasId, @Nullable BlockPos pos) {
        double temp = getCurrentTemp();
        double pressure = getAtmosphereDensity();
        if (pos != null && LifeSupportSystem.isTemperatureRegulated(level(), pos))
            temp = 300;
        if (pos != null && LifeSupportSystem.isPressurized(level(), pos))
            pressure = Math.max(pressure, 1);
        return temp > GasRegistry.gases.get(gasId).getBoilingTemp(pressure);
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

    public static class ChunkInfo {
        boolean shouldTick = false;
    }
}