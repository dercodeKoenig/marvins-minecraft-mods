package advRocketry.Items;

import advRocketry.Missions.AsteroidManager;
import advRocketry.Utils.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ItemAsteroidIdChip extends Item {

    public ItemAsteroidIdChip() {
        super(new Properties());
    }

    public static void setDescriptionForAsteroid(ItemStack stack, String description) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        tag.putString("description", description);
        ItemUtils.setTag(stack, tag);
    }

    public static void setSelectedAsteroid(@NotNull AsteroidManager.DiscoveredAsteroid discoveredAsteroid, ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", discoveredAsteroid.key);
        ItemUtils.setTag(stack, tag);

        AsteroidManager.Asteroid asteroid = AsteroidManager.getAsteroid(discoveredAsteroid);
        setDescriptionForAsteroid(stack, asteroid.description);
    }

    public static AsteroidManager.DiscoveredAsteroid getSelectedAsteroid(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains("id"))
            return AsteroidManager.getDiscoveredAsteroid(tag.getString("id"));
        else return null;
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains("id")) {
            tooltipComponents.add(
                    Component.literal(tag.getString("id")).withStyle(ChatFormatting.GRAY)
            );
        }
        if (tag.contains("description")) {
            String description = tag.getString("description");
            for (String i : description.split("\n")) {
                tooltipComponents.add(
                        Component.literal(i).withStyle(ChatFormatting.GRAY)
                );
            }
        }
    }

}