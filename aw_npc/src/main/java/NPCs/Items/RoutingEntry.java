package NPCs.Items;

import NPCs.Utils;
import WorkSites.Warehouse.ComparableItemStack;
import WorkSites.Warehouse.EntityWarehouse;
import WorkSites.Warehouse.WarehouseItemHandler;
import WorkSites.WarehouseInterface.EntityWarehouseInterface;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.HashMap;

import static NPCs.Utils.insertStackIntoInventory;

public class RoutingEntry {

    public int mode;
    public ItemStackHandler filterInventory = new ItemStackHandler(9);
    public int durabilityPercentFilter = 0;
    public boolean durability_needsToBeAboveFilter = true;

    public int posX, posY, posZ, facingOrdinal;

    public String getModeText() {
        if (mode == 0) {
            return "match target";
        }
        if (mode == 1) {
            return "take any";
        }
        if (mode == 2) {
            return "put any";
        }
        if (mode == 3) {
            return "take except";
        }
        if (mode == 4) {
            return "put except";
        }
        if (mode == 5) {
            return "take upto";
        }
        if (mode == 6) {
            return "put upto";
        }
        if (mode == 7) {
            return "match npc";
        }
        return "";
    }

    public void switchMode() {
        mode++;
        if (mode > 7)
            mode = 0;
    }


    public HashMap<ComparableItemStack, Integer> getStacksToInsert(IItemHandler targetInventory, IItemHandler inventory) {
        if (mode == 0) {  // match target to filter and durability filter
            return getStacksToInsert_putUpto(targetInventory, inventory);
        }
        if (mode == 7) {  // match inventory to filter and durability filter, basically inverse from above
            return getStacksToExtract_matchTarget(inventory, targetInventory);
        }
        if (mode == 1) { // take any
            return new HashMap<>(); // nothing to insert
        }
        if (mode == 2) { // put any
            return getStacksToInsert_putAny(targetInventory, inventory);
        }
        if (mode == 3) { // take except
            return new HashMap<>();
        }
        if (mode == 4) { // put except
            return getStacksToInsert_putExcept(targetInventory, inventory);
        }
        if (mode == 5) { // take upto
            return new HashMap<>();
        }
        if (mode == 6) { // put upto
            return getStacksToInsert_putUpto(targetInventory, inventory);
        }
        return new HashMap<>();
    }

    public HashMap<ComparableItemStack, Integer> getStacksToExtract(IItemHandler targetInventory, IItemHandler inventory) {
        if (mode == 0) {  // match target to filter and durability filter
            return getStacksToExtract_matchTarget(targetInventory, inventory);
        }
        if (mode == 7) {  // match inventory to filter and durability filter, basically inverse from above
            return getStacksToInsert_putUpto(inventory, targetInventory);
        }
        if (mode == 1) { // take any
            return getStacksToInsert_putAny(inventory, targetInventory);
        }
        if (mode == 2) { // put any
            return new HashMap<>();
        }
        if (mode == 3) { // take except
            return getStacksToInsert_putExcept(inventory, targetInventory);
        }
        if (mode == 4) { // put except
            return new HashMap<>();
        }
        if (mode == 5) { // take upto
            return getStacksToInsert_putUpto(inventory, targetInventory);
        }
        if (mode == 6) { // put upto
            return new HashMap<>();
        }
        return new HashMap<>();
    }


    public HashMap<ComparableItemStack, Integer> listInventory(IItemHandler inv, boolean includeComponents, boolean applyDurabilityFilter) {
        HashMap<ComparableItemStack, Integer> total = new HashMap<>();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (applyDurabilityFilter)
                if (!EntityWarehouseInterface.fitsDurabilityFilter(stack, durability_needsToBeAboveFilter, durabilityPercentFilter))
                    continue;
            ComparableItemStack c;
            if (includeComponents)
                c = new ComparableItemStack(stack);
            else
                c = new ComparableItemStack(new ItemStack(stack.getItem()));
            total.putIfAbsent(c, 0);
            total.put(c, total.get(c) + stack.getCount());
        }
        return total;
    }

    public HashMap<ComparableItemStack, Integer> getStacksToInsert_putAny(IItemHandler targetInventory, IItemHandler inventory) {
        HashMap<ComparableItemStack, Integer> toInsert = new HashMap<>();

        HashMap<ComparableItemStack, Integer> filterTotalNoComponents = listInventory(filterInventory, false, false);
        HashMap<ComparableItemStack, Integer> inventoryTotalMatchingDurability = listInventory(inventory, true, true);

        for (ComparableItemStack c : inventoryTotalMatchingDurability.keySet()) {
            if (filterTotalNoComponents.containsKey(new ComparableItemStack(new ItemStack(c.stack.getItem())))) {
                int count = inventoryTotalMatchingDurability.get(c);
                ItemStack notInserted = insertStackIntoInventory(c.stack.copyWithCount(count), targetInventory, true);
                int inserted = count - notInserted.getCount();
                if (inserted > 0) {
                    toInsert.put(c, inserted);
                }
            }
        }
        return toInsert;
    }

    public HashMap<ComparableItemStack, Integer> getStacksToInsert_putExcept(IItemHandler targetInventory, IItemHandler inventory) {
        HashMap<ComparableItemStack, Integer> toInsert = new HashMap<>();

        HashMap<ComparableItemStack, Integer> filterTotalNoComponents = listInventory(filterInventory, false, false);
        HashMap<ComparableItemStack, Integer> inventoryTotalMatchingDurability = listInventory(inventory, true, true);

        for (ComparableItemStack c : inventoryTotalMatchingDurability.keySet()) {
            if (!filterTotalNoComponents.containsKey(new ComparableItemStack(new ItemStack(c.stack.getItem())))) {
                int count = inventoryTotalMatchingDurability.get(c);
                ItemStack notInserted = insertStackIntoInventory(c.stack.copyWithCount(count), targetInventory, true);
                int inserted = count - notInserted.getCount();
                if (inserted > 0) {
                    toInsert.put(c, inserted);
                }
            }
        }
        return toInsert;
    }


    public HashMap<ComparableItemStack, Integer> getStacksToInsert_putUpto(IItemHandler targetInventory, IItemHandler inventory) {

        HashMap<ComparableItemStack, Integer> toInsert = new HashMap<>();

        HashMap<ComparableItemStack, Integer> targetTotalFittingDurabilityFilterNoComponents = listInventory(targetInventory, false, true);
        HashMap<ComparableItemStack, Integer> filterTotal = listInventory(filterInventory, false, false);
        HashMap<ComparableItemStack, Integer> inventoryTotalFittingDurabilityFilter = listInventory(inventory, true, true);

        // loop over all items in my inventory that fit the durability filter
        for (ComparableItemStack c : inventoryTotalFittingDurabilityFilter.keySet()) {
            ComparableItemStack cNoComponents = new ComparableItemStack(new ItemStack(c.stack.getItem()));
            // check if the filter contains an entry for this item
            if (filterTotal.containsKey(cNoComponents)) {
                // found an item in my inventory that could be required and fits the durability filter
                // now check if it is already in the target inventory with the required count

                // get the entry from the target inventory, ignoring components
                int targetCount = targetTotalFittingDurabilityFilterNoComponents.getOrDefault(cNoComponents, 0);
                int filterCount = filterTotal.get(cNoComponents);
                int toInsertCount = filterCount - targetCount;
                if (toInsertCount > 0) {
                    ItemStack notInserted = insertStackIntoInventory(c.stack.copyWithCount(toInsertCount), inventory, true);
                    int inserted = toInsertCount - notInserted.getCount();
                    if (inserted > 0) {
                        toInsert.put(c, inserted);

                        // remove filled count from the filter count so it does not fill the same filter multiple times for different item components
                        if(targetTotalFittingDurabilityFilterNoComponents.get(cNoComponents)!=null){
                            targetTotalFittingDurabilityFilterNoComponents.put(cNoComponents, targetTotalFittingDurabilityFilterNoComponents.get(cNoComponents)-inserted);
                        }
                    }
                }
            }
        }
        return toInsert;
    }

    public HashMap<ComparableItemStack, Integer> getStacksToExtract_matchTarget(IItemHandler targetInventory, IItemHandler inventory) {

        HashMap<ComparableItemStack, Integer> filterTotalNoComponents = listInventory(filterInventory, false, false);
        HashMap<ComparableItemStack, Integer> targetTotal = listInventory(targetInventory, true, false);

        HashMap<ComparableItemStack, Integer> toExtract = new HashMap<>();
        for (ComparableItemStack c : targetTotal.keySet()) {
            ComparableItemStack cNoComponents = new ComparableItemStack(new ItemStack(c.stack.getItem()));
            int targetCount = targetTotal.get(c);
            int toRemove = 0;
            if (!EntityWarehouseInterface.fitsDurabilityFilter(c.stack, durability_needsToBeAboveFilter, durabilityPercentFilter))
                // if it does not fit the durability filter, remove it all
                toRemove = targetCount;
            else {
                // if the existing item matches the durability filter, see if it matches the filter count
                int filterCount = filterTotalNoComponents.getOrDefault(cNoComponents, 0);
                toRemove = targetCount - filterCount;
            }
            if (toRemove > 0) {
                ItemStack notInserted = insertStackIntoInventory(c.stack.copyWithCount(toRemove), inventory, true);
                int inserted = toRemove - notInserted.getCount();
                if (inserted > 0) {
                    toExtract.put(c, inserted);

                    // remove the inserted from the filter in case there are multiple same items with different components
                    if(filterTotalNoComponents.get(cNoComponents) != null){
                        filterTotalNoComponents.put(cNoComponents, filterTotalNoComponents.get(cNoComponents)-inserted);
                    }
                }
            }

        }
        return toExtract;
    }
}
