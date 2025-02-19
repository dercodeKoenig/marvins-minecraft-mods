package NPCs.Items;

import NPCs.Npc.NPCBase;
import NPCs.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class ItemSetHomeTool extends Item {
    public ItemSetHomeTool() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Return true to make the item glow
        return true;
    }
@Override
    public InteractionResult useOn(UseOnContext context) {

    if (context.getLevel() instanceof ServerLevel l) {
        CompoundTag t = Utils.getStackTagOrEmpty(context.getItemInHand());
        if (t.contains("uuid")) {
            Entity e = l.getEntity(t.getUUID("uuid"));
            if (e instanceof NPCBase npc) {
                BlockState s = context.getLevel().getBlockState(context.getClickedPos());
                if (s.getBlock() instanceof BedBlock) {
                    if(s.getValue(BedBlock.PART) == BedPart.FOOT){
                        npc.homePosition = context.getClickedPos().relative(s.getValue(BlockStateProperties.HORIZONTAL_FACING));
                    }else{
                        npc.homePosition = context.getClickedPos();
                    }
                    context.getPlayer().sendSystemMessage(Component.literal("Home point for " + npc.getName().getString() + " set to " + context.getClickedPos()));
                } else {
                    npc.homePosition = null;
                    context.getPlayer().sendSystemMessage(Component.literal("Home point removed for " + npc.getName().getString()));
                }
            }
        }
    }
    context.getPlayer().setItemInHand(context.getHand(), ItemStack.EMPTY);
    return InteractionResult.SUCCESS;
}

}
