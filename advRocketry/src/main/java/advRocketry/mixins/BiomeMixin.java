package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.SpaceStationDimension;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
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

    @Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("HEAD"),
            cancellable = true)
    public void shouldFreeze(LevelReader level, BlockPos water, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> ci) {
        BlockState blockstate = level.getBlockState(water);
        FluidState fluidstate = level.getFluidState(water);
        if (fluidstate.getType() != Fluids.WATER) {
            ci.setReturnValue(false);
            return;
        }

        if (!(blockstate.getBlock() instanceof LiquidBlock)) {
            ci.setReturnValue(false);
            return;
        }

        if (!blockstate.getFluidState().isSource()) {
            ci.setReturnValue(false);
            return;
        }


        if (level instanceof ServerLevel serverLevel && DimensionManager.INSTANCE_SERVER.get(serverLevel.dimension().location()) instanceof Dimension dimension) {
            double temp = dimension.getCurrentTemp();
            double pressure = dimension.getAtmosphereDensity();
            if (LifeSupportSystem.isTemperatureRegulated(serverLevel, water))
                temp = 300;
            if (LifeSupportSystem.isPressurized(serverLevel, water))
                pressure = Math.max(pressure, 1);

            if (dimension instanceof SpaceStationDimension) {
                // special case space stations:
                // i want it to freeze when not pressurized no matter the temp
                // note: if temperature is regulated, the dimension events may evaporate ice into air
                //       so in this case it is a bit random if water freezes / evaporates
                if (!LifeSupportSystem.isPressurized(serverLevel, water)) {
                    ci.setReturnValue(true);
                    ci.cancel();
                    return;
                }
            }

            GasRegistry.Gas waterGas = GasRegistry.gases.get(GasRegistry.water);
            if (temp > waterGas.getBoilingTemp(pressure)) {
                // too hot for any ice, even in frozen biomes
                ci.setReturnValue(false);
                return;
            } else if (temp < waterGas.getFreezeTemp(pressure)) {
                // cold enough to force freeze
                // the default code of Biome class follows:
                if (water.getY() >= level.getMinBuildHeight() && water.getY() < level.getMaxBuildHeight()) {
                    if (!mustBeAtEdge) {
                        ci.setReturnValue(true);
                        return;
                    }

                    boolean flag = level.isWaterAt(water.west()) && level.isWaterAt(water.east()) && level.isWaterAt(water.north()) && level.isWaterAt(water.south());
                    if (!flag) {
                        ci.setReturnValue(true);
                        return;
                    }
                }
            }
            // default behaviour should take over
        }
    }
}