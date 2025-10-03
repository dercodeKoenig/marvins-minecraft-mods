package advRocketry.utils;

import net.minecraft.world.phys.Vec3;

public class AxisDirections {
    public AxisDirections(Vec3 north, Vec3 east, Vec3 up){
        this.north = north;
        this.up = up;
        this.east = east;
    }
    public Vec3 north;
    public Vec3 east;
    public Vec3 up;
}
