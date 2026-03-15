package advRocketry.Dimension;

import advRocketry.Blocks.DryIceBlock;
import advRocketry.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public class PlanetEvents {

    public static void handleDimensionTransfer() {
        // should be called by rocket on teleport / railgun when it carries items during teleport
        // scan all items / fluids and add / remove atm composition on entry / exit
        // for itemstacks, check if the itemstack is a fluidhandler containing fluid or maybe an itemhandler and scan recursive
        //new ItemStack().getCapability(Capabilities.ItemHandler.ITEM)

    }

    // called from server level mixin
    public static void performRandomTickEvents(PlanetDimension planet, ServerLevel level, LevelChunk chunk) {

        // pick a random position to work
        ChunkPos chunkPos = chunk.getPos();
        int blockX = chunkPos.getBlockX(level.random.nextInt(16));
        int blockZ = chunkPos.getBlockZ(level.random.nextInt(16));

        int worldSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
        int blockY = level.random.nextIntBetweenInclusive(worldSurface - 10, worldSurface);

        // places dry ice if there is lots of frozen co2 on surface
        // should take way below < 1ms to check with 4 checks a tick on average, probably even lower when nothing to do
        if (level.random.nextInt(100) == 0) {
            DryIceBlock.placeDryIceIfPossible(planet, blockX, blockZ);
        }
    }

    public static boolean boilWaterWhenTooHot(PlanetDimension planet, PlanetDimensionProperties properties, boolean simulate) {
        // slowly reduced target sea level while too hot
        // water will simply be voided, it is way too complicated to handle it in atm
        // because it would heavily interfere with player placed water and would not allow a sea level changing satellite
        if (planet.getCurrentTemp() > 375) {
            if (!simulate) {
                properties.seaLevel -= 0.001;
                planet.setRequiresSync();
            }
            return true;
        }
        return false;
    }

    public static void handleGasFreezeAndSublimation(PlanetDimension planet, PlanetDimensionProperties properties) {

        // evaporate / snow down gases
        double temp = properties.currentTemp;

        for (String id : GasRegistry.gases.keySet()) {
            GasRegistry.Gas gas = GasRegistry.gases.get(id);
            PlanetDimensionProperties.GasProperty property = planet.getGasProperty(id);
            if (property.in_atm > 0) {
                if (gas.freezingTemp > temp) {
                    // snow down some gas to surface
                    // slower when larger planet, faster when more gas in atmosphere
                    double toSnow = (Config.INSTANCE.gas_Atm_Ground_Transition_Speed / (1 + planet.getGravitationalMultiplier()) * (1 + property.in_atm));
                    toSnow = Math.min(property.in_atm, toSnow);
                    property.in_atm -= toSnow;
                    property.frozen_surface += toSnow;
                    planet.setRequiresSync();
                }
            }
            if (gas.sublimationTemp < temp) {
                // gas goes up into the air
                if (property.frozen_surface > 0) {
                    double toTransfer = (Config.INSTANCE.gas_Atm_Ground_Transition_Speed / (1 + planet.getGravitationalMultiplier()));
                    toTransfer = Math.min(property.frozen_surface, toTransfer);
                    property.in_atm += toTransfer;
                    property.frozen_surface -= toTransfer;
                    planet.setRequiresSync();
                }
                if (property.frozen_deep_below_surface > 0) {
                    // deep below surface transfers slower
                    double toTransfer = 0.05 * (Config.INSTANCE.gas_Atm_Ground_Transition_Speed / (1 + planet.getGravitationalMultiplier()));
                    toTransfer = Math.min(property.frozen_deep_below_surface, toTransfer);
                    property.in_atm += toTransfer;
                    property.frozen_deep_below_surface -= toTransfer;
                    planet.setRequiresSync();
                }
            }
        }
    }

    public static double handleOceanCo2Reduction(PlanetDimension planet, boolean simulate) {
        // water will reduce co2 up to a target based on sea level
        // high temperature will make it hold less co2, but then we would have high humidity with plants
        // and plants would again absorb more co2, so i say temperature cancels out and use sea level only
        // this should result in about 3% target at 63 sea level
        // is not the thing that makes a planet habitable, but at least it reduces co2
        if (planet.getOceanFraction() > 0.1 && planet.warmEnoughForWater()) {
            double targetCO2 = 0.01 / (planet.getOceanFraction());
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
                return targetCO2;
            }
        }
        return -1;
    }

    public static double handlePhotosynthesis(PlanetDimension planet, boolean simulate) {

        // if it is very warm, algae will consume co2 and produce oxygen.
        // this process should significantly slow down as it gets cold and cut off long before freezing point
        // to prevent taking all co2 from the atmosphere and causing a freeze
        PlanetDimensionProperties.GasProperty co2 = planet.getGasProperty(GasRegistry.co2);
        if (planet.getOceanFraction() > 0.1 && planet.warmEnoughForWater() && co2.in_atm > 0) {
            double sweetSpotForAlgae = 273.15 + 30;
            double maxTemperatureDeviationForAlgae = 15;
            double photosynthesisValue = 1 - Math.abs(sweetSpotForAlgae - planet.getCurrentTemp()) / maxTemperatureDeviationForAlgae;
            if (photosynthesisValue > 0) {
                PlanetDimensionProperties.GasProperty o2 = planet.getGasProperty(GasRegistry.oxygen);
                double toReduce = photosynthesisValue * Config.INSTANCE.planet_Photosynthesis_Factor;
                toReduce = Math.min(toReduce, co2.in_atm);
                if(!simulate) {
                    co2.in_atm -= toReduce;
                    o2.in_atm += toReduce;
                    planet.setRequiresSync();
                }
                return photosynthesisValue;
            }
        }
        return 0;
    }

    public static void adjustWorldgenSeaLevelIfRequired(PlanetDimension planet, PlanetDimensionProperties properties){
        // do not use round, i want some significant change before i adjust worldgen sea level
        if(Math.abs(properties.seaLevel - properties.seaLevelWorldgen) > 0.6){
            properties.seaLevelWorldgen = (int) Math.round(properties.seaLevel);
            planet.setRequiresSync();
        }
    }

    public static void tick(PlanetDimension planet, PlanetDimensionProperties properties, ServerLevel level) {

        boilWaterWhenTooHot(planet, properties, false);

        handleGasFreezeAndSublimation(planet, properties);

        handlePhotosynthesis(planet, false);

        handleOceanCo2Reduction(planet, false);

        adjustWorldgenSeaLevelIfRequired(planet, properties);

    }
}
