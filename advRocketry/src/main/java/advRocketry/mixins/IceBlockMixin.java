package advRocketry.mixins;

import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static advRocketry.Dimension.DimensionEvents.water_frozen_by_low_planet_temp;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin extends Block {

    public IceBlockMixin(Properties properties) {
        super(properties);
    }

    @Shadow
    protected abstract void melt(BlockState state, Level level, BlockPos pos);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        if (this.getClass().getName().equals(IceBlock.class.getName())) {
            builder.add(water_frozen_by_low_planet_temp);
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setDefaultClimateState(Properties properties, CallbackInfo ci) {
        // Grab the current default state, change our property to false, and re-register it
        if (this.getClass().getName().equals(IceBlock.class.getName())) {
            this.registerDefaultState(this.defaultBlockState().setValue(water_frozen_by_low_planet_temp, false));
        }
    }

    @Inject(method = "melt", at = @At("HEAD"), cancellable = true)
    protected void melt(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {
            int seaLevel = planet.getGasProperty(GasRegistry.water).worldGenSeaLevel;
            if (pos.getY() > seaLevel && state.getValue(water_frozen_by_low_planet_temp)) {
                // if the sea level adjusts while frozen and it starts to melt, it would create water at the
                // higher sea level but the sea level adjustment will not work this position again
                // so above sea level it should melt into air directly
                // but this is reserved for sea blocks only so if you melt ice by hand it should not evaporate
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                ci.cancel();
            }
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (this.getClass().getName().equals(IceBlock.class.getName())) {
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

                if (temp > waterGas.getFreezeTemp(pressure) && state.getValue(water_frozen_by_low_planet_temp)) {
                    // block was frozen by low temp and can now melt back into original water without
                    // messing up the original terrain or player placed ice blocks
                    this.melt(state, level, pos);
                    ci.cancel();
                    return;
                }

                // if not boiling and not freezing, let default logic run
                // composition tracker still ignores melt from ice block class
            }
        }
    }
}