package advRocketry.Items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;

public class ItemGalaxyStorageDisk extends Item {

    public static int UNLOCKED_POINTS = 20 * 120;

    public ItemGalaxyStorageDisk() {
        super(new Properties().stacksTo(1));
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.literal("known planets: " + getKnownDimensions(stack).size())
        );
    }

    public Set<String> getKnownDimensions(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        return tag.getAllKeys();
    }

    public static int getUnlockPoints(ItemStack stack, String dimensionId) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains(dimensionId)) {
            return tag.getInt(dimensionId);
        }
        return -1;
    }

    public static void setUnlockPoints(ItemStack stack, String dimensionId, int points) {
        if (points < 0) points = 0;
        if (points > UNLOCKED_POINTS) points = UNLOCKED_POINTS;
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        tag.putInt(dimensionId, points);
        ItemUtils.setTag(stack, tag);
    }

    public static boolean isDimensionKnown(ItemStack stack, String dimensionId) {
        return getUnlockPoints(stack, dimensionId) != -1;
    }

    public static boolean isDimensionUnlocked(ItemStack stack, String dimensionId) {
        return getUnlockPoints(stack, dimensionId) == UNLOCKED_POINTS;
    }
}
