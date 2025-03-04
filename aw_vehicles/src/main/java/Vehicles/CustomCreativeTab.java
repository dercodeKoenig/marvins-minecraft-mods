package Vehicles;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CustomCreativeTab extends CreativeModeTab {

    public CustomCreativeTab() {
        super(CreativeModeTab.builder()
                .title(Component.literal("Vehicles"))
                .icon(()->new ItemStack(Registry.ITEM_BALLISTA_SPAWN.get()))
        );
    }
}

