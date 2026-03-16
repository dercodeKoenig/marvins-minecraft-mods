import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class API {


    public static boolean canBurn(ResourceLocation levelId, boolean isClientSide) {
        return (getDimension(levelId, isClientSide) instanceof Dimension dimension && dimension.hasEnoughOxygenToBurn());
    }

    public static int getAvailableGasMB(ResourceLocation levelId, String gasId, boolean isClientside) {
        if (getDimension(levelId, isClientside) instanceof PlanetDimension planet) {
            return (int) (planet.getGasProperty(gasId).in_atm / Config.INSTANCE.fluid_Contribution_To_Composition_Per_1000MB * 1000);
        }
        return 0;
    }

    public static void addGasMB(ResourceLocation levelId, String gasId, int mb, boolean isClientside) {
        if (getDimension(levelId, isClientside) instanceof PlanetDimension planet) {
            double toAdd = (double) mb / 1000 * Config.INSTANCE.fluid_Contribution_To_Composition_Per_1000MB;
            planet.getGasProperty(gasId).in_atm += toAdd;
        }
    }


    @Nullable
    public static Dimension getDimension(ResourceLocation levelId, boolean isClientSide) {
        return DimensionManager.getDimensionManager(isClientSide).get(levelId);
    }
}
