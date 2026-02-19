package ARLib.multiblockCore;

import ARLib.utils.BlockIdentifier;
import ARLib.utils.DimensionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;

import static ARLib.multiblockCore.BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED;

public class BlockMultiblockPart extends Block {

    static final Map<BlockIdentifier, BlockPos> multiblockMasterPositions = new HashMap<>();

    public boolean isSpecialBlock = false; // special blocks have their own flag of forward interaction to master

    public BlockMultiblockPart(Properties properties) {
        super(properties.noOcclusion().pushReaction(PushReaction.DESTROY));
        this.registerDefaultState(this.stateDefinition.any().setValue(STATE_MULTIBLOCK_FORMED, false));

    }

    public void setMaster(BlockIdentifier mypos, BlockPos masterpos) {
        if (masterpos == null && multiblockMasterPositions.containsKey(mypos))
            multiblockMasterPositions.remove(mypos);
        else
            multiblockMasterPositions.put(mypos, masterpos);
    }

    public BlockPos getMaster(BlockIdentifier mypos) {
        return multiblockMasterPositions.get(mypos);
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATE_MULTIBLOCK_FORMED); // Define the state property
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide) {
            if (state.getBlock() instanceof BlockMultiblockPart t) {
                BlockPos master = t.getMaster(new BlockIdentifier(level, pos));
                if (master != null && level.getBlockEntity(master) instanceof EntityMultiblockMaster masterTile) {
                    masterTile.scanStructure(); // returns on clientside by itself
                }
                multiblockMasterPositions.remove(new BlockIdentifier(level, pos));
            }
        }
    }


    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // because client does not have the map for master blocks I just return OK
        if (level.isClientSide) return InteractionResult.SUCCESS_NO_ITEM_USED;

        BlockPos master = getMaster(new BlockIdentifier(level, pos));
        if (master != null && level.getBlockEntity(master) instanceof EntityMultiblockMaster masterTile) {
            // if this is not a special block, use the normal flag
            if (masterTile.forwardInteractionToMaster && !isSpecialBlock)
                return masterTile.useWithoutItem(state, level, pos, player, hitResult);
            // if this IS a special block, use the special block flag
            if (masterTile.forwardSpecialBlockInteractionToMaster && isSpecialBlock)
                return masterTile.useWithoutItem(state, level, pos, player, hitResult);
        }

        return InteractionResult.PASS;
    }
}
