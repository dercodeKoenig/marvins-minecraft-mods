package advRocketry.Items;

import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteRegistry;
import advRocketry.utils.ItemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemSatellite extends Item {
    public ItemSatellite() {
        super(new Properties().stacksTo(16));
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
