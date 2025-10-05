package advRocketry.Dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static java.lang.Math.pow;


public class DimensionProperties {

    public ResourceLocation dimensionId = null;             // required
    public double earthRadiusMultiplier = 1;
    public double earthMassMultiplier = 1;
    public Vec3 position = new Vec3(0, 0, 0);
    public Vec3 rotationAxis = new Vec3(0, 1, 0);
    public int targetDayLength = 24000;

    public ResourceLocation parentDimensionId = null;       // optional, overwrites position
    public Vec3 orbitAxis = new Vec3(0, 1, 0);
    public double orbitalDistanceToParent = 1;
    public double orbitalBaseOffsetDegrees = 0;

    public ResourceLocation lightSourceDimensionId = null;  // required (reference for day start)

    public ResourceLocation texture = null;                 // required (planet texture)

    public Vector3f skyColor = new Vector3f(0.471f, 0.655f, 1.0f);
    public Vector3f sunRiseColor = new Vector3f(0.471f, 0.655f, 0.2f);
    public Vector3f fogColor = new Vector3f(1.0f, 1.0f, 1.0f);
    public Vector4f emissiveColor = new Vector4f(0, 0, 0, 0);
    public float reflectivity = 1f;
    public float atmosphereDensity = 1;

    public int latitude_len = 200000;                                        // how much you have to move in z direction to "go around the planet"

    public float dayTime;

    public DimensionProperties(ResourceLocation dimensionId) {
        this.dimensionId = dimensionId;
    }

    public static float hdr(float ldr) {
        float ldr_lin = (float) pow(ldr, 2.2);
        //float hdr = ldr_lin/(1.0001f-ldr_lin);
        return ldr_lin;
    }
}
