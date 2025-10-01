package advRocketry.mixins;// Make sure you have the correct imports for your project

import advRocketry.AstronomicalLighting;
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

        // --- 1. Get dimension properties (same as your original code) ---
        ResourceLocation dimensionId = level.dimension().location();
        DimensionProperties myProps = DimensionManager.INSTANCE.dimensions.get(dimensionId);
        if (myProps == null) return;

        DimensionProperties lightSourceProps = DimensionManager.INSTANCE.dimensions.get(myProps.lightSourceDimensionId);
        if (lightSourceProps == null) return;

        // --- 2. Prepare astronomical parameters (same as your original code) ---
        Vec3 starToPlanet = myProps.position.subtract(lightSourceProps.position);
        Vec3 rotationAxis = myProps.rotationAxis.normalize(); // Ensure axis is normalized
        double rotationAngle = Math.toRadians(myProps.getSelfRotationDegrees(partialTick));
        double observerLatitude = 0.0; // Default for server logic

        if (level.isClientSide()) {
            // On the client, get the player's latitude for more accurate rendering
            float latDegree = myProps.getLatitude(); // Assumes this method exists
            observerLatitude = Math.toRadians(latDegree);
        }

        // --- 3. Calculate brightness using the new system ---
        // It's important to note that getSkyDarken is misnamed in vanilla; it actually returns sky BRIGHTNESS.
        // Our new method returns a raw brightness from 0.0 (dark) to 1.0 (bright).
        float astronomicalBrightness = AstronomicalLighting.calculateAstronomicalBrightness(
                starToPlanet, rotationAxis, rotationAngle, observerLatitude
        );

        // --- 4. Apply weather dimming (adapted from vanilla logic) ---
        // Rain and thunder reduce the final brightness.
        astronomicalBrightness *= 1.0F - level.getRainLevel(partialTick) * 5.0F / 16.0F;
        astronomicalBrightness *= 1.0F - level.getThunderLevel(partialTick) * 5.0F / 16.0F;

        // --- 5. Apply vanilla's final scaling ---
        // This maps the brightness from [0.0, 1.0] to [0.2, 1.0], ensuring there's always
        // some ambient light, even at midnight.
        float finalSkyValue = astronomicalBrightness * 0.8F + 0.2F;

        // --- 6. Set the return value and cancel the original method ---
        cir.setReturnValue(finalSkyValue);
    }
}
