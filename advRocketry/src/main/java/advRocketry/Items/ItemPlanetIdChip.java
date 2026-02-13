package advRocketry.Items;

import advRocketry.Dimension.DimensionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemPlanetIdChip extends Item {
    public ItemPlanetIdChip() {
        super(new Properties().stacksTo(1));
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if(tag.contains("dimensionId")){
            tooltipComponents.add(
                    Component.literal(
                            DimensionManager.INSTANCE_CLIENT.get(ResourceLocation.parse(tag.getString("dimensionId"))).getName()
                    )
            );
        }
    }

    public static void setSelectedDimension(ResourceLocation dimensionId, ItemStack stack){
        CompoundTag tag = new CompoundTag();
        tag.putString("dimensionId", dimensionId.toString());
        System.out.println(tag);
        ItemUtils.setTag(stack, tag);
    }
}
