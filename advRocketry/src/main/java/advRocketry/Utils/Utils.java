package advRocketry.Utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class Utils {
    public static CompoundTag serializeVec3(Vec3 v) {
        CompoundTag Tag = new CompoundTag();
        if(v == null){
            Tag.putInt("null",0);
        }else {
            Tag.putDouble("x", v.x);
            Tag.putDouble("y", v.y);
            Tag.putDouble("z", v.z);
        }
        return Tag;
    }
    public static Vec3 deSerializeVec3(CompoundTag tag) {
        if(tag.contains("null"))return null;
        return new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
    }

    public static CompoundTag serializeVec3i(Vec3i v) {
        CompoundTag Tag = new CompoundTag();
        if(v == null){
            Tag.putInt("null",0);
        }else {
            Tag.putInt("x", v.getX());
            Tag.putInt("y", v.getY());
            Tag.putInt("z", v.getZ());
        }
        return Tag;
    }
    public static Vec3i deSerializeVec3i(CompoundTag tag) {
        if(tag.contains("null"))return null;
        return new Vec3i(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    public static Holder<Biome> getBiomeHolder(String biomeId) {
        return ServerLifecycleHooks.getCurrentServer().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(biomeId)));
    }

}
