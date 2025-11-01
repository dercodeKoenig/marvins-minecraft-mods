package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    public void isRaining(CallbackInfoReturnable<Boolean> cir){
        Level level = (Level) (Object) this;
        ResourceLocation dimensionId = level.dimension().location();
        Dimension dimension = DimensionManager.get(dimensionId);
        if (dimension != null) {
            if (!dimension.canRain()){
                cir.setReturnValue(false);
            }
        }
    }

}