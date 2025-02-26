package NPCs.Npc.programs;

import NPCs.Npc.NPCBase;

import NPCs.Utils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

import static NPCs.Utils.*;
import static NPCs.Utils.SUCCESS_STILL_RUNNING;

public class UnloadInventoryProgram {

    int requiredDistance = 2;

    NPCBase npc;
    int workDelay = 0;

    public UnloadInventoryProgram(NPCBase npc) {
        this.npc = npc;
    }

    public static ItemStack getAnyItemToUnload(IItemHandler itemHandlerTarget, NPCBase npc) {
        for (int j = 0; j < npc.combinedInventory.getSlots(); j++) {
            ItemStack canExtract = npc.combinedInventory.extractItem(j, 1, true);
            ItemStack stackCopyToReturn = npc.combinedInventory.getStackInSlot(j).copy();
            if (!canExtract.isEmpty()) {
                // try to insert in inventory
                for (int i = 0; i < itemHandlerTarget.getSlots(); i++) {
                    ItemStack notInserted = itemHandlerTarget.insertItem(i, canExtract, true);
                    if (notInserted.isEmpty()) {
                        return stackCopyToReturn;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack unloadOneItem(IItemHandler itemHandlerTarget, ItemStack stackToUnload) {
        for (int j = 0; j < npc.combinedInventory.getSlots(); j++) {
            ItemStack canExtract = npc.combinedInventory.extractItem(j, 1, true);
            ItemStack stackCopyToReturn = npc.combinedInventory.getStackInSlot(j).copy();
            if (!canExtract.isEmpty() && ItemStack.isSameItemSameComponents(stackToUnload, canExtract)) {
                // try to insert in inventory
                for (int i = 0; i < itemHandlerTarget.getSlots(); i++) {
                    ItemStack notInserted = itemHandlerTarget.insertItem(i, canExtract, true);
                    if (notInserted.isEmpty()) {
                        ItemStack extracted = npc.combinedInventory.extractItem(j, 1, false);
                        itemHandlerTarget.insertItem(i, extracted, false);
                        return stackCopyToReturn;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public int run(IItemHandler targetInventory, BlockPos targetPos, ItemStack nextStackToUnload) {
//System.out.println(nextStackToUnload);
        if (!ItemStack.isSameItemSameComponents(npc.getMainHandItem(), nextStackToUnload) &&
                !ItemStack.isSameItemSameComponents(npc.getOffhandItem(), nextStackToUnload)) {
            Utils.moveItemStackToAnyHand(nextStackToUnload, npc);
        }

        int pathFindExit = npc.slowMobNavigation.moveToPosition(
                targetPos,
                requiredDistance,
                npc.slowNavigationMaxDistance,
                npc.slowNavigationMaxNodes,
                npc.slowNavigationStepPerTick
        );
        if (pathFindExit == EXIT_FAIL) {
            return EXIT_FAIL;
        }
        if (pathFindExit == SUCCESS_STILL_RUNNING) {
            workDelay = 0;
            return SUCCESS_STILL_RUNNING;
        }
        npc.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos.getCenter());
        npc.lookAt(EntityAnchorArgument.Anchor.FEET, targetPos.getCenter());

        if (workDelay > 5) {
            workDelay = 0;
            if (ItemStack.isSameItemSameComponents(npc.getMainHandItem(), nextStackToUnload)) {
                npc.swing(InteractionHand.MAIN_HAND);
            } else if (ItemStack.isSameItemSameComponents(npc.getOffhandItem(), nextStackToUnload)) {
                npc.swing(InteractionHand.OFF_HAND);
            }
            if (unloadOneItem(targetInventory, nextStackToUnload).isEmpty()) {
                return EXIT_FAIL; // unable to unload this item
            } else {
                return EXIT_SUCCESS; // unloaded
            }
        }
        workDelay++;

        return SUCCESS_STILL_RUNNING;
    }
}
