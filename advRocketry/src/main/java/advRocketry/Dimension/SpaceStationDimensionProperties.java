package advRocketry.Dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;


public class SpaceStationDimensionProperties extends DimensionProperties{

    public SpaceStationDimensionProperties(){
        this.type = DimensionType.SPACE_STATION;
    }

    public Vec3 position = new Vec3(0, 0, 0);

    // based on target position, the new target position is calculated during orbit
    // position tries to close up to target position while it maintains a minimum distance to other planets
    // every tick we can calculate the orbit speed and move the target position orthogonal to planet vector
    // then we scale the targetposition along the planet-target vector to keep the target orbit distance
    // every few seconds this target position and orbit distance can be synced, client will perform same logic
    // targetposition = targetposition + cross(orbitaxis, planetvector) * orbitspeed
    // targetposition = planetposition + (targetposition - planetposition).norm.scale(distance)
    // position = position + (targetposition - position) * 0.1 or similar
    public Vec3 targetPosition = new Vec3(0, 0, 0);

    public ResourceLocation parentDimensionId = null;       // optional, overwrites position

    public float orbitalDistanceToParent = 1;

}
