package advRocketry.Dimension;

import net.minecraft.resources.ResourceLocation;


public class DimensionProperties {

    public String name = "";
    public DimensionType type = DimensionType.DUMMY;
    public ResourceLocation dimensionId = null;

    public enum DimensionType {
        PLANET,
        SPACE_STATION,
        DUMMY,
        ROCKET_TRAVEL
    }
}
