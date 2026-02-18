package AgeOfSteam.Items.Hammer;

import AgeOfSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;

public class ItemHammer extends Item {
    public ItemHammer() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
        if(blockState.getBlock() instanceof HammerInteractionBlock hammerInteractionBlock){
            return hammerInteractionBlock.onHammer(context.getItemInHand(), context.getLevel(), context.getClickedPos(), blockState, context.getPlayer(), context.getHand());
        }

        return InteractionResult.PASS;
    }

    public interface HammerInteractionBlock {
        InteractionResult onHammer(ItemStack hammer, Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand);
    }

}
