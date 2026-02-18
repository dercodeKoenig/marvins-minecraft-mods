package AgeOfSteam.Items.Hammer;

import AgeOfSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase;
import AgeOfSteam.Blocks.Mechanics.TJunction.BlockTJunctionBase;
import AgeOfSteam.Blocks.Mechanics.TJunction.EntityTJunctionBase;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;

public class ItemHammer extends Item {
    public ItemHammer() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        BlockEntity tile =context.getLevel().getBlockEntity(context.getClickedPos());

        if(tile instanceof EntityCrankShaftBase i) {
            if(!context.getLevel().isClientSide()) {
                i.incRotationOffset();
                i.myMechanicalBlock.propagateResetRotation(0, null, new HashSet<>());
            }
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }

        return InteractionResult.PASS;
    }

}
