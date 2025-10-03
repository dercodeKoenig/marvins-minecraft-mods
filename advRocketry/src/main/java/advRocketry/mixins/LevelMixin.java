package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.utils.CelestialUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class LevelMixin {

    // getSkyDarken is used by the built-in terrain shader. It has to use a mixin to overwrite terrain color based on current brightness
    @Inject(method = "getSkyDarken", at = @At("HEAD"), cancellable = true)
    public void getSkyDarken(float partialTick, CallbackInfoReturnable<Float> cir) {
        Level level = (Level)(Object)this;

        ResourceLocation dimensionId = level.dimension().location();
        Dimension dimension = DimensionManager.get(dimensionId);

        double brightness = dimension.getAccumulatedBrightness(partialTick, null);

        brightness *= 1.0F - level.getRainLevel(partialTick) * 5.0F / 16.0F;
        brightness *= 1.0F - level.getThunderLevel(partialTick) * 5.0F / 16.0F;

        double finalSkyValue = brightness * 0.8F + 0.2F;

        cir.setReturnValue((float) finalSkyValue);
    }
}