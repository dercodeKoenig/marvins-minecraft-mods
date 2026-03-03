package advRocketry.Dimension;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;


public class SpaceStationDimensionProperties extends DimensionProperties {

    // for more smooth movements, some variables exist 3 times:
    // target Vectors = the vector it wants to reach, saved in properties
    // normal Vectors = the vector it currently has, also saved in properties
    // lazy Vectors = interpolates toward normal vector, instance variable of dimension class, not saved or synced

    // target position is calculated from parentDimensionId
    // lazy value for smooth movement defined in dimension class
    public Vec3 position = new Vec3(0, 0, 0);

    // the planet it currently orbits
    public ResourceLocation parentDimensionId = null;
    // the planet it was in orbit before
    public ResourceLocation lastParentDimensionId = null;

    // 0 - 1, how far away from planet to orbit
    public float orbitDistanceTarget = 0.001f;

    // the axis we orbit around
    public Vec3 orbitAxisTarget = new Vec3(0, 1, 0);

    // TODO: move to dimension class, replace with target YRot & XRot to use when not in space travel
    public Vec3 targetFront = new Vec3(0, 0, 1);
    public Vec3 front = new Vec3(0, 0, 1);
    public Vec3 targetUp = new Vec3(0, 1, 0);
    public Vec3 up = new Vec3(0, 1, 0);

    // the space station owner, he should be able to have ways to return to station without id chip
    public UUID owner = null;

    // was the station container placed already?
    public boolean initialBlocksPlaced = false;
    // has the station received its initial position?
    public boolean positionInitialized = false;

    // this is the front direction of the station
    public Direction frontFacing = Direction.NORTH;

    // rotation settings
    public double yaw = 0;
    public double roll = 0;
    public double pitch = 0;
    public RotationMode rotationMode = RotationMode.disabled;

    public SpaceStationDimensionProperties() {
        this.type = DimensionType.SPACE_STATION;
    }

    public enum RotationMode {
        disabled,
        relative,
        absolute
    }


}
