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
    private void setDefaultBlockState(Properties properties, CallbackInfo ci) {
        // Grab the current default state, change our property to false, and re-register it
        if (this.getClass().getName().equals(IceBlock.class.getName())) {
            this.registerDefaultState(this.defaultBlockState().setValue(water_frozen_by_low_planet_temp, false));
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (this.getClass().getName().equals(IceBlock.class.getName())) {
            if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {
                if(planet.shouldFreezeBlocks(GasRegistry.water, pos)){
                    // force freeze in any conditions, it is too cold
                    ci.cancel();
                    return;
                }

                // if not force freezing, let default logic run
                // melting will be handled in dimension events for longer chunk ticking range
            }
        }
    }
}