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

    /*
    0 -> match inventory
     */

    public int mode;
    public ItemStackHandler filterInventory = new ItemStackHandler(9);
    public int durabilityPercentFilter = 0;
    public boolean durability_needsToBeAboveFilter = true;

    public int posX, posY, posZ, facingOrdinal;

    public String getModeText(){
        if(mode == 0){
            return "match target";
        }
        if(mode == 1){
            return "take any";
        }
        return "";
    }
    public void switchMode(){
        mode++;
        if(mode > 1)
            mode = 0;
    }


    public HashMap<ComparableItemStack, Integer> getStacksToInsert(IItemHandler targetInventory, IItemHandler inventory) {
        if (mode == 0) {  // match target to filter and durability filter
            return getStacksToInsert_matchFilter(targetInventory,inventory);
        }
        return null;
    }

    public HashMap<ComparableItemStack, Integer> getStacksToExtract(IItemHandler targetInventory, IItemHandler inventory) {
        if (mode == 0) {  // match target to filter and durability filter
            return getStacksToExtract_matchFilter(targetInventory,inventory);
        }
        return null;
    }




    public HashMap<ComparableItemStack, Integer> listInventory(IItemHandler inv, boolean includeComponents, boolean applyDurabilityFilter) {
        HashMap<ComparableItemStack, Integer> total = new HashMap<>();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if(applyDurabilityFilter)
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

    public HashMap<ComparableItemStack, Integer> getStacksToInsert_matchFilter(IItemHandler targetInventory, IItemHandler inventory) {

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
                    // check if the entire stack from my inventory (including components) can be inserted into the target
                    if (insertStackIntoInventory(c.stack.copyWithCount(toInsertCount), targetInventory, true) == ItemStack.EMPTY) {
                        toInsert.putIfAbsent(c, 0);
                        toInsert.put(c, toInsert.get(c) + toInsertCount);
                    }
                }
            }
        }
        return toInsert;
    }

    public HashMap<ComparableItemStack, Integer> getStacksToExtract_matchFilter(IItemHandler targetInventory, IItemHandler inventory) {

        HashMap<ComparableItemStack, Integer> filterTotalNoComponents = listInventory(filterInventory, false, false);
        HashMap<ComparableItemStack, Integer> targetTotal = listInventory(targetInventory, true, false);

        HashMap<ComparableItemStack, Integer> toExtract = new HashMap<>();
        for (ComparableItemStack c : targetTotal.keySet()) {
            int targetCount = targetTotal.get(c);
            if (!EntityWarehouseInterface.fitsDurabilityFilter(c.stack, durability_needsToBeAboveFilter, durabilityPercentFilter)) {
                // remove a stack if it does not fit the durability filter
                if (insertStackIntoInventory(c.stack.copyWithCount(targetCount), inventory, true) == ItemStack.EMPTY) {
                    toExtract.putIfAbsent(c, 0);
                    toExtract.put(c, toExtract.get(c) + targetCount);
                }
            } else {
                // if the existing item matches the durability filter, see if it matches the filter count
                int filterCount = filterTotalNoComponents.getOrDefault(new ComparableItemStack(new ItemStack(c.stack.getItem())), 0);
                int toRemove = targetCount - filterCount;
                if (toRemove > 0) {
                    if (insertStackIntoInventory(c.stack.copyWithCount(toRemove), inventory, true) == ItemStack.EMPTY) {
                        // remove a stack if the target has more compared to what the filter says it should have
                        toExtract.putIfAbsent(c, 0);
                        toExtract.put(c, toExtract.get(c) + toRemove);
                    }
                }
            }
        }
        return toExtract;
    }
}
