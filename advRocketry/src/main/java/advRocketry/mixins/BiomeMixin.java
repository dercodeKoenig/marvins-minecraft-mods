package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeMixin {

    @Inject(method="shouldSnow",
            at = @At("HEAD"),
        cancellable = true)
    public void shouldSnow(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        if (level instanceof ServerLevel serverLevel && DimensionManager.INSTANCE_SERVER.get(serverLevel.dimension().location()) instanceof Dimension dimension) {
            if(!dimension.canRain()) {
                // if it can not rain, it can not snow!
                ci.setReturnValue(false);
                ci.cancel();
            }
        }
    }

    @Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("HEAD"),
            cancellable = true)
    public void shouldFreeze(LevelReader level, BlockPos water, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> ci) {
        if (level instanceof ServerLevel serverLevel && DimensionManager.INSTANCE_SERVER.get(serverLevel.dimension().location()) instanceof Dimension dimension) {
            GasRegistry.Gas waterGas = GasRegistry.gases.get(GasRegistry.water);
            if(LifeSupportSystem.isTemperatureRegulated(serverLevel,water)){
                // regulated temperature does not form ice
                ci.setReturnValue(false);
                ci.cancel();
            }
            else if (dimension.getCurrentTemp() > waterGas.getBoilingTemp(dimension.getAtmosphereDensity())) {
                // too hot for any ice, even in frozen biomes
                ci.setReturnValue(false);
                ci.cancel();
            }
            else if (dimension.getCurrentTemp() < waterGas.getFreezeTemp(dimension.getAtmosphereDensity())) {
                // cold enough to force freeze
                // the default code of Biome class follows:
                if (water.getY() >= level.getMinBuildHeight() && water.getY() < level.getMaxBuildHeight()) {
                    BlockState blockstate = level.getBlockState(water);
                    FluidState fluidstate = level.getFluidState(water);
                    if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
                        if (!mustBeAtEdge) {
                            ci.setReturnValue(true);
                            ci.cancel();
                        }

                        boolean flag = level.isWaterAt(water.west()) && level.isWaterAt(water.east()) && level.isWaterAt(water.north()) && level.isWaterAt(water.south());
                        if (!flag) {
                            ci.setReturnValue(true);
                            ci.cancel();
                        }
                    }
                }
            }
        }
    }
}