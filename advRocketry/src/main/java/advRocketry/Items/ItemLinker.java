package advRocketry.Items;

import ARLib.utils.DimensionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ItemLinker extends Item {
    public ItemLinker() {
        super(new Properties().stacksTo(1));
    }

    public CompoundTag getStacktagOrEmpty(ItemStack stack) {
        try {
            return ((CustomData) stack.get(DataComponents.CUSTOM_DATA)).copyTag();
        } catch (Exception var3) {
            return new CompoundTag();
        }
    }

    public void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            BlockPos p = context.getClickedPos();
            String levelId = ARLib.utils.DimensionUtils.getLevelId(context.getLevel());
            CompoundTag tag = getStacktagOrEmpty(context.getItemInHand());
            BlockEntity be = context.getLevel().getBlockEntity(p);
            if (tag.contains("p") && tag.contains("l") && be instanceof linkable l) {
                boolean result = l.link(NbtUtils.readBlockPos(tag, "p").get(), DimensionUtils.getDimensionLevelServer(tag.getString("l")));
                if(result)
                    context.getPlayer().sendSystemMessage(Component.literal("link executed"));
                else
                    context.getPlayer().sendSystemMessage(Component.literal("link failed"));
            } else {
                tag.put("p", NbtUtils.writeBlockPos(p));
                tag.putString("l", levelId);
                setTag(context.getItemInHand(), tag);
                context.getPlayer().sendSystemMessage(Component.literal("set position to " + levelId + ":" + p));
            }
            System.out.println(getStacktagOrEmpty(context.getItemInHand()));
        }
        return InteractionResult.SUCCESS;
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(!level.isClientSide) {
            setTag(player.getItemInHand(usedHand), new CompoundTag());
            player.sendSystemMessage(Component.literal("clear position"));
        }
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    public interface linkable {
        boolean link(BlockPos otherpos, Level otherLevel);
    }

}
