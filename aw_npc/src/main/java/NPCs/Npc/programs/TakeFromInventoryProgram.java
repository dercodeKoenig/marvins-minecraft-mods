package NPCs.Npc.programs;

import NPCs.Npc.NPCBase;
import NPCs.Utils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import static NPCs.Utils.*;

public class TakeFromInventoryProgram {

    int requiredDistance = 2;

    NPCBase npc;
    int workDelay = 0;

    public TakeFromInventoryProgram(NPCBase npc) {
        this.npc = npc;
    }

    public ItemStack loadOneItem(IItemHandler itemHandlerTarget, ItemStack stackToload) {
        for (int j = 0; j < itemHandlerTarget.getSlots(); j++) {
            ItemStack canExtract = itemHandlerTarget.extractItem(j, 1, true);
            ItemStack stackCopyToReturn = itemHandlerTarget.getStackInSlot(j).copy();
            if (!canExtract.isEmpty() && ItemStack.isSameItemSameComponents(stackToload, canExtract)) {
                // try to insert in inventory
                for (int i = 0; i < npc.combinedInventory.getSlots(); i++) {
                    ItemStack notInserted = npc.combinedInventory.insertItem(i, canExtract, true);
                    if (notInserted.isEmpty()) {
                        ItemStack extracted = itemHandlerTarget.extractItem(j, 1, false);
                        npc.combinedInventory.insertItem(i, extracted, false);
                        return stackCopyToReturn;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public int run(IItemHandler targetInventory, BlockPos targetPos, ItemStack nextStackToload) {

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
            if (loadOneItem(targetInventory, nextStackToload).isEmpty()) {
                return EXIT_FAIL; // unable to unload this item
            } else {
                npc.swing(moveItemStackToAnyHand(nextStackToload, npc));
                return EXIT_SUCCESS; // unloaded
            }
        }
        workDelay++;

        return SUCCESS_STILL_RUNNING;
    }
}
