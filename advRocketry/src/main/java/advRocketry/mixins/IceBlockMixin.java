package advRocketry.mixins;

import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin {

    @Shadow
    protected abstract void melt(BlockState state, Level level, BlockPos pos);

    @Inject(method = "randomTick",
            at = @At("HEAD"),
            cancellable = true)
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {

            double temp = planet.getCurrentTemp();
            double pressure = planet.getAtmosphereDensity();
            if (LifeSupportSystem.isTemperatureRegulated(level, pos))
                temp = 300;
            if (LifeSupportSystem.isPressurized(level, pos))
                pressure = Math.max(pressure, 1);


            GasRegistry.Gas waterGas = GasRegistry.gases.get(GasRegistry.water);

            if (temp < waterGas.getFreezeTemp(pressure)) {
                // force freeze in any conditions, it is too cold
                ci.cancel();
                return;
            }

            if (temp > waterGas.getBoilingTemp(pressure)) {
                // too hot for any ice
                if (pos.getY() > planet.getGasProperty(GasRegistry.water).worldGenSeaLevel)
                    // melt into air because it is above sea level
                    // if it would melt into water, the position was probably already worked by the sea level adjustment
                    // and it would not remove the water, so directly melt into air
                    // this is most important for if there is an ice ocean and temperature increases while sea level drops
                    // composition tracker ignores setblock composition change when called from ice block (this)
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                else
                    // normal melt into water
                    // freeze and melt go from ice->water or water->ice so the composition tracker ignores it,
                    // (this class is also specifically excluded from composition change)
                    this.melt(state, level, pos);

                ci.cancel();
                return;
            }

            // if not boiling and not freezing, let default logic run
            // composition tracker still ignores melt from ice block class
        }
    }
}