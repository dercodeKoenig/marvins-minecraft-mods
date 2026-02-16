package ARLib.blocks;

import ARLib.blockentities.EntityItemInputBlock;
import ARLib.multiblockCore.BlockMultiblockPart;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static ARLib.ARLibRegistry.*;


public class BlockItemInputBlock extends BlockMultiblockPart implements EntityBlock {
    public BlockItemInputBlock(Properties p_49795_) {
        super(p_49795_);
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_ITEM_INPUT_BLOCK.get().create(blockPos, blockState);
    }


    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!world.isClientSide) {
            if (super.useWithoutItem(state, world, pos, player, hitResult) == InteractionResult.PASS) {
                BlockEntity be = world.getBlockEntity(pos);
                if(be instanceof EntityItemInputBlock entityItemInputBlock){
                    entityItemInputBlock.signalOpenGui((ServerPlayer) player);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityItemInputBlock::tick;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            BlockEntity me = level.getBlockEntity(pos);
            if (me instanceof EntityItemInputBlock i && !newState.getBlock().equals(state.getBlock())) {
                i.popInventory();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(this, 1));
        return drops;
    }
}
