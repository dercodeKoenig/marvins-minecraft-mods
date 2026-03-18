package advRocketry.Items;

import advRocketry.Registry.Blocks;
import advRocketry.Satellites.SatelliteBattery;
import advRocketry.Utils.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemFluidRelease extends BlockItem {

    public ItemFluidRelease() {
        super(Blocks.FLUID_RELEASE.get(), new Properties());
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.literal(
                        "release fluids back into the world"
                ).withStyle(ChatFormatting.GRAY)
        );
    }
}
