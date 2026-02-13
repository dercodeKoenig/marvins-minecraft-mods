package advRocketry.Items;

import ARLib.utils.DimensionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import static advRocketry.Items.ItemUtils.getStacktagOrEmpty;
import static advRocketry.Items.ItemUtils.setTag;

public class ItemLinker extends Item {
    public ItemLinker() {
        super(new Properties().stacksTo(1));
    }

    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            BlockPos p = context.getClickedPos();
            String levelId = ARLib.utils.DimensionUtils.getLevelId(context.getLevel());
            CompoundTag tag = getStacktagOrEmpty(context.getItemInHand());
            BlockEntity be = context.getLevel().getBlockEntity(p);
            if (tag.contains("p") && tag.contains("l") && be instanceof linkable l) {
                if(l.link(NbtUtils.readBlockPos(tag, "p").get(), DimensionUtils.getDimensionLevelServer(tag.getString("l"))))
                    context.getPlayer().sendSystemMessage(Component.literal("link executed"));
                else
                    context.getPlayer().sendSystemMessage(Component.literal("link failed"));
                setTag(context.getItemInHand(), new CompoundTag());
            } else if(tag.contains("uuid") && be instanceof linkableToEntity le){
                if(context.getLevel() instanceof ServerLevel sl) {
                    if (le.link(sl.getEntity(tag.getUUID("uuid"))))
                        context.getPlayer().sendSystemMessage(Component.literal("link executed"));
                    else
                        context.getPlayer().sendSystemMessage(Component.literal("link failed"));
                }
                setTag(context.getItemInHand(), new CompoundTag());
            }
            else {
                // select block position
                tag = new CompoundTag();
                tag.put("p", NbtUtils.writeBlockPos(p));
                tag.putString("l", levelId);
                setTag(context.getItemInHand(), tag);
                context.getPlayer().sendSystemMessage(Component.literal("set position to " + levelId + ":" + p));
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static void useOnEntity(Player p,ItemStack stack, Entity e){
        // select entity
        if(e.level().isClientSide)
            return;
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", e.getUUID());
        setTag(stack, tag);
        p.sendSystemMessage(Component.literal("selected Entity " + e.toString()));
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(!level.isClientSide) {
            if(player.isShiftKeyDown()) {
                setTag(player.getItemInHand(usedHand), new CompoundTag());
                player.sendSystemMessage(Component.literal("clear position"));
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    public interface linkable {
        boolean link(BlockPos otherpos, Level otherLevel);
    }
    public interface linkableToEntity {
        boolean link(Entity e);
    }

}
