package advRocketry.Registry;


import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CustomCreativeTab extends CreativeModeTab {

    public CustomCreativeTab() {
        super(CreativeModeTab.builder()
                .title(Component.literal("Advanced Rocketry"))
                        .icon(()->new ItemStack(Items.ITEM_LAUNCHPAD.get()))
                );
    }
}
