package advRocketry.mixins;

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

        // --- 1. Get dimension properties ---
        ResourceLocation dimensionId = level.dimension().location();
        DimensionProperties myProps = DimensionManager.INSTANCE.dimensions.get(dimensionId);
        // Exit if this dimension is not managed by the mod
        if (myProps == null || myProps.lightSourceDimensionId == null) return;

        DimensionProperties lightSourceProps = DimensionManager.INSTANCE.dimensions.get(myProps.lightSourceDimensionId);
        if (lightSourceProps == null) return;

        // --- 2. Prepare astronomical parameters ---
        // Vector from the star's center to the planet's center
        Vec3 starToPlanet = myProps.position.subtract(lightSourceProps.position);
        Vec3 rotationAxis = myProps.rotationAxis.normalize();

        // Get rotation and latitude in DEGREES, as per the new method's requirements
        double timeOfDayAngle = myProps.getSelfRotationDegrees(partialTick);
        double observerLatitude = 0.0;

        // On the client, get the actual player's latitude for accurate rendering
        if (level.isClientSide()) {
            observerLatitude = myProps.getLatitude(); // Assumes this method returns degrees
        }

        // NOTE: The planet's orbit angle is not needed here. Its effect is already included
        // in the 'starToPlanet' vector, which changes as the planet orbits.

        // --- 3. Calculate brightness using the new system ---
        // The method name "getSkyDarken" is misleading in vanilla; it actually returns sky BRIGHTNESS.
        // Our new method returns a raw brightness from 0.0 (dark) to 1.0 (bright).
        float astronomicalBrightness = AstronomicalLighting.calculateAstronomicalBrightness(
                starToPlanet, rotationAxis, timeOfDayAngle, observerLatitude
        );

        // --- 4. Apply weather dimming (vanilla logic) ---
        astronomicalBrightness *= 1.0F - level.getRainLevel(partialTick) * 5.0F / 16.0F;
        astronomicalBrightness *= 1.0F - level.getThunderLevel(partialTick) * 5.0F / 16.0F;

        // --- 5. Apply vanilla's final scaling ---
        // This maps our calculated brightness from [0.0, 1.0] to Minecraft's expected [0.2, 1.0] range,
        // ensuring there's always some ambient light, even at midnight.
        float finalSkyValue = astronomicalBrightness * 0.8F + 0.2F;

        // --- 6. Set the return value and cancel the original method ---
        cir.setReturnValue(finalSkyValue);
    }
}