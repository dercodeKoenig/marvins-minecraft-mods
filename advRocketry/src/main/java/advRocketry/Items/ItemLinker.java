package advRocketry.Items;

import ARLib.utils.DimensionUtils;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

import static advRocketry.Utils.ItemUtils.getStacktagOrEmpty;
import static advRocketry.Utils.ItemUtils.setTag;

public class ItemLinker extends Item {
    public ItemLinker() {
        super(new Properties().stacksTo(1));
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = getStacktagOrEmpty(stack);
        boolean hasSelected = false;
        if(tag.contains("uuid")){
            hasSelected = true;
            tooltipComponents.add(
                    Component.literal(
                            "Selected Entity: "+tag.getUUID("uuid")
                    )
            );
        }
        if(tag.contains("p")){
            hasSelected = true;
            tooltipComponents.add(
                    Component.literal(
                            "Selected Position: "+NbtUtils.readBlockPos(tag, "p").get()
                    )
            );
        }
        if(tag.contains("l")){
            hasSelected = true;
            String levelString = tag.getString("l");
            Dimension selectedDimension = DimensionManager.INSTANCE_CLIENT.get(ResourceLocation.parse(levelString));
            if(selectedDimension != null) {
                tooltipComponents.add(
                        Component.literal(
                                "selected level: " + selectedDimension.getName()
                        )
                );
            }
            tooltipComponents.add(
                    Component.literal(
                            "level id: " +levelString
                    )
            );
        }

        if(hasSelected)
            tooltipComponents.add(
                Component.literal(
                        "shift click to clear selection"
                )
            );
    }

    public static void selectBlockPos(ItemStack stack, String levelId, BlockPos pos){
        CompoundTag tag = new CompoundTag();
        tag.put("p", NbtUtils.writeBlockPos(pos));
        tag.putString("l", levelId);
        setTag(stack, tag);
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
                if(!tag.isEmpty())
                    return InteractionResult.FAIL; // only allow on empty tag to avoid replacing entry by accident

                selectBlockPos(context.getItemInHand(),levelId,p);
                context.getPlayer().sendSystemMessage(Component.literal("set position to " + levelId + ":" + p));
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean useOnEntity(Player p,ItemStack stack, Entity e){
        // select entity
        CompoundTag tag = getStacktagOrEmpty(stack);
        if(!tag.isEmpty())
            return false; // only allow on empty tag to avoid replacing entry by accident
        if(e.level().isClientSide)
            return true;
        tag.putUUID("uuid", e.getUUID());
        setTag(stack, tag);
        p.sendSystemMessage(Component.literal("selected Entity " + e));
        return true;
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
