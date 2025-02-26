package NPCs.Items;

import NPCs.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ItemWorkOrder extends Item {

    public static class vec3{
        public int x,y,z;
    }

    public static List<vec3> getBlockList(ItemStack stack) {
        CompoundTag stackTag = Utils.getStackTagOrEmpty(stack);
        List<vec3> l = new ArrayList<>();
        if (stackTag.contains("data")) {
            String data = stackTag.getString("data");
            Gson gson = new Gson();
            Type listType = new TypeToken<List<vec3>>() {}.getType();
            l = gson.fromJson(data, listType);
        }
        return l;
    }
    public static void setBlockList(List<vec3> blocks,ItemStack stack){
        Gson gson = new Gson();
        String data = gson.toJson(blocks);
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("data", data);
        Utils.setStackTag(stack,stackTag);
    }

    public ItemWorkOrder() {
        super(new Properties());
    }

    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            if (context.getPlayer() != null) {
                if (!context.getPlayer().isShiftKeyDown()) {
                    List<vec3> existing = getBlockList(context.getItemInHand());

                    BlockPos target = context.getClickedPos();
                    BlockState targetState = context.getLevel().getBlockState(target);
                    if(!targetState.getCollisionShape(context.getLevel(),target).isEmpty())
                        target = target.above();

                    vec3 newPos = new vec3();
                    newPos.x = target.getX();
                    newPos.y = target.getY();
                    newPos.z = target.getZ();

                    existing.add(newPos);

                    setBlockList(existing, context.getItemInHand());

                    context.getPlayer().sendSystemMessage(Component.literal("position set to " + target));
                }else{
                    setBlockList(new ArrayList<>(), context.getItemInHand());
                    context.getPlayer().sendSystemMessage(Component.literal("positions cleared"));
                }
            }
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {

    }
}
