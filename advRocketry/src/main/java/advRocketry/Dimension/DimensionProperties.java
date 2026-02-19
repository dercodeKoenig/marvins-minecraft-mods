package advRocketry.Dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;


public class DimensionProperties {

    public String name = "";
    public DimensionType type = DimensionType.DUMMY;
    public ResourceLocation dimensionId = null;

    public enum DimensionType {
        PLANET,
        SPACE_STATION,
        DUMMY;
    }
}
