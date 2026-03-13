package advRocketry.Dimension;

import advRocketry.Blocks.DryIceBlock;
import advRocketry.GlobalTime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

public class PlanetEvents {
    public static void tickTemperatureEvents(PlanetDimension planet, PlanetDimensionProperties properties) {
        // slowly reduced target sea level while too hot
        // water will simply be voided, it is way too complicated to handle it in atm
        // because it would heavily interfere with player placed water and would not allow a sea level changing satellite
        if (properties.currentTemp > 375) {
            if (Math.random() < 0.1 && properties.seaLevel > 0)
                properties.seaLevel--;
        }


        // evaporate / snow down gases
        double temp = properties.currentTemp;
        double gasTransitionSpeed = 0.000005;
        for (String id : GasRegistry.gases.keySet()) {
            GasRegistry.Gas gas = GasRegistry.gases.get(id);
            PlanetDimensionProperties.GasProperty property = planet.getGasProperty(id);
            if (property.in_atm > 0) {
                if (gas.freezingTemp > temp) {
                    // snow down some gas to surface
                    // slower when larger planet, faster when more gas in atmosphere
                    float toSnow = (float) (gasTransitionSpeed / (1 + planet.getGravitationalMultiplier()) * (1 + property.in_atm));
                    toSnow = Math.min(property.in_atm, toSnow);
                    property.in_atm -= toSnow;
                    property.frozen_surface += toSnow;
                }
            }
            if (gas.sublimationTemp < temp) {
                // gas goes up into the air
                if (property.frozen_surface > 0) {
                    float toTransfer = (float) (gasTransitionSpeed / (1 + planet.getGravitationalMultiplier()));
                    toTransfer = Math.min(property.frozen_surface, toTransfer);
                    property.in_atm += toTransfer;
                    property.frozen_surface -= toTransfer;
                }
                if (property.frozen_deep_below_surface > 0) {
                    float toTransfer = (float) (gasTransitionSpeed / (1 + planet.getGravitationalMultiplier()));
                    toTransfer = Math.min(property.frozen_deep_below_surface, toTransfer);
                    property.in_atm += toTransfer;
                    property.frozen_deep_below_surface -= toTransfer;
                }
            }
        }
    }

    // called from server level mixin
    public static void performRandomTickEvents(PlanetDimension planet, ServerLevel level, LevelChunk chunk){

        // pick a random position to work
        ChunkPos chunkPos = chunk.getPos();
        int blockX = chunkPos.getBlockX(level.random.nextInt(16));
        int blockZ = chunkPos.getBlockZ(level.random.nextInt(16));
        int blockY = level.random.nextIntBetweenInclusive(level.getMinBuildHeight(), level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ));

        // places dry ice if there is lots of frozen co2 on surface
        // should take way below < 1ms to check with 4 checks a tick on average, probably even lower when nothing to do
        if(level.random.nextInt(100) == 0) {
            DryIceBlock.placeDryIceIfPossible(planet, blockX, blockZ);
        }



    }

    public static void tick(PlanetDimension planet, PlanetDimensionProperties properties, ServerLevel level){
        tickTemperatureEvents(planet, properties);
    }
}
