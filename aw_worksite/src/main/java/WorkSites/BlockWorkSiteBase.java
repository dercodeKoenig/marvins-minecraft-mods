package WorkSites;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public abstract class BlockWorkSiteBase extends Block implements EntityBlock {

    public BlockWorkSiteBase() {
        super(Properties.of());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer != null) {
            if (placer.isShiftKeyDown())
                level.setBlock(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING, placer.getDirection()), 3);
            else
                level.setBlock(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING, placer.getDirection().getOpposite()), 3);
        }

        if (level.isClientSide) {
            BlockEntity m = level.getBlockEntity(pos);
            if (m instanceof EntityWorkSiteBase c) {
                c.renderInfoTimer = 20 * 120;
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity m = level.getBlockEntity(pos);
        if (m instanceof EntityWorkSiteBase c) {
            if (!player.isShiftKeyDown()) {
                c.openMainGui();
            } else {
                if (level.isClientSide) {
                    if(c.renderInfoTimer != 0)
                        c.renderInfoTimer = 0;
                    else
                        c.renderInfoTimer = 20 * 120;
                }

            }
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityWorkSiteBase::tick;
    }
}
