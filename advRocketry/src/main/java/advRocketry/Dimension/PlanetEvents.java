package advRocketry.Dimension;

import advRocketry.Blocks.DryIceBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

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
        for (String id : GasRegistry.gases.keySet()) {
            GasRegistry.Gas gas = GasRegistry.gases.get(id);
            PlanetDimensionProperties.GasProperty property = planet.getGasProperty(id);
            if (property.in_atm > 0) {
                if (gas.freezingTemp > temp) {
                    // snow down some gas to surface
                    // slower when larger planet, faster when more gas in atmosphere
                    float toSnow = (float) (0.00001 / (1 + planet.getGravitationalMultiplier()) * (1 + property.in_atm));
                    toSnow = Math.min(property.in_atm, toSnow);
                    property.in_atm -= toSnow;
                    property.frozen_surface += toSnow;
                }
            }
        }
    }

    // called from server level mixin
    public static void performRandomTickEvents(PlanetDimension planet, ServerLevel level, LevelChunk chunk){
        // pick a random xz position
        ChunkPos chunkPos = chunk.getPos();
        int blockX = chunkPos.getBlockX(level.random.nextInt() % 16);
        int blockZ = chunkPos.getBlockZ(level.random.nextInt() % 16);

        DryIceBlock.placeDryIceIfPossible(planet, blockX, blockZ);

    }

    public static void tick(PlanetDimension planet, PlanetDimensionProperties properties, ServerLevel level){
        tickTemperatureEvents(planet, properties);
    }
}
