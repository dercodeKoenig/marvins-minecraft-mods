package advRocketry.Items;

import advRocketry.Satellites.SatelliteEquipment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemRadiationShield extends Item implements SatelliteEquipment {
    public ItemRadiationShield() {
        super(new Properties());
    }
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.literal("fully protects satellites against radiation damage").withStyle(ChatFormatting.GRAY)
        );
    }
}
