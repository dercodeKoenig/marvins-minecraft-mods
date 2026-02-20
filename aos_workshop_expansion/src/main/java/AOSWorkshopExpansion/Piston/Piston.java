package AOSWorkshopExpansion.Piston;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import static AOSWorkshopExpansion.Piston.PistonExtension.AXIS;
import static AOSWorkshopExpansion.Registry.ENTITY_PISTON;

public class Piston extends Block implements EntityBlock {

    public static EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static BooleanProperty STATE1 = BooleanProperty.create("state1");


    public Piston() {
        super(Properties.of().noOcclusion().dynamicShape());
        BlockState state = this.stateDefinition.any();
        state = state.setValue(FACING, Direction.NORTH).setValue(STATE1, false);
        this.registerDefaultState(state);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_PISTON.get().create(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(STATE1);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player) {
            state = state.setValue(FACING, player.getNearestViewDirection().getOpposite());
            level.setBlock(pos, state, 3);
        }
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.hasNeighborSignal(pos)) {
            Direction facing = state.getValue(FACING);
            BlockPos headPos = pos.relative(facing);
            BlockState infront = level.getBlockState(headPos);
            BlockPos behindPos = pos.relative(facing.getOpposite());
            BlockState behind = level.getBlockState(behindPos);
            if ((infront.getBlock() instanceof PistonHead && infront.getValue(FACING) == facing) ||
                    (infront.getBlock() instanceof PistonExtension && infront.getValue(AXIS) == facing.getAxis())) {
                if (behind.getBlock() instanceof PistonExtension && behind.getValue(AXIS) == facing.getAxis()) {
                    PistonStructureResolver resolver = new PistonStructureResolver(level, headPos, facing, true);
                    if (resolver.resolve()) {
                        for (BlockPos p : resolver.getToDestroy()) {
                            level.destroyBlock(p, true);
                        }
                        HashMap<BlockPos, BlockState> toMove = new HashMap<>();
                        for (BlockPos p : resolver.getToPush()) {
                            toMove.put(p.relative(facing), level.getBlockState(p));
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        }
                        for (BlockPos p : toMove.keySet()) {
                            level.setBlock(p, toMove.get(p), 3);
                        }

                        level.setBlock(headPos, behind, 3);
                        level.setBlock(headPos.relative(facing), infront, 3);
                        level.setBlock(behindPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityPiston::tick;
    }
}
