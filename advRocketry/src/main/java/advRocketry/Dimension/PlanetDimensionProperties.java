package advRocketry.Dimension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;


public class PlanetDimensionProperties extends DimensionProperties{
    // TODO: add always rain / always thunder values or custom rain times

    public PlanetDimensionProperties(){
        this.type = DimensionType.PLANET;
    }

    public Vec3 position = new Vec3(0, 0, 0);

    public boolean hasCustomSky = true;

    public float gravitationalMultiplier = 1;

    public float earthRadiusMultiplier = 1;

    public Vec3 rotationAxis = new Vec3(0.2, 1, 0);

    public int seaLevel = 63;

    public boolean generateStructures = false;

    public ResourceLocation parentDimensionId = null;       // optional, overwrites position

    public Vec3 orbitAxis = new Vec3(0, 1, 0);

    public float orbitalDistanceToParent = 1;

    public float orbitalBaseOffsetDegrees = 0;

    public ResourceLocation dayTimeReference = null;  // required reference for day start

    public ResourceLocation texture = null;

    public Vector3f skyColor = new Vector3f(0.45f, 0.7f, 1f);

    public Vector3f fogColor = new Vector3f(0.89f, 0.95f, 1.0f);

    public Vector3f sunRiseColor = new Vector3f(3f, 2f, 0.1f);

    public Vector3f reflectiveTextureTintColor = new Vector3f(1f, 1f, 1f); // TODO: include this in shader

    public Vector3f emissiveColor = new Vector3f(0, 0, 0); // the color that the planet radiates with

    public float radiationIntensity; // radiation strength, can be used for terrain shading, temperature calculation and maybe more stuff

    public float atmosphereDensity = 1;

    public int latitude_len = 400000;// how much you have to move in z direction to "go around the planet" 0% = equator, 25% = South Pole, 50% = equator again, 75% = North Pole

    public int targetDayLength = 24000; // set negative for fixed time

    public float dayTime;

    public boolean isKnown = true; // TODO: planet discovery with telescope / satellites

}
