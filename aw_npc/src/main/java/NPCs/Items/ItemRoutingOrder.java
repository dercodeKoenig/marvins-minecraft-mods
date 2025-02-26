package NPCs.Items;

import NPCs.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.ArrayList;
import java.util.List;

public class ItemRoutingOrder extends Item {
    public ItemRoutingOrder() {
        super(new Properties());
    }

    public static List<RoutingEntry> getRoutingEntries(ItemStack stack, RegistryAccess registry) {
        List<RoutingEntry> entries = new ArrayList<>();
        CompoundTag stackTag = Utils.getStackTagOrEmpty(stack);
        if (stackTag.contains("routingEntries")) {
            ListTag entriesTag = stackTag.getList("routingEntries", Tag.TAG_COMPOUND);
            for (int i = 0; i < entriesTag.size(); i++) {
                CompoundTag entryTag = entriesTag.getCompound(i);
                RoutingEntry e = new RoutingEntry();
                e.posX = entryTag.getInt("posX");
                e.posY = entryTag.getInt("posY");
                e.posZ = entryTag.getInt("posZ");
                e.facingOrdinal = entryTag.getInt("facingOrdinal");
                e.mode = entryTag.getInt("mode");
                e.durabilityPercentFilter = entryTag.getInt("durabilityPercentFilter");
                e.durability_needsToBeAboveFilter = entryTag.getBoolean("durability_needsToBeAboveFilter");
                e.filterInventory.deserializeNBT(registry, entryTag.getCompound("filterInv"));
                entries.add(e);
            }
        }
        return entries;
    }

    public static void setRoutingEntries(ItemStack stack, List<RoutingEntry> entries, RegistryAccess registry) {
        CompoundTag stackTag = Utils.getStackTagOrEmpty(stack);
        ListTag entriesTag = new ListTag();
        for (RoutingEntry e : entries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("posX", e.posX);
            entryTag.putInt("posY", e.posY);
            entryTag.putInt("posZ", e.posZ);
            entryTag.putInt("facingOrdinal", e.facingOrdinal);
            entryTag.putInt("mode", e.mode);
            entryTag.putInt("durabilityPercentFilter", e.durabilityPercentFilter);
            entryTag.putBoolean("durability_needsToBeAboveFilter", e.durability_needsToBeAboveFilter);
            entryTag.put("filterInv", e.filterInventory.serializeNBT(registry));
            entriesTag.add(entryTag);
        }
        stackTag.put("routingEntries", entriesTag);
        Utils.setStackTag(stack, stackTag);
    }

    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            List<RoutingEntry> entries = getRoutingEntries(context.getItemInHand(), context.getLevel().registryAccess());
            RoutingEntry e = new RoutingEntry();
            e.facingOrdinal = context.getClickedFace().ordinal();
            e.posX = context.getClickedPos().getX();
            e.posY = context.getClickedPos().getY();
            e.posZ = context.getClickedPos().getZ();
            entries.add(e);
            setRoutingEntries(context.getItemInHand(), entries, context.getLevel().registryAccess());
            if (context.getPlayer() != null)
                context.getPlayer().sendSystemMessage(Component.literal("position set to " + context.getClickedPos() + ":" + context.getClickedFace()));
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }
}
