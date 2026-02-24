package ARLib.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class DimensionUtils {
    public static String getLevelId(Level level) {
        ResourceKey<Level> key = level.dimension();
        return key.location().toString();
    }

    public static Level getDimensionLevelServer(String dimensionId) {
        ResourceLocation location = ResourceLocation.parse(dimensionId);
        return ServerLifecycleHooks.getCurrentServer().getLevel(ResourceKey.create(Registries.DIMENSION, location));
    }
}
