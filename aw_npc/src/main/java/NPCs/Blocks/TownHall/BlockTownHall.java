package NPCs.Blocks.TownHall;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static NPCs.Registry.ENTITY_TOWNHALL;

public class BlockTownHall extends Block implements EntityBlock {
    public BlockTownHall() {
        super(Properties.of());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_TOWNHALL.get().create(blockPos, blockState);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity e = level.getBlockEntity(pos);
        if (e instanceof EntityTownHall t) {
            t.useWithoutItem(player);
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if(!level.isClientSide) {
            TownHallData.removeEntry(level, pos);
            EntityTownHall.updateTownhalls();
            BlockEntity e = level.getBlockEntity(pos);
            if(e instanceof EntityTownHall t){
                for (int i = 0; i < t.inventory.getSlots(); i++) {
                    Block.popResource(level,pos,t.inventory.getStackInSlot(i).copy());
                    t.inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @javax.annotation.Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            if (placer instanceof Player player) {
                TownHallData.addOwner(level, pos, player.getName().getString());
                TownHallData.setName(level, pos, "Town of " + player.getName().getString());
                EntityTownHall.updateTownhalls();
            }
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityTownHall::tick;
    }
}
