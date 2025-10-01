package advRocketry.mixins;

import advRocketry.AstronomicalLighting;
import advRocketry.DimensionManager;
import advRocketry.DimensionProperties;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelTimeAccess;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Shadow private int skyDarken;

    @Shadow public abstract boolean isClientSide();

    @Inject(method = "updateSkyBrightness", at = @At("HEAD"), cancellable = true)
    private void updateSkyBrightness(CallbackInfo ci) {
        Level level = (Level)(Object)this;

        ResourceLocation dimensionId =  level.dimension().location();
        DimensionProperties myProps = DimensionManager.INSTANCE.dimensions.get(dimensionId);
        DimensionProperties lightSourceProps = DimensionManager.INSTANCE.dimensions.get(myProps.lightSourceDimensionId);
        // Your astronomical parameters
        Vec3 starToPlanet = myProps.position.subtract(lightSourceProps.position);
        Vec3 rotationAxis = myProps.rotationAxis;
        double rotationAngle =  Math.toRadians(myProps.getSelfRotationDegrees(0));
        double observerLatitude = Math.toRadians(0); // default
        if (this.isClientSide()){
            // on client, use the players latitude for render. server will use 0 for logic ut client can still use its own latitude for render
            float latDegree = myProps.getLatitude();
            observerLatitude = Math.toRadians(latDegree);
        }


        this.skyDarken = AstronomicalLighting.calculateSkyDarken(
                starToPlanet, rotationAxis, rotationAngle, observerLatitude
        );

        ci.cancel();
    }


}