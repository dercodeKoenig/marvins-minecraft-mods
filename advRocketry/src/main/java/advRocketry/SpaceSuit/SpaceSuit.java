package advRocketry.SpaceSuit;

import advRocketry.Main;
import advRocketry.Registry.GeneralRegistry;
import advRocketry.Utils.ItemUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.List;

public abstract class SpaceSuit extends ArmorItem {
    public static final List<ArmorMaterial.Layer> spaceSuitLayers = List.of(
            new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Main.MODID, "space_suit"))
    );
    public static final HashMap<ArmorItem.Type, Integer> protection = new HashMap<>();

    static {
        protection.put(Type.HELMET, 5);
        protection.put(Type.CHESTPLATE, 5);
        protection.put(Type.LEGGINGS, 5);
        protection.put(Type.BOOTS, 5);
    }

    public SpaceSuit(Type type, Properties properties) {
        super(GeneralRegistry.SPACE_SUIT_MATERIAL, type, properties);
    }

    public static ItemStackHandler loadInventory(ItemStack stack, HolderLookup.Provider provider) {
        if(stack.isEmpty()) return null;
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        ItemStackHandler inventory = new ItemStackHandler(((SpaceSuit)stack.getItem()).getInventorySlots());
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(provider, tag.getCompound("inventory"));
        }
        return inventory;
    }

    public static void saveInventory(ItemStackHandler inventory, ItemStack stack, HolderLookup.Provider provider) {
        if (inventory == null)
            inventory = new ItemStackHandler(((SpaceSuit) stack.getItem()).getInventorySlots());
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        tag.put("inventory", inventory.serializeNBT(provider));
        addCachedData(tag, inventory, provider);
        ItemUtils.setTag(stack, tag);
    }

    public static CompoundTag getCachedData(ItemStack stack, HolderLookup.Provider provider) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains("C"))
            return tag.getCompound("C");
        return new CompoundTag();
    }

    private static void addCachedData(CompoundTag tag, IItemHandler inventory, HolderLookup.Provider provider) {
        CompoundTag cachedData = new CompoundTag();

    }

    public abstract int getInventorySlots();

    public abstract boolean isItemValid(ItemStack stack, int slot);

}
