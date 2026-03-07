package advRocketry.Utils;

import net.minecraft.world.phys.Vec3;

public class AxisDirections {
    public AxisDirections(Vec3 front, Vec3 up){
        this.up = up;
        this.front = front;
    }
    public Vec3 front;
    public Vec3 up;
}
