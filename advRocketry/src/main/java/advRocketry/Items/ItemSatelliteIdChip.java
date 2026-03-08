package advRocketry.Items;

import advRocketry.Utils.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.UUID;

public class ItemSatelliteIdChip extends Item {
    public ItemSatelliteIdChip() {
        super(new Properties());
    }

    public static void setTarget(ItemStack stack, UUID target) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", target);
        ItemUtils.setTag(stack, tag);
    }

    public static UUID getTarget(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains("uuid"))
            return tag.getUUID("uuid");
        return null;
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.literal(
                        "target: " + getTarget(stack)
                ).withStyle(ChatFormatting.GRAY)
        );
    }
}
