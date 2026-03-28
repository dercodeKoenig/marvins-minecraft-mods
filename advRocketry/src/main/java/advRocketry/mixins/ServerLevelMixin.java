package advRocketry.mixins;

import advRocketry.Dimension.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickChunk", at = @At("HEAD"))
    public void onTickChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerLevel serverLevel = (ServerLevel) (Object) this;
        if (DimensionManager.INSTANCE_SERVER.get(serverLevel.dimension().location()) instanceof Dimension dimension) {
            DimensionEvents.performRandomTickEvents(dimension, serverLevel, chunk);
        }

        if (DimensionManager.INSTANCE_SERVER.get(serverLevel.dimension().location()) instanceof PlanetDimension planet) {
            PlanetEvents.performTerraformingTicks(planet, serverLevel, chunk);
        }
    }
}