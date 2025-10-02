package advRocketry.mixins;

import advRocketry.CelestialUtils;
import advRocketry.DimensionManager;
import advRocketry.DimensionProperties;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class LevelMixin {

    @Inject(method = "getSkyDarken", at = @At("HEAD"), cancellable = true)
    public void getSkyDarken(float partialTick, CallbackInfoReturnable<Float> cir) {
        Level level = (Level)(Object)this;

        // --- 1. Get dimension properties ---
        ResourceLocation dimensionId = level.dimension().location();
        DimensionProperties myProps = DimensionManager.INSTANCE.dimensions.get(dimensionId);
        // Exit if this dimension is not managed by the mod
        if (myProps == null || myProps.lightSourceDimensionId == null) return;

        DimensionProperties lightSourceProps = DimensionManager.INSTANCE.dimensions.get(myProps.lightSourceDimensionId);
        if (lightSourceProps == null) return;

        double astronomicalBrightness = CelestialUtils.getSurfaceDotToPlanet(
                myProps, lightSourceProps,partialTick
        );

        astronomicalBrightness *= 1.0F - level.getRainLevel(partialTick) * 5.0F / 16.0F;
        astronomicalBrightness *= 1.0F - level.getThunderLevel(partialTick) * 5.0F / 16.0F;

        double finalSkyValue = astronomicalBrightness * 0.8F + 0.2F;

        cir.setReturnValue((float) finalSkyValue);
    }
}