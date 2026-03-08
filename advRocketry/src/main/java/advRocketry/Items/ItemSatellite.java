package advRocketry.Items;

import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteRegistry;
import advRocketry.Utils.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemSatellite extends Item {
    public ItemSatellite() {
        super(new Properties().stacksTo(1));
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        Satellite sat = createFromItem(stack, context.registries());
        if(sat != null) {
            tooltipComponents.add(
                    Component.literal(
                            sat.getName()
                    ).withStyle(ChatFormatting.GRAY)
            );
            if(sat.uuid != null) {
                tooltipComponents.add(
                        Component.literal(
                                sat.uuid.toString()
                        ).withStyle(ChatFormatting.GRAY)
                );
            }
        }
    }

    public static Satellite createFromItem(ItemStack stack, HolderLookup.Provider registries) {
        if(!(stack.getItem() instanceof ItemSatellite))
            return null;
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        Satellite satellite = SatelliteRegistry.createFromNbt(tag, registries);
        if (satellite == null)
            satellite = new Satellite();
        return satellite;
    }

    public static void saveToStack(ItemStack stack, Satellite satellite, HolderLookup.Provider registries) {
        CompoundTag tag = SatelliteRegistry.saveToNbt(satellite, registries);
        ItemUtils.setTag(stack, tag);
    }
}
