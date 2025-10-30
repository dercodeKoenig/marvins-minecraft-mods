package advRocketry.utils;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public class Utils {
    public static CompoundTag serializeVec3(Vec3 v) {
        CompoundTag Tag = new CompoundTag();
        Tag.putDouble("x", v.x);
        Tag.putDouble("y", v.y);
        Tag.putDouble("z", v.z);
        return Tag;
    }
    public static Vec3 deSerializeVec3(CompoundTag tag) {
        return new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
    }

    public static CompoundTag serializeVec3i(Vec3i v) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("x", v.getX());
        Tag.putInt("y", v.getY());
        Tag.putInt("z", v.getZ());
        return Tag;
    }
    public static Vec3i deSerializeVec3i(CompoundTag tag) {
        return new Vec3i(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }
}
