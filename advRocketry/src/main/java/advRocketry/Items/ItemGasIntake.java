package advRocketry.Items;

import advRocketry.Registry.Blocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemGasIntake extends BlockItem {

    public ItemGasIntake() {
        super(Blocks.GAS_INTAKE.get(), new Properties());
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.literal(
                        "Required for gas mining"
                ).withStyle(ChatFormatting.GRAY)
        );
    }
}
