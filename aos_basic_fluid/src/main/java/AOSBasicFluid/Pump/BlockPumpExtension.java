package AOSBasicFluid.Pump;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static AOSBasicFluid.Registry.PUMP_EXT;

public class BlockPumpExtension extends Block {
    public BlockPumpExtension() {
        //super(Properties.of().instabreak().noOcclusion().noLootTable().replaceable().pushReaction(PushReaction.DESTROY));
        super(Properties.of().instabreak().noOcclusion().noLootTable().pushReaction(PushReaction.DESTROY));
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
        super.createBlockStateDefinition(builder);
    }

    VoxelShape myShape = Shapes.create(0.25, 0.05, 0.25, 0.75, 0.95, 0.75);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return myShape;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if(level.getBlockState(pos.below()).getBlock().equals(PUMP_EXT.get())){
            level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}