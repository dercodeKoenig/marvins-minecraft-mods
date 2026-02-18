package ARLib.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemUtils {
    public static boolean matches(String identifier, ItemStack stack) {
        //check for tag first
        ResourceLocation tagLocation = ResourceLocation.tryParse(identifier);
        if (tagLocation != null) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLocation);
            if (stack.is(tagKey))
                return true;
        }

        // It's a direct item ID
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(identifier));
        return stack.is(item);
    }


    public static boolean matches(String identifier, FluidStack stack) {
        //check for tag first
        ResourceLocation tagLocation = ResourceLocation.tryParse(identifier);
        if (tagLocation != null) {
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagLocation);
            if (stack.is(tagKey))
                return true;
        }

        // It's a direct fluid ID
        return stack.is(BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(identifier)));
    }


    public static List<Item> getItemsFromTag(String tag, RegistryAccess registry){
        ResourceLocation Id = ResourceLocation.tryParse(tag);
        if(Id != null) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, Id);
            HolderSet.Named<Item> itemsInTag = registry.lookupOrThrow(BuiltInRegistries.ITEM.key()).get(tagKey).orElse(null);
            if(itemsInTag!=null) {
                List<Holder<Item>> itemsInTagList = itemsInTag.stream().toList();
                List<Item> res = new ArrayList<>();
                for(Holder<Item> i:itemsInTagList){
                    res.add(i.value().asItem());
                }
                return res;
            }

        }
        return null;
    }

    // id is the item ID string, e.g., "minecraft:diamond"
    public static ItemStack getItemStackFromIdOrTag(String id, int count, RegistryAccess registry) {
        List<Item> itemsInTag = getItemsFromTag(id,registry);
        if(itemsInTag!=null && !itemsInTag.isEmpty()){
            return new ItemStack(itemsInTag.getFirst(),count);
        }

        return getItemStackFromid(id, count);
    }

    // id is the item ID string, e.g., "minecraft:diamond"
    public static ItemStack getItemStackFromid(String id, int count) {
        ResourceLocation itemId = ResourceLocation.tryParse(id);
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if(item == Items.AIR)return null;
        return new ItemStack(item, count);
    }

    // id is the fluid ID string, e.g., "minecraft:water"
    public static FluidStack getFluidStackFromId(String id, int amount) {
        ResourceLocation fluidId = ResourceLocation.tryParse(id);
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
        if (fluid == Fluids.EMPTY)return null;
        return new FluidStack(fluid, amount);
    }

}
