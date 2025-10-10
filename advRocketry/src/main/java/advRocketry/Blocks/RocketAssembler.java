package advRocketry.Blocks;

import advRocketry.BlockEntities.EntityRocketAssembler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static advRocketry.Registry.ENTITY_ROCKET_ASSEMBLER;

// Launchpad area can have 2 modes: normal pad and station docking area. it should scan for the alternative pad in space
// a rocket can be instructed to go to a specific location. if the block at this location is a rocket assembler, it should go to the launchpad / docking area
// a rocket should enter space from where the planet is in universe space. this has to be translated in world coordinates. the rocket should navigate to below the docket area and then start the docking
// the station docking area can not be constructed in a non-space dimension
// if the assembler has no area, the rocket should enter on a point x blocks in front of the assembler


public class RocketAssembler extends Block implements EntityBlock {
    public RocketAssembler() {
        super(Properties.of());
        registerDefaultState(getStateDefinition().any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @javax.annotation.Nullable LivingEntity placer, ItemStack stack) {
        state = level.getBlockState(pos);
        if (placer != null) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, placer.getDirection().getOpposite());
        }
        level.setBlock(pos, updateFromNeighbourShapes(state, level, pos), 3);
    }


    // when a launch pad structure is placed or removed, trigger a rescan of all nearby rocket assembling machines
    public static void propagateScanRequestToMaster(Set<BlockPos> completed, BlockPos current, Level level) {
        if (completed.contains(current)) return;
        if(level.isClientSide)return;
        completed.add(current);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos next = new BlockPos(current.offset(x, y, z));
                    Block nextBlock = level.getBlockState(next).getBlock();
                    if (
                            nextBlock instanceof LaunchPad ||
                                    nextBlock instanceof StructureTower) {
                        propagateScanRequestToMaster(completed, next, level);
                    }
                    BlockEntity be = level.getBlockEntity(next);
                    if (be instanceof EntityRocketAssembler rocketAssembler) {
                        rocketAssembler.scanArea();
                    }
                }
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_ROCKET_ASSEMBLER.get().create(blockPos, blockState);
    }
}
