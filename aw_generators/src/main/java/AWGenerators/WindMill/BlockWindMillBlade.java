package AWGenerators.WindMill;

import ARLib.utils.BlockIdentifier;
import ARLib.utils.DimensionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;


public class BlockWindMillBlade extends Block {

    static final Map<BlockIdentifier, BlockPos> multiblockMasterPositions = new HashMap<>();


    public BlockWindMillBlade() {
        super(Properties.of().noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED, false));
    }


    public static void setMaster(BlockIdentifier mypos, BlockPos masterpos) {
        if (masterpos == null)
            multiblockMasterPositions.remove(mypos);
        else
            multiblockMasterPositions.put(mypos, masterpos);
    }

    public static BlockPos getMasterPos(BlockIdentifier mypos) {
        return multiblockMasterPositions.get(mypos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED); // Define the state property
    }

    // I do not want it to scan every x ticks, it should only scan on block placement
    boolean propagatePlacementToMasterForScanUpdate(Level level, BlockPos pos, HashSet<BlockPos> workedPositions) {
        if (!workedPositions.contains(pos)) {
            workedPositions.add(pos);
            for (Direction i : Direction.values()) {
                BlockPos other = pos.relative(i);
                if (level.getBlockState(other).getBlock() instanceof BlockWindMillGenerator g) {
                    if (level.getBlockEntity(other) instanceof EntityWindMillGenerator e) {
                        e.scanStructure();
                        //return true;
                    }
                }
                if (level.getBlockState(other).getBlock() instanceof BlockWindMillBlade b) {
                    if (b.propagatePlacementToMasterForScanUpdate(level, other, workedPositions)) {
                        //return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        propagatePlacementToMasterForScanUpdate(level, pos, new HashSet<>());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        BlockPos masterPos = multiblockMasterPositions.get(new BlockIdentifier(DimensionUtils.getLevelId(level), pos));
        if (masterPos != null && level.getBlockEntity(masterPos) instanceof EntityWindMillGenerator e) {
            e.scanStructure();
        }
        multiblockMasterPositions.remove(new BlockIdentifier(DimensionUtils.getLevelId(level), pos));
    }


    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        return state.getValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_309084_, BlockGetter p_309133_, BlockPos p_309097_) {
        return true;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED)) {
            return Shapes.empty();
        } else {
            return Shapes.block();
        }
    }

    VoxelShape notFullBlock = Shapes.create(0.01,0.01,0.01,0.99,0.99,0.99);
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return notFullBlock;
    }
}
