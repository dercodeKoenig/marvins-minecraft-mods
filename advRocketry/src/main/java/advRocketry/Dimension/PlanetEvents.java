package advRocketry.Dimension;

import advRocketry.Blocks.DryIceBlock;
import advRocketry.Config;
import advRocketry.Registry.GasRegistry;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static advRocketry.Dimension.DimensionEvents.water_frozen_by_low_planet_temp;

public class PlanetEvents {

    // return all gases sorted by the boiling temp on this atm density with highest boiling temp first
    public static List<GasRegistry.Gas> getGasesSortedByBoilingTemp(double atm){
        return GasRegistry.gases.values().stream()
                .sorted(
                        Comparator.comparingDouble(g -> g.getBoilingTemp(atm))
                ).toList()
                .reversed();
    }


    // called from server level mixin
    // performs slow terraforming ticks
    public static void performTerraformingTicks(PlanetDimension planet, ServerLevel level, LevelChunk chunk) {

        ChunkPos chunkPos = chunk.getPos();

        int speed = 5;

        if ((level.getGameTime() + Math.abs(chunkPos.hashCode())) % speed == 0) {

            // 1. Get the current time in seconds
            long currentIndex = level.getGameTime() / speed;

            // 2. Create a deterministic offset for this specific chunk.
            // Multiplying by prime numbers spreads out the starting positions wildly across the world.
            long chunkOffset = Math.abs((long) chunkPos.x * 31337L + (long) chunkPos.z * 31L);

            // 3. Calculate the index within the 0-255 range (16x16 blocks = 256 total)
            int blockIndex = (int) ((currentIndex + chunkOffset) % 256);

            // 4. Convert the 1D index back into 2D local chunk coordinates (0-15)
            int localX = blockIndex % 16;
            int localZ = blockIndex / 16;

            // 5. Get the actual world coordinates
            int blockX = chunkPos.getBlockX(localX);
            int blockZ = chunkPos.getBlockZ(localZ);

            // Run the logic on the targeted block

            // adjust sea level for all the gases
            for (GasRegistry.Gas gas : getGasesSortedByBoilingTemp(planet.getAtmosphereDensity())) {
                if (SeaLevelAdjustment.adjustSeaLevelIfRequired(planet, gas, blockX, blockZ, 3))
                    break; // avoid gas mixing if many gases exist
            }

            // spawn possible dry ice blocks
            DryIceBlock.placeDryIceIfPossible(planet, blockX, blockZ, 3);


            TerraformingSystem.maybeUpdateBlocksForNewBiome(level, blockX, blockZ);
        }
    }

    // performs initial full terraforming on chunk load
    // runs for the 3x3 chunk region around any chunk thats created
    public static void maybePerformInitialTerraforming(PlanetDimension planet, ServerLevel serverLevel, int chunkX, int chunkZ) {

        // check if neighbor chunks are already loaded to prevent infinite chunk loading
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                if (!serverLevel.hasChunk(chunkX + x, chunkZ + z))
                    return; // neighbor not loaded

        ChunkAccess chunk = serverLevel.getChunk(chunkX, chunkZ);
        String key = "had_initial_terraforming_tick";
        CompoundTag info = ChunkUtils.getEntryOrNew(chunk, key);
        if (info.contains("true")) return; // already handled
        info.put("true", new CompoundTag());
        ChunkUtils.setEntry(chunk, key, info);
        //System.out.println("initial terraforming at " + chunkX + ":" + chunkZ+":"+serverLevel);

        double planetTemp = planet.getCurrentTemp();
        double atmLevel = planet.getAtmosphereDensity();
        boolean shouldFreezeWater = planetTemp + 1 < GasRegistry.gases.get(GasRegistry.water).getFreezeTemp(atmLevel);
        for (int cx = 0; cx < 16; cx++) {
            for (int cz = 0; cz < 16; cz++) {
                int x = chunk.getPos().getBlockX(cx);
                int z = chunk.getPos().getBlockZ(cz);

                // adjust sea level
                for (GasRegistry.Gas gas : getGasesSortedByBoilingTemp(planet.getAtmosphereDensity())) {
                    while (SeaLevelAdjustment.adjustSeaLevelIfRequired(planet, gas, x, z, 3)) {
                        continue; // nothing to do, all the action happens above
                    }
                }

                // place dry ice
                while (DryIceBlock.placeDryIceIfPossible(planet, x, z, 3)) {
                    continue; // nothing to do, all the action happens above
                }

                // after sea level adjustment, maybe freeze water or do other actions
                for (int y = serverLevel.getMinBuildHeight(); y < serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, x, z); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = serverLevel.getBlockState(pos);

                    // freeze water if possible, after the sea level is adjusted
                    if (state.getBlock().equals(net.minecraft.world.level.block.Blocks.WATER) && shouldFreezeWater) {
                        serverLevel.setBlock(pos, net.minecraft.world.level.block.Blocks.ICE.defaultBlockState().setValue(water_frozen_by_low_planet_temp, true), 3);
                    }
                }
            }
        }
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && DimensionManager.INSTANCE_SERVER.get(serverLevel.dimension().location()) instanceof PlanetDimension planet) {
            ChunkAccess chunk = event.getChunk();

            if (event.isNewChunk()) {
                for (int cx = 0; cx < 16; cx++) {
                    for (int cz = 0; cz < 16; cz++) {
                        int x = chunk.getPos().getBlockX(cx);
                        int z = chunk.getPos().getBlockZ(cz);

                        if (((PlanetDimensionProperties) planet.properties).customSeaFluid == null) {
                            // it generated water, save initial water level so that it doesn't rain by default and fill caves
                            SeaLevelAdjustment.saveInitialWaterLevelOnChunkGeneration(serverLevel, chunk, x, z);
                        }

                        TerraformingSystem.storeGeneratedBiome(TerraformingSystem.getCurrentSurfaceBiome(serverLevel, x, z), chunk, x, z);
                    }
                }

                // trigger neighbor to maybe run initial terraforming
                // run as tick task to avoid deadlocks
                MinecraftServer server = serverLevel.getServer();
                server.tell(new TickTask(server.getTickCount() + 100, () -> {
                    ChunkPos currentPos = chunk.getPos();
                    for (int x = -2; x <= 2; x++)
                        for (int z = -2; z <= 2; z++) {
                            maybePerformInitialTerraforming(planet, serverLevel, currentPos.x + x, currentPos.z + z);
                        }
                }));
            }
        }
    }

    public static double handleOceanCo2Reduction(PlanetDimension planet, boolean simulate) {
        // TODO: this should consider temperature
        // water will reduce co2 up to a target based on sea level
        // high temperature will make it hold less co2, but then we would have high humidity with plants
        // and plants would again absorb more co2, so i say temperature cancels out and use sea level only
        // this should result in about 0.3% target at 63 sea level
        // is not the thing that makes a planet habitable, but at least it reduces co2
        double oceanFractionWater = planet.getOceanFraction(GasRegistry.water);
        if (oceanFractionWater > 0.1 && planet.getGasProperty(GasRegistry.water).liquid > 0) {
            double targetCO2 = 0.001 / oceanFractionWater;
            PlanetDimensionProperties.GasProperty co2 = planet.getGasProperty(GasRegistry.co2);
            double diff = co2.in_atm - targetCO2;
            if (diff > 0.0001) {
                // absorb some co2. higher diff = higher rate
                // co2 will simply be "voided" since gas property can either be frozen or in atmosphere,
                // but not bound in rocks or ocean
                double toReduce = diff * Config.INSTANCE.planet_Sea_Lvl_Co2_Reduction_Factor;
                if (!simulate) {
                    co2.in_atm -= toReduce;
                    planet.setRequiresSync();
                }
            }
            return targetCO2;
        }
        return -1;
    }

    public static double handlePhotosynthesis(PlanetDimension planet, boolean simulate) {
        // if it is very warm and has sun light, algae will consume co2 and produce oxygen.
        double totalStarIntensity = 0;
        Vec3 planetPos = planet.getPosition(0);
        for (ResourceLocation targetId : planet.getCurrentMainStars()) {
            Dimension target = planet.dimensionManager.get(targetId);
            if (target == null) continue;
            Vec3 targetPosition = target.getPosition(0);
            double distanceToSqr = targetPosition.distanceToSqr(planetPos);
            double intensity = target.getRadiationIntensity() / distanceToSqr;
            totalStarIntensity += intensity;
        }
        if (totalStarIntensity < 0.5)
            // too little starlight for algae to work
            return 0;


        PlanetDimensionProperties.GasProperty co2 = planet.getGasProperty(GasRegistry.co2);
        double minCo2 = 0.0001;
        if (co2.in_atm < minCo2)
            // too little co2
            return 0;

        double oceanFractionWater = planet.getOceanFraction(GasRegistry.water);
        if (oceanFractionWater < 0.3)
            // too little oceans (temperature check below ensures we don't generate on frozen oceans)
            return 0;

        double sweetSpotForAlgae = 273.15 + 30;
        double maxTemperatureDeviationForAlgae = 15;
        double temperatureMultiplier = Math.max(0, 1 - Math.abs(sweetSpotForAlgae - planet.getCurrentTemp()) / maxTemperatureDeviationForAlgae);
        double photosynthesisValue = totalStarIntensity * temperatureMultiplier;
        if (photosynthesisValue > 0) {
            PlanetDimensionProperties.GasProperty o2 = planet.getGasProperty(GasRegistry.oxygen);
            double toReduce = photosynthesisValue * Config.INSTANCE.planet_Photosynthesis_Factor;
            toReduce = Math.min(toReduce, co2.in_atm - minCo2);
            if (!simulate) {
                co2.in_atm -= toReduce;
                o2.in_atm += toReduce;
                planet.setRequiresSync();
            }
            return photosynthesisValue;
        }

        return 0;
    }

    public static void tick(PlanetDimension planet, PlanetDimensionProperties properties, ServerLevel level) {
        handlePhotosynthesis(planet, false);

        handleOceanCo2Reduction(planet, false);

    }
}
