package advRocketry.SpaceSuit;

import advRocketry.Items.ItemPortablePressureTank;
import advRocketry.Utils.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ChestPlate extends SpaceSuit {
    public ChestPlate() {
        super(Type.CHESTPLATE, new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = SpaceSuit.getCachedData(stack, context.registries());
        if (tag.contains("pressureTanks")) {
            int pressureTanks = tag.getInt("pressureTanks");
            tooltipComponents.add(
                    Component.literal("Pressure Tanks: " + pressureTanks).withStyle(ChatFormatting.GRAY)
            );
        }
        if (tag.contains("oxygen")) {
            int oxygen = tag.getInt("oxygen");
            tooltipComponents.add(
                    Component.literal("Oxygen: " + oxygen).withStyle(ChatFormatting.GRAY)
            );
        }
    }

    @Override
    public int getInventorySlots() {
        return 2;
    }

    @Override
    public boolean isItemValid(ItemStack stack, int slot) {
        if (stack.getItem() instanceof ItemPortablePressureTank)
            return true;
        return false;
    }
}
