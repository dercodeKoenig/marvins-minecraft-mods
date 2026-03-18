package advRocketry.mixins;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin {

    @Shadow
    protected abstract void melt(BlockState state, Level level, BlockPos pos);

    @Inject(method = "randomTick",
            at = @At("HEAD"),
            cancellable = true)
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {
            if (!level.getBiome(pos).value().shouldFreeze(level, pos)) {
                if (pos.getY() > planet.getGasProperty(GasRegistry.water).worldGenSeaLevel)
                    // melt into air because it is above sea level
                    // if it would melt into water, the position was probably already worked by the sea level adjustment
                    // and it would not remove the water, so directly melt into air
                    // this is most important for if there is an ice ocean and temperature increases while sea level drops
                    // composition tracker ignores setblock composition change when called from ice block (this)
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                else
                    // normal melt into water
                    // freeze and melt go from ice->water or water->ice so the composition tracker ignores it
                    this.melt(state, level, pos);
                ci.cancel();
            }
        }
    }
}