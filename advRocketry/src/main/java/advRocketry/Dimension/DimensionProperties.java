package advRocketry.Dimension;

import net.minecraft.resources.ResourceLocation;


public class DimensionProperties {

    public String name = "";
    public DimensionType type = null;
    public ResourceLocation dimensionId = null;

    public enum DimensionType {
        PLANET,
        SPACE_STATION,
        ROCKET_TRAVEL
    }
}
