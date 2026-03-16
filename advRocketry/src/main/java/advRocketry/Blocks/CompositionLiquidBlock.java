package advRocketry.Blocks;

import advRocketry.Config;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.PlanetDimensionProperties;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.Objects;

public class CompositionLiquidBlock extends LiquidBlock {

    public static final BooleanProperty PREVENT_COMPOSITION_CHANGE_ON_BREAK = BooleanProperty.create("ignore_composition_change_on_break");
    public static final BooleanProperty PREVENT_COMPOSITION_CHANGE_ON_PLACE = BooleanProperty.create("ignore_composition_change_on_place");

    String gasId;

    public CompositionLiquidBlock(FlowingFluid fluid, Properties properties, String gasId) {
        super(fluid, properties);
        registerDefaultState(defaultBlockState()
                .setValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK, false)
                .setValue(PREVENT_COMPOSITION_CHANGE_ON_PLACE, false));
        this.gasId = gasId;
    }

    public static double getCompositionModifier(PlanetDimension planet) {
        return Config.INSTANCE.fluid_Contribution_To_Composition_Per_1000MB / planet.getGravitationalMultiplier();
    }

    protected boolean isRandomlyTicking(BlockState state) {
        return state.getFluidState().isSource();
    }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getFluidState().isSource()) {
            if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {
                if (planet.getCurrentTemp() > 1 + GasRegistry.gases.get(gasId).getBoilingTemp(planet.getAtmosphereDensity())) {
                    // too hot, boils away
                    level.setBlock(pos, state.setValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK, true), 3);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // Ensure LiquidBlock's LEVEL property is registered
        builder.add(PREVENT_COMPOSITION_CHANGE_ON_BREAK);
        builder.add(PREVENT_COMPOSITION_CHANGE_ON_PLACE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level.isClientSide) return;

        // Only count SOURCE blocks (LEVEL == 0 in LiquidBlock, or check fluidState.isSource())
        if (state.getFluidState().isSource()) {
            if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {
                if (!Objects.equals(oldState.getBlock(), state.getBlock()) && !state.getValue(PREVENT_COMPOSITION_CHANGE_ON_PLACE)) {
                    PlanetDimensionProperties.GasProperty gas = planet.getGasProperty(gasId);
                    gas.liquid += getCompositionModifier(planet);
                    System.out.println("added " + gasId + " - " + gas.liquid);
                    planet.setRequiresSync();
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);

        if (level.isClientSide) return;

        // Only remove composition if the block being removed was a source block
        if (state.getFluidState().isSource()) {
            if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof PlanetDimension planet) {
                if (!Objects.equals(newState.getBlock(), state.getBlock()) && !state.getValue(PREVENT_COMPOSITION_CHANGE_ON_BREAK)) {
                    PlanetDimensionProperties.GasProperty gas = planet.getGasProperty(gasId);
                    gas.liquid -= Math.min(getCompositionModifier(planet), gas.liquid);
                    System.out.println("removed " + gasId + " - " + gas.liquid);
                }
            }
        }
    }
}