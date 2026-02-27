package advRocketry.Dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;


public class SpaceStationDimensionProperties extends DimensionProperties {

    public Vec3 position = new Vec3(0, 0, 0);

    public ResourceLocation parentDimensionId = null; // when in orbit

    public float orbitDistanceTarget = 0.001f;// when in orbit

    // the space station owner, he should be able to have ways to return to station without id chip
    public UUID owner = null;

    public boolean initialBlocksPlaced = false; // was the station container placed already?

    public SpaceStationDimensionProperties() {
        this.type = DimensionType.SPACE_STATION;
    }

    // to calculate global axis direction, rotate the front vector around the  y rotation of the stations facing direction?

}
