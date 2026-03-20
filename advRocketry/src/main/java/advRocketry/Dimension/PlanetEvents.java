package advRocketry.Dimension;

import advRocketry.Blocks.DryIceBlock;
import advRocketry.Config;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.level.ChunkEvent;

public class PlanetEvents {

    // called from server level mixin
    public static void performTerraformingTicks(PlanetDimension planet, ServerLevel level, LevelChunk chunk) {

        ChunkPos chunkPos = chunk.getPos();

        int speed = 1;

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

            // spawn possible dry ice blocks
            DryIceBlock.placeDryIceIfPossible(planet, blockX, blockZ, 3);

            // adjust sea level for all the gases
            for (GasRegistry.Gas gas : GasRegistry.gases.values()) {
                if (SeaLevelAdjustment.adjustSeaLevelIfRequired(planet, gas, blockX, blockZ, 3))
                    break; // avoid gas mixing if many gases exist
            }
        }
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && DimensionManager.INSTANCE_SERVER.get(serverLevel.dimension().location()) instanceof PlanetDimension planet) {
            // perform some terraforming checks and replacement rules on new chunks

            // placement flags:
            // 2  -> sync to player
            // 16 -> no neighbor update (if i read it correctly)

            double planetTemp = planet.getCurrentTemp();
            double atmLevel = planet.getAtmosphereDensity();
            boolean shouldFreezeWater = planetTemp < GasRegistry.gases.get(GasRegistry.water).getFreezeTemp(atmLevel) - 1;

            if (event.isNewChunk()) {
                long t0 = System.nanoTime();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int xB = event.getChunk().getPos().getBlockX(x);
                        int zB = event.getChunk().getPos().getBlockZ(z);


                        // TODO: place custom sea block here before everything else


                        // adjust sea levels
                        SeaLevelAdjustment.saveInitialWaterLevelOnChunkGeneration(serverLevel, event.getChunk(), xB, zB);
                        for (GasRegistry.Gas gas : GasRegistry.gases.values()) {
                            while (SeaLevelAdjustment.adjustSeaLevelIfRequired(planet, gas, xB, zB, 2 | 16)) {
                                continue; // nothing to do, all the action happens above
                            }
                        }

                        // place dry ice
                        while (DryIceBlock.placeDryIceIfPossible(planet, xB, zB, 2 | 16)) {
                            continue; // nothing to do, all the action happens above
                        }


                        // after sea level adjustment, maybe freeze water or do other actions
                        for (int y = serverLevel.getMinBuildHeight(); y < serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, xB, zB); y++) {
                            BlockPos pos = new BlockPos(xB, y, zB);
                            BlockState state = serverLevel.getBlockState(pos);

                            // freeze water if possible, after the sea level is adjusted
                            if (state.getBlock().equals(net.minecraft.world.level.block.Blocks.WATER) && shouldFreezeWater) {
                                serverLevel.setBlock(pos, net.minecraft.world.level.block.Blocks.ICE.defaultBlockState(), 2 | 16);
                            }
                        }
                    }
                }
                //System.out.println("block replacement on chunk load: " + (double) (System.nanoTime() - t0) / 1000 / 1000);
            }
        }
    }

    public static double handleOceanCo2Reduction(PlanetDimension planet, boolean simulate) {
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

        // if it is very warm, algae will consume co2 and produce oxygen.
        // this process should significantly slow down as it gets cold and cut off long before freezing point
        // to prevent taking all co2 from the atmosphere and causing a freeze
        PlanetDimensionProperties.GasProperty co2 = planet.getGasProperty(GasRegistry.co2);
        double oceanFractionWater = planet.getOceanFraction(GasRegistry.water);
        if (oceanFractionWater > 0.1 && planet.getGasProperty(GasRegistry.water).liquid > 0 && co2.in_atm > 0.0001001) {
            double sweetSpotForAlgae = 273.15 + 30;
            double maxTemperatureDeviationForAlgae = 15;
            double photosynthesisValue = 1 - Math.abs(sweetSpotForAlgae - planet.getCurrentTemp()) / maxTemperatureDeviationForAlgae;
            if (photosynthesisValue > 0) {
                PlanetDimensionProperties.GasProperty o2 = planet.getGasProperty(GasRegistry.oxygen);
                double toReduce = photosynthesisValue * Config.INSTANCE.planet_Photosynthesis_Factor;
                toReduce = Math.min(toReduce, co2.in_atm - 0.0001);
                if (!simulate) {
                    co2.in_atm -= toReduce;
                    o2.in_atm += toReduce;
                    planet.setRequiresSync();
                }
                return photosynthesisValue;
            }
        }
        return 0;
    }

    public static void tick(PlanetDimension planet, PlanetDimensionProperties properties, ServerLevel level) {

        handlePhotosynthesis(planet, false);

        handleOceanCo2Reduction(planet, false);

    }
}
