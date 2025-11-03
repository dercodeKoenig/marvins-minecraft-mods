package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    // getSkyDarken is used by the built-in terrain shader. It has to use a mixin to overwrite terrain color based on current brightness
    @Inject(method = "getSkyDarken", at = @At("HEAD"), cancellable = true)
    public void getSkyDarken(float partialTick, CallbackInfoReturnable<Float> cir) {
        Level level = (Level) (Object) this;
        ResourceLocation dimensionId = level.dimension().location();
        Dimension dimension = DimensionManager.get(dimensionId);
        double brightness;
        if (dimension != null) { // registered in DimensionManager
            brightness = dimension.getAccumulatedWorldBrightness(partialTick, 0.2f, null);
            // just some adjustments because it looks better. make it change dark to bright faster and stay bright for longer
            brightness = Math.clamp(Math.pow(brightness, 0.8) * 1, 0, 1);
        } else {
            // original code
            float f = level.getTimeOfDay(partialTick);
            brightness = 1.0F - (Mth.cos(f * ((float) Math.PI * 2F)) * 2.0F + 0.2F);
            brightness = Mth.clamp(brightness, 0.0F, 1.0F);
            brightness = 1.0F - brightness;
        }

        brightness *= 1.0F - level.getRainLevel(partialTick) * 5.0F / 16.0F;
        brightness *= 1.0F - level.getThunderLevel(partialTick) * 5.0F / 16.0F;

        double finalSkyValue = brightness * 0.9F + 0.1F; // original is *0.8+0.2

        cir.setReturnValue((float) finalSkyValue);
        cir.cancel();
    }


    @Inject(method = "getCloudColor", at = @At("HEAD"), cancellable = true)
    public void getCloudColorOverwrite(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Level level = (Level) (Object) this;
        ResourceLocation dimensionId = level.dimension().location();
        Dimension dimension = DimensionManager.get(dimensionId);
        double brightness;
        if (dimension != null) { // registered in DimensionManager
            brightness = dimension.getAccumulatedWorldBrightness(partialTick, 0.4f, null);
            // just some adjustments because it looks better. make it change dark to bright faster and stay bright for longer
            brightness = Math.clamp(Math.pow(brightness, 0.8) * 1, 0, 1);
        }else{
            float f = level.getTimeOfDay(partialTick);
            brightness = Mth.cos(f * ((float)Math.PI * 2F)) * 2.0F + 0.5F;
        }

        float f1 = (float) brightness;
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        float f2 = 1.0F;
        float f3 = 1.0F;
        float f4 = 1.0F;
        float f5 = level.getRainLevel(partialTick);
        if (f5 > 0.0F) {
            float f6 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.6F;
            float f7 = 1.0F - f5 * 0.95F;
            f2 = f2 * f7 + f6 * (1.0F - f7);
            f3 = f3 * f7 + f6 * (1.0F - f7);
            f4 = f4 * f7 + f6 * (1.0F - f7);
        }

        f2 *= f1 * 0.9F + 0.1F;
        f3 *= f1 * 0.9F + 0.1F;
        f4 *= f1 * 0.85F + 0.15F;
        float f9 = level.getThunderLevel(partialTick);
        if (f9 > 0.0F) {
            float f10 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.2F;
            float f8 = 1.0F - f9 * 0.95F;
            f2 = f2 * f8 + f10 * (1.0F - f8);
            f3 = f3 * f8 + f10 * (1.0F - f8);
            f4 = f4 * f8 + f10 * (1.0F - f8);
        }

        cir.setReturnValue(new Vec3((double) f2, (double) f3, (double) f4));
        cir.cancel();
    }

}