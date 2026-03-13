package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.PlanetEvents;
import advRocketry.Utils.ClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickChunk", at = @At("HEAD"))
    public void onTickChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerLevel serverLevel = (ServerLevel) (Object) this;
        if (DimensionManager.INSTANCE_SERVER.get(serverLevel.dimension().location()) instanceof PlanetDimension planet) {
            PlanetEvents.performRandomTickEvents(planet, serverLevel, chunk);
        }
    }
}