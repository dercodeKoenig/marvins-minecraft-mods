package ARLib.utils;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

import static ARLib.utils.ItemUtils.*;

public class InventoryUtils {
    public static <I extends IItemHandler, F extends IFluidHandler> boolean canFitElements(List<I> itemInTiles, List<F> fluidInTiles, List<RecipePart> elements, RegistryAccess registry) {
        List<ItemStack> itemStacks = new ArrayList<>();
        List<FluidStack> fluidStacks = new ArrayList<>();

        for (RecipePart part : elements) {
            int num = part.amount;
            String id = part.id;
            ItemStack istack = getItemStackFromIdOrTag(id, num, registry);
            if (istack != null) {
                itemStacks.add(istack);
            }
            FluidStack fstack = getFluidStackFromId(id, num);
            if (fstack != null) {
                fluidStacks.add(fstack);
            }
        }

        return (canInsertAllItems(itemInTiles, itemStacks) && canInsertAllFluids(fluidInTiles, fluidStacks));
    }

    public static <I extends IItemHandler> boolean canInsertAllItems(List<I> itemHandlers, List<ItemStack> itemsToInsert) {
        // List to simulate all slots across handlers with item stacks
        List<ItemStack> simulatedSlots = new ArrayList<>();
        // List to track the maximum slot size for each slot in simulatedSlots
        List<Integer> maxSlotSizes = new ArrayList<>();

        // Initialize the simulated slots and max slot sizes from the actual handlers
        for (IItemHandler handler : itemHandlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack slotCopy = handler.getStackInSlot(slot).copy();  // Copy to avoid modifying real slots
                simulatedSlots.add(slotCopy);
                maxSlotSizes.add(handler.getSlotLimit(slot));  // Track the max stack size of this slot
            }
        }

        // Try to insert each item into the simulated slots
        for (ItemStack stackToInsert : itemsToInsert) {
            if (stackToInsert.isEmpty()) continue;  // Skip empty items

            int remainingCount = stackToInsert.getCount();  // Track remaining count to insert

            // Attempt to insert into each simulated slot
            for (int i = 0; i < simulatedSlots.size(); i++) {
                if (remainingCount <= 0) break;  // Move to the next item if insertion is complete

                ItemStack simulatedSlot = simulatedSlots.get(i);
                int maxSlotSize = maxSlotSizes.get(i);

                // Check if the current slot can accept this item
                if (simulatedSlot.isEmpty() || ItemStack.isSameItemSameComponents(simulatedSlot, stackToInsert)) {

                    int spaceAvailable = maxSlotSize - simulatedSlot.getCount();
                    if (!simulatedSlot.isEmpty())
                        spaceAvailable = Math.min(spaceAvailable, stackToInsert.getMaxStackSize() - simulatedSlot.getCount());

                    int toInsert = Math.min(spaceAvailable, remainingCount);

                    // Insert items into the simulated slot
                    if (simulatedSlot.isEmpty()) {
                        // If slot is empty, create a new stack with the insert amount
                        ItemStack newStack = stackToInsert.copyWithCount(toInsert);
                        simulatedSlots.set(i, newStack);
                    } else {
                        // Add to the existing stack count in the slot
                        simulatedSlot.grow(toInsert);
                    }

                    // Update remaining count after insertion
                    remainingCount -= toInsert;
                }
            }

            // If we couldn't fully insert the item stack, return false
            if (remainingCount > 0) {
                return false;
            }
        }

        // All items were fully inserted in simulation, return true
        return true;
    }


    public static <F extends IFluidHandler> boolean canInsertAllFluids(List<F> fluidHandlers, List<FluidStack> fluidsToInsert) {
        // Create a list of simulated tanks representing all tanks in all handlers
        List<FluidStack> simulatedTanks = new ArrayList<>();
        // Create a list of corresponding capacities for each simulated tank
        List<Integer> capacities = new ArrayList<>();

        // Initialize the simulated tanks and store their corresponding maximum capacities
        for (IFluidHandler handler : fluidHandlers) {
            for (int tankIndex = 0; tankIndex < handler.getTanks(); tankIndex++) {
                // Copy the current fluid stack in the tank to prevent modification of the real tank
                FluidStack tankCopy = handler.getFluidInTank(tankIndex).copy();
                simulatedTanks.add(tankCopy);

                // Store the maximum capacity of the current tank
                int maxCapacity = handler.getTankCapacity(tankIndex);
                capacities.add(maxCapacity);
            }
        }

        // For each fluid that we want to insert
        for (FluidStack fluidToInsert : fluidsToInsert) {
            if (fluidToInsert.isEmpty()) continue;  // Skip empty fluids

            // Track the remaining amount of the fluid that still needs to be inserted
            int remainingAmount = fluidToInsert.getAmount();

            // Try to insert into the simulated tanks using a single index for both the tanks and their capacities
            for (int i = 0; i < simulatedTanks.size(); i++) {
                if (remainingAmount <= 0) break;  // If the fluid is fully inserted, move to the next fluid
                int maxCapacity = capacities.get(i);  // Get the corresponding max capacity for this simulated tank

                // Check if the current simulated tank can accept this fluid
                if (simulatedTanks.get(i).isEmpty() || FluidStack.isSameFluidSameComponents(simulatedTanks.get(i), fluidToInsert)) {
                    int spaceAvailable = maxCapacity - simulatedTanks.get(i).getAmount(); // Calculate available space in the tank
                    int toInsert = Math.min(spaceAvailable, remainingAmount);

                    // Simulate insertion by "adding" fluid to the simulated tank
                    if (simulatedTanks.get(i).isEmpty()) {
                        // If the tank is empty, simulate filling it with this fluid
                        simulatedTanks.set(i, fluidToInsert.copy());
                    } else {
                        // If the tank already contains compatible fluids, add to the existing amount
                        simulatedTanks.get(i).grow(toInsert);
                    }

                    // Reduce the remaining amount by the amount that was able to be inserted
                    remainingAmount -= toInsert;
                }
            }

            // If there's remaining fluid after attempting to insert into all tanks, return false
            if (remainingAmount > 0) {
                return false;
            }
        }

        // If all fluids were fully inserted in the simulation, return true
        return true;
    }


    public static <F extends IFluidHandler, I extends IItemHandler> void createElements(List<F> fluidHandlers, List<I> itemHandlers, String id_or_tag_to_produce, int num, RegistryAccess registry) {
        ItemStack istack = getItemStackFromIdOrTag(id_or_tag_to_produce, num, registry);
        if (istack != null) {
            for (int i = 0; i < itemHandlers.size(); i++) {
                for (int o = 0; o < itemHandlers.get(i).getSlots(); o++) {
                    istack = itemHandlers.get(i).insertItem(o, istack, false);
                    if (istack.isEmpty()) return;
                }
            }
        }
        FluidStack fstack = getFluidStackFromId(id_or_tag_to_produce, num);
        if (fstack != null) {
            for (int i = 0; i < fluidHandlers.size(); i++) {
                int filled = fluidHandlers.get(i).fill(fstack, IFluidHandler.FluidAction.EXECUTE);
                fstack.shrink(filled);
                if (fstack.isEmpty()) return;
            }
        }
    }


    public static <F extends IFluidHandler, I extends IItemHandler> ItemFluidStacks consumeElements(List<F> fluidHandlers, List<I> itemHandlers, String id_or_tag_to_consume, int num, boolean simulate) {
        ItemFluidStacks consumedStacks = new ItemFluidStacks();
        for (int i = 0; i < itemHandlers.size(); i++) {
            for (int o = 0; o < itemHandlers.get(i).getSlots(); o++) {
                if (!itemHandlers.get(i).getStackInSlot(o).isEmpty()) {
                    if (matches(id_or_tag_to_consume, itemHandlers.get(i).getStackInSlot(o))) {
                        ItemStack extracted = itemHandlers.get(i).extractItem(o, num, simulate);
                        consumedStacks.itemStacks.add(extracted);
                        num -= extracted.getCount();
                        if (num == 0)
                            return consumedStacks;
                    }
                }
            }
        }

        for (int i = 0; i < fluidHandlers.size(); i++) {
            for (int o = 0; o < fluidHandlers.get(i).getTanks(); o++) {
                if (!fluidHandlers.get(i).getFluidInTank(o).isEmpty()) {
                    if (matches(id_or_tag_to_consume, fluidHandlers.get(i).getFluidInTank(o))) {
                        FluidStack drained = fluidHandlers.get(i).drain(fluidHandlers.get(i).getFluidInTank(o).copyWithAmount(num), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
                        consumedStacks.fluidStacks.add(drained);
                        num -= drained.getAmount();
                        if (num == 0)
                            return consumedStacks;
                    }
                }
            }
        }
        return consumedStacks;
    }


    public static <F extends IFluidHandler, I extends IItemHandler> boolean hasInputs(List<I> itemInTiles, List<F> fluidInTiles, List<RecipePart> inputs) {
        // Collect all non-empty item stacks from the item handlers
        List<ItemStack> myInputItems = new ArrayList<>();
        for (IItemHandler handler : itemInTiles) {
            for (int o = 0; o < handler.getSlots(); o++) {
                ItemStack s = handler.getStackInSlot(o);
                if (!s.isEmpty()) {
                    myInputItems.add(s.copy());
                }
            }
        }

        // Collect all non-empty fluid stacks from the fluid handlers
        List<FluidStack> myInputFluids = new ArrayList<>();
        for (IFluidHandler handler : fluidInTiles) {
            for (int o = 0; o < handler.getTanks(); o++) {
                FluidStack s = handler.getFluidInTank(o);
                if (!s.isEmpty()) {
                    myInputFluids.add(s.copy());
                }
            }
        }

        // Iterate over each required input
        for (RecipePart part : inputs) {
            int required = part.amount;
            String id = part.id;

            // Try to satisfy the fluid requirement first
            for (int i = 0; i < myInputFluids.size(); i++) {
                FluidStack s = myInputFluids.get(i);
                if (matches(id, s)) {
                    int count = s.getAmount();
                    int toFill = Math.min(required, count);
                    required -= toFill;
                    s.shrink(toFill);
                    if (required == 0) break;
                }
            }

            // If fluids are not enough, try to satisfy the item requirement
            if (required > 0) {
                for (int i = 0; i < myInputItems.size(); i++) {
                    ItemStack s = myInputItems.get(i);
                    if (matches(id, s)) {
                        int count = s.getCount();
                        int toFill = Math.min(required, count);
                        required -= toFill;
                        s.shrink(toFill);
                        if (required == 0) break;
                    }
                }
            }

            // If the required amount is still not satisfied, return false
            if (required > 0) {
                return false;
            }
        }

        // All required inputs were satisfied
        return true;
    }

}
