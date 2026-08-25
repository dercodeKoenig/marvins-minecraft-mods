package advRocketry;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class API {


    public static boolean canBurn(ResourceLocation levelId, boolean isClientSide) {
        return (getDimension(levelId, isClientSide) instanceof Dimension dimension && dimension.hasEnoughOxygenToBurn());
    }

    public static double getAvailableGasInBuckets(ResourceLocation levelId, String gasId, boolean isClientside) {
        if (getDimension(levelId, isClientside) instanceof PlanetDimension planet) {
            return planet.getGasProperty(gasId).in_atm * planet.getGravitationalMultiplier() / Config.INSTANCE.fluid_Contribution_To_Composition_Per_1000MB;
        }
        return 0;
    }

    public static void addGasInBuckets(ResourceLocation levelId, String gasId, double buckets) {
        if (getDimension(levelId, false) instanceof PlanetDimension planet) {
            double toAdd = buckets * Config.INSTANCE.fluid_Contribution_To_Composition_Per_1000MB / planet.getGravitationalMultiplier();
            planet.getGasProperty(gasId).in_atm += toAdd;
            planet.getGasProperty(gasId).in_atm = Math.max(0, planet.getGasProperty(gasId).in_atm);
            planet.setRequiresSync();

            System.out.println("API added gas buckets: " + gasId + ":" + buckets);
        }
    }

    public static double getAvailableLiquidInBuckets(ResourceLocation levelId, String gasId, boolean isClientside) {
        if (getDimension(levelId, isClientside) instanceof PlanetDimension planet) {
            return planet.getGasProperty(gasId).liquid * planet.getGravitationalMultiplier() / Config.INSTANCE.fluid_Contribution_To_Composition_Per_1000MB;
        }
        return 0;
    }

    public static void addLiquidInBuckets(ResourceLocation levelId, String gasId, double buckets) {
        if (getDimension(levelId, false) instanceof PlanetDimension planet) {
            double toAdd = buckets * Config.INSTANCE.fluid_Contribution_To_Composition_Per_1000MB / planet.getGravitationalMultiplier();
            planet.getGasProperty(gasId).liquid += toAdd;
            planet.getGasProperty(gasId).liquid = Math.max(0, planet.getGasProperty(gasId).liquid);
            planet.setRequiresSync();

            System.out.println("API added liquid buckets: " + gasId + ":" + buckets);
        }
    }

    public static double getAvailableSurfaceIceInBlocks(ResourceLocation levelId, String gasId, boolean isClientside) {
        if (getDimension(levelId, isClientside) instanceof PlanetDimension planet) {
            return planet.getGasProperty(gasId).frozen_surface * planet.getGravitationalMultiplier() / Config.INSTANCE.solid_Contribution_To_Composition_Per_Block;
        }
        return 0;
    }

    public static void addSurfaceIceInBlocks(ResourceLocation levelId, String gasId, double blocks) {
        if (getDimension(levelId, false) instanceof PlanetDimension planet) {
            double toAdd = blocks * Config.INSTANCE.solid_Contribution_To_Composition_Per_Block / planet.getGravitationalMultiplier();
            planet.getGasProperty(gasId).frozen_surface += toAdd;
            planet.getGasProperty(gasId).frozen_surface = Math.max(0, planet.getGasProperty(gasId).frozen_surface);
            planet.setRequiresSync();

            System.out.println("API added frozen blocks: " + gasId + ":" + blocks);
            //System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
        }
    }


    @Nullable
    public static Dimension getDimension(ResourceLocation levelId, boolean isClientSide) {
        return DimensionManager.getDimensionManager(isClientSide).get(levelId);
    }
}
