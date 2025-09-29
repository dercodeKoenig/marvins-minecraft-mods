package advRocketry.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

import static advRocketry.skyrenderer.CUSTOM_SKY_DIMENSIONS;


@Mixin(DimensionSpecialEffects.OverworldEffects.class)
public class MixinOverworldEffects {
    // This controls the horizon fog color
    @Inject(method = "getBrightnessDependentFogColor", at = @At("HEAD"), cancellable = true)
    private void modifyHorizonColor(Vec3 fogColor, float brightness,
                                    CallbackInfoReturnable<Vec3> cir) {
        ResourceLocation loc = Minecraft.getInstance().level.dimension().location();

        // fogColor - default fog, probably biome specific. needs to be adjusted to match sky color

        if (CUSTOM_SKY_DIMENSIONS.contains(loc)) {
            // Make it match the sky color (dark blue) or black
            // Option 1: Full black
            //cir.setReturnValue(Vec3.ZERO);

            // Option 2: Match the actual fog color (removes gradient)
            // cir.setReturnValue(fogColor);

            // Option 3: Custom color (e.g., darker blue)
            cir.setReturnValue(fogColor.multiply(brightness,brightness,brightness));
        }
    }
}
